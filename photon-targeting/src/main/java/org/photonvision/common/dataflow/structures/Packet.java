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

package org.photonvision.common.dataflow.structures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.photonvision.targeting.serde.PhotonStructSerializable;

/** A packet that holds byte-packed data to be sent over NetworkTables. */
public class Packet {
    // Data stored in the packet.
    byte[] packetData;
    // Read and write positions.
    int readPos, writePos;

    /**
     * Constructs an empty packet. This buffer will dynamically expand if we need more data space.
     *
     * @param size The size of the packet buffer.
     */
    public Packet(int size) {
        packetData = new byte[size];
    }

    /**
     * Constructs a packet with the given data.
     *
     * @param data The packet data.
     */
    public Packet(byte[] data) {
        packetData = data;
    }

    /** Clears the packet and resets the read and write positions. */
    public void clear() {
        packetData = new byte[packetData.length];
        readPos = 0;
        writePos = 0;
    }

    public int getNumBytesWritten() {
        return writePos + 1;
    }

    public int getNumBytesRead() {
        return readPos + 1;
    }

    public int getSize() {
        return packetData.length;
    }

    /**
     * Returns a copy of only the packet data we've actually written to so far.
     *
     * @return The packet data.
     */
    public byte[] getWrittenDataCopy() {
        return Arrays.copyOfRange(packetData, 0, writePos);
    }

    /**
     * Sets the packet data.
     *
     * @param data The packet data.
     */
    public void setData(byte[] data) {
        packetData = data;
    }

