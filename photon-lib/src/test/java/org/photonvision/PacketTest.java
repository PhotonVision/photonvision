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

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

class PacketTest {
    @Test
    void testSimpleTrackedTarget() {
        var target =
                new PhotonTrackedTarget(
                        3.0, 4.0, 9.0, -5.0, new Transform2d(new Translation2d(1, 2), new Rotation2d(1.5)),
                        List.of(
                                new TargetCorner(1,2),
                                new TargetCorner(3,4),
                                new TargetCorner(5,6),
                                new TargetCorner(7,8)
                        ));
        var p = new Packet(PhotonTrackedTarget.PACK_SIZE_BYTES);
        target.populatePacket(p);

        var b = new PhotonTrackedTarget();
        b.createFromPacket(p);

        Assertions.assertEquals(target, b);
    }

    @Test
    void testSimplePipelineResult() {
        var result = new PhotonPipelineResult(1, new ArrayList<>());
        var p = new Packet(result.getPacketSize());
        result.populatePacket(p);

        var b = new PhotonPipelineResult();
        b.createFromPacket(p);

        Assertions.assertEquals(result, b);

        var result2 =
                new PhotonPipelineResult(
                        2,
                        List.of(
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.0,
                                        4.0,
                                        new Transform2d(new Translation2d(1, 2), new Rotation2d(1.5)),
                                        List.of(
                                                new TargetCorner(1,2),
                                                new TargetCorner(3,4),
                                                new TargetCorner(5,6),
                                                new TargetCorner(7,8)
                                        )),
                                new PhotonTrackedTarget(
                                        3.0,
                                        -4.0,
                                        9.1,
                                        6.7,
                                        new Transform2d(new Translation2d(1, 5), new Rotation2d(1.5)),
                                        List.of(
                                                new TargetCorner(1,2),
                                                new TargetCorner(3,4),
                                                new TargetCorner(5,6),
                                                new TargetCorner(7,8)
                                        ))));
        var p2 = new Packet(result2.getPacketSize());
        result2.populatePacket(p2);

        var b2 = new PhotonPipelineResult();
        b2.createFromPacket(p2);

        Assertions.assertEquals(result2, b2);
    }
}
