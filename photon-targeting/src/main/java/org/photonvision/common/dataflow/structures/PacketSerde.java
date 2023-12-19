package org.photonvision.common.dataflow.structures;

public interface PacketSerde<T> {
    public int getMaxByteSize();

    void pack(Packet packet, T value);

    T unpack(Packet packet);
}
