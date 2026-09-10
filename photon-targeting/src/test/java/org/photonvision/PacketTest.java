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
    public void testTargetCorner() {
        TargetCorner corner = new TargetCorner(1, 2);

        var packet = new Packet(0);

        packet.encode(corner);
    }

    @Test
    public void pipelineResultSerde() {
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

    public void VLASerde() {
        var ret1 =
                List.of(
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
                                                        new TargetCorner(7, 8))))),
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
                                                        new TargetCorner(7, 8))))),
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
                                                        new TargetCorner(7, 8))))));
        var p1 = new Packet(10);
        p1.encodeList(ret1);
        var unpackedRet1 = p1.decodeList(PhotonPipelineResult.photonStruct);
        assertEquals(ret1, unpackedRet1);

        List<PhotonPipelineResult> ret2 = List.of();
        var p2 = new Packet(10);
        p2.encodeList(ret2);
        var unpackedRet2 = p2.decodeList(PhotonPipelineResult.photonStruct);
        assertEquals(ret2, unpackedRet2);
    }

    public void optionalSerde() {
        var ret1 =
                Optional.of(
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
                                                        new TargetCorner(7, 8))))));
        var p1 = new Packet(10);
        p1.encodeOptional(ret1);
        var unpackedRet1 = p1.decodeOptional(PhotonPipelineResult.photonStruct);
        assertEquals(p1, unpackedRet1);

        Optional<PhotonPipelineResult> ret2 = Optional.empty();
        var p2 = new Packet(10);
        p2.encodeOptional(ret2);
        var unpackedRet2 = p2.decodeOptional(PhotonPipelineResult.photonStruct);
        assertEquals(ret2, unpackedRet2);
    }

    public void encodePhotonStruct() {
        var ret1 =
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
        var p1 = new Packet(10);
        p1.encode(ret1);
        var unpackedRet1 = p1.decode(ret1);
        assertEquals(ret1, unpackedRet1);
    }
}
