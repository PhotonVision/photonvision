/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
        // Get /all/ changes since last call to readQueue
        var changes = subscriber.readQueue();

        List<PacketResult<T>> ret = new ArrayList<>(changes.length);
        for (var change : changes) {
            ret.add(parse(change.value, change.timestamp));
        }

        return ret;
    }
}
