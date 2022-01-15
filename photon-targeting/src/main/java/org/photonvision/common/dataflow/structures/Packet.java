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

/** A packet that holds byte-packed data to be sent over NetworkTables. */
public class Packet {
    // Size of the packet.
    int size;
    // Data stored in the packet.
    byte[] packetData;
    // Read and write positions.
    int readPos, writePos;

    /**
     * Constructs an empty packet.
     *
     * @param size The size of the packet buffer.
     */
    public Packet(int size) {
        this.size = size;
        packetData = new byte[size];
    }

    /**
     * Constructs a packet with the given data.
     *
     * @param data The packet data.
     */
    public Packet(byte[] data) {
        packetData = data;
        size = packetData.length;
    }

    /** Clears the packet and resets the read and write positions. */
    public void clear() {
        packetData = new byte[size];
        readPos = 0;
        writePos = 0;
    }

    public int getSize() {
        return size;
    }

    /**
     * Returns the packet data.
     *
     * @return The packet data.
     */
    public byte[] getData() {
        return packetData;
    }

    /**
     * Sets the packet data.
     *
     * @param data The packet data.
     */
    public void setData(byte[] data) {
        packetData = data;
        size = data.length;
    }

    /**
     * Encodes the byte into the packet.
     *
     * @param src The byte to encode.
     */
    public void encode(byte src) {
        packetData[writePos++] = src;
    }

    /**
     * Encodes the integer into the packet.
     *
     * @param src The integer to encode.
     */
    public void encode(int src) {
        packetData[writePos++] = (byte) (src >>> 24);
        packetData[writePos++] = (byte) (src >>> 16);
        packetData[writePos++] = (byte) (src >>> 8);
        packetData[writePos++] = (byte) src;
    }

    /**
     * Encodes the double into the packet.
     *
     * @param src The double to encode.
     */
    public void encode(double src) {
        long data = Double.doubleToRawLongBits(src);
        packetData[writePos++] = (byte) ((data >> 56) & 0xff);
        packetData[writePos++] = (byte) ((data >> 48) & 0xff);
        packetData[writePos++] = (byte) ((data >> 40) & 0xff);
        packetData[writePos++] = (byte) ((data >> 32) & 0xff);
        packetData[writePos++] = (byte) ((data >> 24) & 0xff);
        packetData[writePos++] = (byte) ((data >> 16) & 0xff);
        packetData[writePos++] = (byte) ((data >> 8) & 0xff);
        packetData[writePos++] = (byte) (data & 0xff);
    }

    /**
     * Encodes the boolean into the packet.
     *
     * @param src The boolean to encode.
     */
    public void encode(boolean src) {
        packetData[writePos++] = src ? (byte) 1 : (byte) 0;
    }

    /**
     * Returns a decoded byte from the packet.
     *
     * @return A decoded byte from the packet.
     */
    public byte decodeByte() {
        return packetData[readPos++];
    }

    /**
     * Returns a decoded int from the packet.
     *
     * @return A decoded int from the packet.
     */
    public int decodeInt() {
        return (0xff & packetData[readPos++]) << 24
                | (0xff & packetData[readPos++]) << 16
                | (0xff & packetData[readPos++]) << 8
                | (0xff & packetData[readPos++]);
    }

    /**
     * Returns a decoded double from the packet.
     *
     * @return A decoded double from the packet.
     */
    public double decodeDouble() {
        long data =
                (long) (0xff & packetData[readPos++]) << 56
                        | (long) (0xff & packetData[readPos++]) << 48
                        | (long) (0xff & packetData[readPos++]) << 40
                        | (long) (0xff & packetData[readPos++]) << 32
                        | (long) (0xff & packetData[readPos++]) << 24
                        | (long) (0xff & packetData[readPos++]) << 16
                        | (long) (0xff & packetData[readPos++]) << 8
                        | (long) (0xff & packetData[readPos++]);
        return Double.longBitsToDouble(data);
    }

    /**
     * Returns a decoded boolean from the packet.
     *
     * @return A decoded boolean from the packet.
     */
    public boolean decodeBoolean() {
        return packetData[readPos++] == 1;
    }
}