    // Logic taken from ArraysSupport, licensed under GPL V2
    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    // Logic taken from ArraysSupport, licensed under GPL V2
    private static int newLength(int oldLength, int minGrowth, int prefGrowth) {
        // preconditions not checked because of inlining
        // assert oldLength >= 0
        // assert minGrowth > 0

        int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
        if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
            return prefLength;
        } else {
            // put code cold in a separate method
            return hugeLength(oldLength, minGrowth);
        }
    }

    // Logic taken from ArraysSupport, licensed under GPL V2
    private static int hugeLength(int oldLength, int minGrowth) {
        int minLength = oldLength + minGrowth;
        if (minLength < 0) { // overflow
            throw new OutOfMemoryError(
                    "Required array length " + oldLength + " + " + minGrowth + " is too large");
        } else if (minLength <= SOFT_MAX_ARRAY_LENGTH) {
            return SOFT_MAX_ARRAY_LENGTH;
        } else {
            return minLength;
        }
    }

    /**
     * Increases the capacity to ensure that it can hold at least the number of elements specified by
     * the minimum capacity argument.
     *
     * <p>This logic is copied from ArrayList, which is licensed GPL V2
     *
     * @param minCapacity the desired minimum capacity
     * @return
     */
    private void ensureCapacity(int bytesToAdd) {
        int minCapacity = writePos + bytesToAdd;
        int oldCapacity = packetData.length;
        if (minCapacity <= oldCapacity) {
            return;
        }
        if (oldCapacity > 0) {
            int newCapacity =
                    Packet.newLength(
                            oldCapacity,
                            minCapacity - oldCapacity, /* minimum growth */
                            oldCapacity >> 1 /* preferred growth */);
            packetData = Arrays.copyOf(packetData, newCapacity);
        } else {
            packetData = new byte[Math.max(256, minCapacity)];
        }
    }

    /**
     * Encodes the byte into the packet.
     *
     * @param src The byte to encode.
     */
    public void encode(byte src) {
        ensureCapacity(1);
        packetData[writePos++] = src;
    }

    /**
     * Encodes the short into the packet.
     *
     * @param src The short to encode.
     */
    public void encode(short src) {
        ensureCapacity(2);
        packetData[writePos++] = (byte) src;
        packetData[writePos++] = (byte) (src >>> 8);
    }

    /**
     * Encodes the integer into the packet.
     *
     * @param src The integer to encode.
     */
    public void encode(int src) {
        ensureCapacity(4);
        packetData[writePos++] = (byte) src;
        packetData[writePos++] = (byte) (src >>> 8);
        packetData[writePos++] = (byte) (src >>> 16);
        packetData[writePos++] = (byte) (src >>> 24);
    }

    /**
     * Encodes the float into the packet.
     *
     * @param src The float to encode.
     */
    public void encode(float src) {
        ensureCapacity(4);
        int data = Float.floatToIntBits(src);
        packetData[writePos++] = (byte) (data & 0xff);
        packetData[writePos++] = (byte) ((data >> 8) & 0xff);
        packetData[writePos++] = (byte) ((data >> 16) & 0xff);
        packetData[writePos++] = (byte) ((data >> 24) & 0xff);
    }

    /**
     * Encodes the double into the packet.
     *
     * @param data The double to encode.
     */
    public void encode(long data) {
        ensureCapacity(8);
        packetData[writePos++] = (byte) (data & 0xff);
        packetData[writePos++] = (byte) ((data >> 8) & 0xff);
        packetData[writePos++] = (byte) ((data >> 16) & 0xff);
        packetData[writePos++] = (byte) ((data >> 24) & 0xff);
        packetData[writePos++] = (byte) ((data >> 32) & 0xff);
        packetData[writePos++] = (byte) ((data >> 40) & 0xff);
        packetData[writePos++] = (byte) ((data >> 48) & 0xff);
        packetData[writePos++] = (byte) ((data >> 56) & 0xff);
    }

    /**
     * Encodes the double into the packet.
     *
     * @param src The double to encode.
     */
    public void encode(double src) {
        ensureCapacity(8);
        long data = Double.doubleToRawLongBits(src);
        packetData[writePos++] = (byte) (data & 0xff);
        packetData[writePos++] = (byte) ((data >> 8) & 0xff);
        packetData[writePos++] = (byte) ((data >> 16) & 0xff);
        packetData[writePos++] = (byte) ((data >> 24) & 0xff);
        packetData[writePos++] = (byte) ((data >> 32) & 0xff);
        packetData[writePos++] = (byte) ((data >> 40) & 0xff);
        packetData[writePos++] = (byte) ((data >> 48) & 0xff);
        packetData[writePos++] = (byte) ((data >> 56) & 0xff);
    }

    /**
     * Encodes the boolean into the packet.
     *
     * @param src The boolean to encode.
     */
    public void encode(boolean src) {
        ensureCapacity(1);
        packetData[writePos++] = src ? (byte) 1 : (byte) 0;
    }

    public void encode(List<Short> data) {
        byte size = (byte) data.size();
        if (data.size() > Byte.MAX_VALUE) {
            throw new RuntimeException("Array too long! Got " + size);
        }

        // length byte
        encode(size);

        for (var f : data) {
            encode(f);
        }
    }

    public <T extends PhotonStructSerializable<T>> void encode(T data) {
        data.getSerde().pack(this, data);
    }

    /**
     * Encode a list of serializable structs. Lists are stored as [uint8 length, [length many] data
     * structs]
     *
     * @param <T> the class this list will be packing
     * @param data
     */
    public <T extends PhotonStructSerializable<T>> void encodeList(List<T> data) {
        byte size = (byte) data.size();
        if (data.size() > Byte.MAX_VALUE) {
            throw new RuntimeException("Array too long! Got " + size);
        }

        // length byte
        encode(size);

        for (var f : data) {
            f.getSerde().pack(this, f);
        }
    }

    public <T extends PhotonStructSerializable<T>> void encodeOptional(Optional<T> data) {
        encode(data.isPresent());
        if (data.isPresent()) {
            data.get().getSerde().pack(this, data.get());
        }
    }

    /**
     * Returns a decoded byte from the packet.
     *
     * @return A decoded byte from the packet.
     */
    public byte decodeByte() {
        if (packetData.length < readPos) {
            return '\0';
        }
        return packetData[readPos++];
    }

    /**
     * Returns a decoded int from the packet.
     *
     * @return A decoded int from the packet.
     */
    public int decodeInt() {
        if (packetData.length < readPos + 3) {
            return 0;
        }
        return (0xff & packetData[readPos++])
                | (0xff & packetData[readPos++]) << 8
                | (0xff & packetData[readPos++]) << 16
                | (0xff & packetData[readPos++]) << 24;
    }

    public long decodeLong() {
        if (packetData.length < (readPos + 7)) {
            return 0;
        }
        long data =
                (long)
                        (0xff & packetData[readPos++]
                                | (long) (0xff & packetData[readPos++]) << 8
                                | (long) (0xff & packetData[readPos++]) << 16
                                | (long) (0xff & packetData[readPos++]) << 24
                                | (long) (0xff & packetData[readPos++]) << 32
                                | (long) (0xff & packetData[readPos++]) << 40
                                | (long) (0xff & packetData[readPos++]) << 48
                                | (long) (0xff & packetData[readPos++]) << 56);
        return data;
    }

    /**
     * Returns a decoded double from the packet.
     *
     * @return A decoded double from the packet.
     */
    public double decodeDouble() {
        if (packetData.length < (readPos + 7)) {
            return 0;
        }
        long data =
                (long)
                        (0xff & packetData[readPos++]
                                | (long) (0xff & packetData[readPos++]) << 8
                                | (long) (0xff & packetData[readPos++]) << 16
                                | (long) (0xff & packetData[readPos++]) << 24
                                | (long) (0xff & packetData[readPos++]) << 32
                                | (long) (0xff & packetData[readPos++]) << 40
                                | (long) (0xff & packetData[readPos++]) << 48
                                | (long) (0xff & packetData[readPos++]) << 56);
        return Double.longBitsToDouble(data);
    }

    /**
     * Returns a decoded float from the packet.
     *
     * @return A decoded float from the packet.
     */
    public float decodeFloat() {
        if (packetData.length < (readPos + 3)) {
            return 0;
        }

        int data =
                ((0xff & packetData[readPos++]
                        | (0xff & packetData[readPos++]) << 8
                        | (0xff & packetData[readPos++]) << 16
                        | (0xff & packetData[readPos++]) << 24));
        return Float.intBitsToFloat(data);
    }

    /**
     * Returns a decoded boolean from the packet.
     *
     * @return A decoded boolean from the packet.
     */
    public boolean decodeBoolean() {
        if (packetData.length < readPos) {
            return false;
        }
        return packetData[readPos++] == 1;
    }

    public void encode(double[] data) {
        for (double d : data) {
            encode(d);
        }
    }

    public double[] decodeDoubleArray(int len) {
        double[] ret = new double[len];
        for (int i = 0; i < len; i++) {
            ret[i] = decodeDouble();
        }
        return ret;
    }

    public short decodeShort() {
        if (packetData.length < readPos + 1) {
            return 0;
        }
        return (short) ((0xff & packetData[readPos++]) | (0xff & packetData[readPos++]) << 8);
    }

    /**
     * Decode a list of serializable structs. Lists are stored as [uint8 length, [length many] data
     * structs]. Because java sucks, we need to take the serde ref directly
     *
     * @param <T>
     * @param serde
     */
    public <T extends PhotonStructSerializable<T>> List<T> decodeList(PacketSerde<T> serde) {
        byte length = decodeByte();

        var ret = new ArrayList<T>();
        ret.ensureCapacity(length);

        for (int i = 0; i < length; i++) {
            ret.add(serde.unpack(this));
        }

        return ret;
    }

    public <T extends PhotonStructSerializable<T>> Optional<T> decodeOptional(PacketSerde<T> serde) {
        var present = decodeBoolean();
        if (present) {
            return Optional.of(serde.unpack(this));
        }
        return Optional.empty();
    }

    public List<Short> decodeShortList() {
        byte length = decodeByte();

        var ret = new ArrayList<Short>();
        ret.ensureCapacity(length);

        for (int i = 0; i < length; i++) {
            ret.add(decodeShort());
        }

        return ret;
    }

    public <T extends PhotonStructSerializable<T>> T decode(PhotonStructSerializable<T> t) {
        return t.getSerde().unpack(this);
    }
}
