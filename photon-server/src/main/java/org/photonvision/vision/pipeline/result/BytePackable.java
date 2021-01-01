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

package org.photonvision.vision.pipeline.result;

@SuppressWarnings("PointlessBitwiseExpression")
public abstract class BytePackable {
    public abstract byte[] toByteArray();

    public abstract void fromByteArray(byte[] src);

    protected int bufferPosition = 0;

    public int getBufferPosition() {
        return bufferPosition;
    }

    public void resetBufferPosition() {
        bufferPosition = 0;
    }

    protected void bufferData(byte[] src, byte[] dest) {
        System.arraycopy(src, 0, dest, bufferPosition, src.length);
        bufferPosition += src.length;
    }

    protected void bufferData(byte src, byte[] dest) {
        System.arraycopy(new byte[] {src}, 0, dest, bufferPosition, 1);
        bufferPosition++;
    }

    protected void bufferData(int src, byte[] dest) {
        System.arraycopy(toBytes(src), 0, dest, bufferPosition, Integer.BYTES);
        bufferPosition += Integer.BYTES;
    }

    protected void bufferData(double src, byte[] dest) {
        System.arraycopy(toBytes(src), 0, dest, bufferPosition, Double.BYTES);
        bufferPosition += Double.BYTES;
    }

    protected void bufferData(boolean src, byte[] dest) {
        System.arraycopy(toBytes(src), 0, dest, bufferPosition, 1);
        bufferPosition += 1;
    }

    protected boolean unbufferBoolean(byte[] src) {
        return toBoolean(src, bufferPosition++);
    }

    protected byte unbufferByte(byte[] src) {
        var value = src[bufferPosition];
        bufferPosition++;
        return value;
    }

    protected byte[] unbufferBytes(byte[] src, int len) {
        var bytes = new byte[len];
        System.arraycopy(src, bufferPosition, bytes, 0, len);
        return bytes;
    }

    protected int unbufferInt(byte[] src) {
        var value = toInt(src, bufferPosition);
        bufferPosition += Integer.BYTES;
        return value;
    }

    protected double unbufferDouble(byte[] src) {
        var value = toDouble(src, bufferPosition);
        bufferPosition += Double.BYTES;
        return value;
    }

    private static boolean toBoolean(byte[] src, int pos) {
        return src[pos] != 0;
    }

    private static int toInt(byte[] src, int pos) {
        return (0xff & src[pos]) << 24
                | (0xff & src[pos + 1]) << 16
                | (0xff & src[pos + 2]) << 8
                | (0xff & src[pos + 3]) << 0;
    }

    private static long toLong(byte[] src, int pos) {
        return (long) (0xff & src[pos]) << 56
                | (long) (0xff & src[pos + 1]) << 48
                | (long) (0xff & src[pos + 2]) << 40
                | (long) (0xff & src[pos + 3]) << 32
                | (long) (0xff & src[pos + 4]) << 24
                | (long) (0xff & src[pos + 5]) << 16
                | (long) (0xff & src[pos + 6]) << 8
                | (long) (0xff & src[pos + 7]) << 0;
    }

    private static double toDouble(byte[] src, int pos) {
        return Double.longBitsToDouble(toLong(src, pos));
    }

    protected byte[] toBytes(double value) {
        long data = Double.doubleToRawLongBits(value);
        return new byte[] {
            (byte) ((data >> 56) & 0xff),
            (byte) ((data >> 48) & 0xff),
            (byte) ((data >> 40) & 0xff),
            (byte) ((data >> 32) & 0xff),
            (byte) ((data >> 24) & 0xff),
            (byte) ((data >> 16) & 0xff),
            (byte) ((data >> 8) & 0xff),
            (byte) ((data >> 0) & 0xff),
        };
    }

    protected byte[] toBytes(int value) {
        return new byte[] {
            (byte) ((value >> 24) & 0xff),
            (byte) ((value >> 16) & 0xff),
            (byte) ((value >> 8) & 0xff),
            (byte) ((value >> 0) & 0xff)
        };
    }

    protected byte[] toBytes(boolean value) {
        return new byte[] {(byte) (value ? 1 : 0)};
    }
}
