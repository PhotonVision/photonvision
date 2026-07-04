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

package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.PnpResult;
import org.photonvision.targeting.TargetCorner;
import org.wpilib.math.geometry.*;

class PacketTest {
    @Test
    public void testDecodeByteAtBoundary() {
        // Exactly enough data must decode
        var packet = new Packet(new byte[] {0x12});
        assertEquals((byte) 0x12, packet.decodeByte());

        // One byte short must return the default instead of reading out of bounds
        var shortPacket = new Packet(new byte[] {});
        assertEquals((byte) 0, shortPacket.decodeByte());
    }

    @Test
    public void testDecodeBooleanAtBoundary() {
        var packet = new Packet(new byte[] {1});
        assertEquals(true, packet.decodeBoolean());

        var shortPacket = new Packet(new byte[] {});
        assertEquals(false, shortPacket.decodeBoolean());
    }

    @Test
    public void testDecodeShortAtBoundary() {
        var packet = new Packet(new byte[] {0x34, 0x12});
        assertEquals((short) 0x1234, packet.decodeShort());

        var shortPacket = new Packet(new byte[] {0x34});
        assertEquals((short) 0, shortPacket.decodeShort());
    }

    @Test
    public void testDecodeIntAtBoundary() {
        var packet = new Packet(new byte[] {0x78, 0x56, 0x34, 0x12});
        assertEquals(0x12345678, packet.decodeInt());

        var shortPacket = new Packet(new byte[] {0x78, 0x56, 0x34});
        assertEquals(0, shortPacket.decodeInt());
    }

    @Test
    public void testDecodeLongAtBoundary() {
        var packet = new Packet(new byte[] {0x21, 0, 0, 0, 0, 0, 0, 0});
        assertEquals(0x21L, packet.decodeLong());

        var shortPacket = new Packet(new byte[] {0x21, 0, 0, 0, 0, 0, 0});
        assertEquals(0L, shortPacket.decodeLong());
    }

    @Test
    public void testDecodeDoubleAtBoundary() {
        long bits = Double.doubleToLongBits(42.5);
        var data = new byte[8];
        for (int i = 0; i < 8; i++) {
            data[i] = (byte) (bits >> (8 * i));
        }
        var packet = new Packet(data);
        assertEquals(42.5, packet.decodeDouble());

        var shortPacket = new Packet(java.util.Arrays.copyOfRange(data, 0, 7));
        assertEquals(0.0, shortPacket.decodeDouble());
    }

    @Test
    public void testDecodeFloatAtBoundary() {
        int bits = Float.floatToIntBits(3.25f);
        var data = new byte[4];
        for (int i = 0; i < 4; i++) {
            data[i] = (byte) (bits >> (8 * i));
        }
        var packet = new Packet(data);
        assertEquals(3.25f, packet.decodeFloat());

        var shortPacket = new Packet(java.util.Arrays.copyOfRange(data, 0, 3));
        assertEquals(0.0f, shortPacket.decodeFloat());
    }

    @Test
    public void testDecodeAtBoundaryWithNonzeroReadPos() {
        // 8 bytes: two ints back to back; the second decode starts at readPos 4
        var packet = new Packet(new byte[] {1, 0, 0, 0, 2, 0, 0, 0});
        assertEquals(1, packet.decodeInt());
        assertEquals(2, packet.decodeInt());

        // 7 bytes: the second int decode must underflow and return the default
        var shortPacket = new Packet(new byte[] {1, 0, 0, 0, 2, 0, 0});
        assertEquals(1, shortPacket.decodeInt());
        assertEquals(0, shortPacket.decodeInt());
    }

    @Test
    public void testTargetCorner() {
        TargetCorner corner = new TargetCorner(1, 2);

        var packet = new Packet(0);

        packet.encode(corner);
    }

    @Test
    void pipelineResultSerde() {
        var ret1 = new PhotonPipelineResult(1, 2, 3, 1024, List.of());
        var p1 = new Packet(10);
        PhotonPipelineResult.photonStruct.pack(p1, ret1);
        var unpackedRet1 = PhotonPipelineResult.photonStruct.unpack(p1);
        assertEquals(ret1, unpackedRet1);

        var ret2 =
                new PhotonPipelineResult(
                        1,
                        2,
                        3,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        2,
                                        -1,
                                        -1f,
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        0.25,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.1,
                                        6.7,
                                        3,
                                        -1,
                                        -1f,
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
                                        0.25,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))));
        var p2 = new Packet(10);
        PhotonPipelineResult.photonStruct.pack(p2, ret2);
        var unpackedRet2 = PhotonPipelineResult.photonStruct.unpack(p2);
        assertEquals(ret2, unpackedRet2);

        var ret3 =
                new PhotonPipelineResult(
                        3,
                        4,
                        5,
                        1024,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        2,
                                        -1,
                                        -1f,
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)),
                                        0.25,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8))),
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.1,
                                        6.7,
                                        3,
                                        -1,
                                        -1f,
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
                                        new Transform3d(new Translation3d(4, 2, 3), new Rotation3d(1, 5, 3)),
                                        0.25,
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)),
                                        List.of(
                                                new TargetCorner(1, 2),
                                                new TargetCorner(3, 4),
                                                new TargetCorner(5, 6),
                                                new TargetCorner(7, 8)))),
                        Optional.of(
                                new MultiTargetPNPResult(
                                        new PnpResult(
                                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                                        List.of((short) 1, (short) 2, (short) 3))));
        var p3 = new Packet(10);
        PhotonPipelineResult.photonStruct.pack(p3, ret3);
        var unpackedRet3 = PhotonPipelineResult.photonStruct.unpack(p3);
        assertEquals(ret3, unpackedRet3);
    }
}
