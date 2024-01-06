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
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.dataflow.structures.PacketSerde;

public class PacketSubscriber<T> implements AutoCloseable {
    public final RawSubscriber subscriber;
    private final PacketSerde<T> serde;
    private final T defaultValue;

    private final Packet packet = new Packet(1);

    public PacketSubscriber(RawSubscriber subscriber, PacketSerde<T> serde, T defaultValue) {
        this.subscriber = subscriber;
        this.serde = serde;
        this.defaultValue = defaultValue;
    }

    public T get() {
        packet.clear();

        packet.setData(subscriber.get(new byte[] {}));
        if (packet.getSize() < 1) return defaultValue;

        return serde.unpack(packet);
    }

    @Override
    public void close() {
        subscriber.close();
    }
}
