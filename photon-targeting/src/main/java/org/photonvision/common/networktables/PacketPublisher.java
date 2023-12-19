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
