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

import edu.wpi.first.math.geometry.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import org.photonvision.utils.PacketUtils;

class PacketTest {
    @Test
    void rotation2dSerde() {
        var packet = new Packet(PacketUtils.ROTATION2D_BYTE_SIZE);
        var ret = new Rotation2d();
        PacketUtils.packRotation2d(packet, ret);
        var unpacked = PacketUtils.unpackRotation2d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void quaternionSerde() {
        var packet = new Packet(PacketUtils.QUATERNION_BYTE_SIZE);
        var ret = new Quaternion();
        PacketUtils.packQuaternion(packet, ret);
        var unpacked = PacketUtils.unpackQuaternion(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void rotation3dSerde() {
        var packet = new Packet(PacketUtils.ROTATION3D_BYTE_SIZE);
        var ret = new Rotation3d();
        PacketUtils.packRotation3d(packet, ret);
        var unpacked = PacketUtils.unpackRotation3d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void translation2dSerde() {
        var packet = new Packet(PacketUtils.TRANSLATION2D_BYTE_SIZE);
        var ret = new Translation2d();
        PacketUtils.packTranslation2d(packet, ret);
        var unpacked = PacketUtils.unpackTranslation2d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void translation3dSerde() {
        var packet = new Packet(PacketUtils.TRANSLATION3D_BYTE_SIZE);
        var ret = new Translation3d();
        PacketUtils.packTranslation3d(packet, ret);
        var unpacked = PacketUtils.unpackTranslation3d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void transform2dSerde() {
        var packet = new Packet(PacketUtils.TRANSFORM2D_BYTE_SIZE);
        var ret = new Transform2d();
        PacketUtils.packTransform2d(packet, ret);
        var unpacked = PacketUtils.unpackTransform2d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void transform3dSerde() {
        var packet = new Packet(PacketUtils.TRANSFORM3D_BYTE_SIZE);
        var ret = new Transform3d();
        PacketUtils.packTransform3d(packet, ret);
        var unpacked = PacketUtils.unpackTransform3d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void pose2dSerde() {
        var packet = new Packet(PacketUtils.POSE2D_BYTE_SIZE);
        var ret = new Pose2d();
        PacketUtils.packPose2d(packet, ret);
        var unpacked = PacketUtils.unpackPose2d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void pose3dSerde() {
        var packet = new Packet(PacketUtils.POSE3D_BYTE_SIZE);
        var ret = new Pose3d();
        PacketUtils.packPose3d(packet, ret);
        var unpacked = PacketUtils.unpackPose3d(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void targetCornerSerde() {
        var packet = new Packet(TargetCorner.serde.getMaxByteSize());
        var ret = new TargetCorner(0.0, 1.0);
        TargetCorner.serde.pack(packet, ret);
        var unpacked = TargetCorner.serde.unpack(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void pnpResultSerde() {
        var packet = new Packet(PNPResult.serde.getMaxByteSize());
        var ret = new PNPResult();
        PNPResult.serde.pack(packet, ret);
        var unpackedRet = PNPResult.serde.unpack(packet);
        assertEquals(ret, unpackedRet);

        var packet1 = new Packet(PNPResult.serde.getMaxByteSize());
        var ret1 =
                new PNPResult(new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1);
        PNPResult.serde.pack(packet1, ret1);
        var unpackedRet1 = PNPResult.serde.unpack(packet1);
        assertEquals(ret1, unpackedRet1);
    }

    @Test
    void multitagResultSerde() {
        var packet = new Packet(MultiTargetPNPResult.serde.getMaxByteSize());
        var ret =
                new MultiTargetPNPResult(
                        new PNPResult(
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of(1, 2, 3));
        MultiTargetPNPResult.serde.pack(packet, ret);
        var unpackedRet = MultiTargetPNPResult.serde.unpack(packet);
        assertEquals(ret, unpackedRet);
    }

    @Test
    void trackedTargetSerde() {
        var packet = new Packet(PhotonTrackedTarget.serde.getMaxByteSize());
        var ret =
                new PhotonTrackedTarget(
                        3.0,
                        4.0,
                        9.0,
                        -5.0,
                        -1,
                        new Transform3d(),
                        new Transform3d(),
                        -1,
                        List.of(
                                new TargetCorner(1, 2),
                                new TargetCorner(3, 4),
                                new TargetCorner(5, 6),
                                new TargetCorner(7, 8)),
                        List.of(
                                new TargetCorner(1, 2),
                                new TargetCorner(3, 4),
                                new TargetCorner(5, 6),
                                new TargetCorner(7, 8)));
        PhotonTrackedTarget.serde.pack(packet, ret);
        var unpacked = PhotonTrackedTarget.serde.unpack(packet);
        assertEquals(ret, unpacked);
    }

    @Test
    void pipelineResultSerde() {
        var ret1 = new PhotonPipelineResult(1, 2, 3, List.of());
        var p1 = new Packet(ret1.getPacketSize());
        PhotonPipelineResult.serde.pack(p1, ret1);
        var unpackedRet1 = PhotonPipelineResult.serde.unpack(p1);
        assertEquals(ret1, unpackedRet1);

        var ret2 =
                new PhotonPipelineResult(
                        1,
                        2,
                        3,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        2,
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
        var p2 = new Packet(ret2.getPacketSize());
        PhotonPipelineResult.serde.pack(p2, ret2);
        var unpackedRet2 = PhotonPipelineResult.serde.unpack(p2);
        assertEquals(ret2, unpackedRet2);

        var ret3 =
                new PhotonPipelineResult(
                        3,
                        4,
                        5,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        2,
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
                        new MultiTargetPNPResult(
                                new PNPResult(
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                                List.of(1, 2, 3)));
        var p3 = new Packet(ret3.getPacketSize());
        PhotonPipelineResult.serde.pack(p3, ret3);
        var unpackedRet3 = PhotonPipelineResult.serde.unpack(p3);
        assertEquals(ret3, unpackedRet3);
    }

    @Test
    public void testMultiTargetSerde() {
        var result =
                new PhotonPipelineResult(
                        3,
                        4,
                        5,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        2,
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
                        new MultiTargetPNPResult(
                                new PNPResult(
                                        new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                                List.of(1, 2, 3)));
    }
}
