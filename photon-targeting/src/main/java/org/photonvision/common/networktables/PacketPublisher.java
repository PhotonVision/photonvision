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

import edu.wpi.first.networktables.RawPublisher;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.dataflow.structures.PacketSerde;

public class PacketPublisher<T> implements AutoCloseable {
    public final RawPublisher publisher;
    private final PacketSerde<T> serde;

    public PacketPublisher(RawPublisher publisher, PacketSerde<T> serde) {
        this.publisher = publisher;
        this.serde = serde;
    }

    public void accept(T value, int byteSize) {
        var packet = new Packet(byteSize);
        serde.pack(packet, value);
        publisher.set(packet.getData());
    }

    public void accept(T value) {
        accept(value, serde.getMaxByteSize());
    }

    @Override
    public void close() {
        publisher.close();
    }
}
