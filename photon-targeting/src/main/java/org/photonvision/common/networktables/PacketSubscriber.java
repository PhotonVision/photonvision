/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.networktables;

import edu.wpi.first.networktables.RawSubscriber;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.dataflow.structures.PacketSerde;

public class PacketSubscriber<T> implements AutoCloseable {
    public static class PacketResult<U> {
        public final U value;
        public final long timestamp;

        public PacketResult(U value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public PacketResult() {
            this(null, 0);
        }
    }

    public final RawSubscriber subscriber;
    private final PacketSerde<T> serde;

    private final Packet packet = new Packet(1);

    /**
     * Create a PacketSubscriber
     *
     * @param subscriber NT subscriber. Set pollStorage to 1 to make get() faster
     * @param serde How we convert raw to actual things
     */
    public PacketSubscriber(RawSubscriber subscriber, PacketSerde<T> serde) {
        this.subscriber = subscriber;
        this.serde = serde;
    }

    /** Parse one chunk of timestamped data into T */
    private PacketResult<T> parse(byte[] data, long timestamp) {
        packet.clear();
        packet.setData(data);
        if (packet.getSize() < 1) {
            return new PacketResult<T>();
        }

        return new PacketResult<>(serde.unpack(packet), timestamp);
    }

    /**
     * Get the latest value sent over NT. If the value has never been set, returns the provided
     * default
     */
    public PacketResult<T> get() {
        // Get /all/ changes since last call to readQueue
        var data = subscriber.getAtomic();

        // Topic has never been published to?
        if (data.timestamp == 0) {
            return new PacketResult<>();
        }

        return parse(data.value, data.timestamp);
    }

    @Override
    public void close() {
        subscriber.close();
    }

    // TODO - i can see an argument for moving this logic all here instead of keeping in photoncamera
    public String getInterfaceUUID() {
        // ntcore hands us a JSON string with leading/trailing quotes - remove those
        var uuidStr = subscriber.getTopic().getProperty("message_uuid");

        // "null" can be returned if the property does not exist. From system knowledge, uuid can never
        // be the string literal "null".
        if (uuidStr.equals("null")) {
            return "";
        }

        return uuidStr.replace("\"", "");
    }

    public List<PacketResult<T>> getAllChanges() {
        List<PacketResult<T>> ret = new ArrayList<>();

        // Get /all/ changes since last call to readQueue
        var changes = subscriber.readQueue();

        for (var change : changes) {
            ret.add(parse(change.value, change.timestamp));
        }

        return ret;
    }
}
