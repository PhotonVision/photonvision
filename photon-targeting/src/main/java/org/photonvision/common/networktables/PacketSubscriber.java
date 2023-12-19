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
