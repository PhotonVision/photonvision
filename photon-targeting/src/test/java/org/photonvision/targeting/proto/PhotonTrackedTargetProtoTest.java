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

package org.photonvision.targeting.proto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.proto.Photon.ProtobufPhotonTrackedTarget;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import us.hebi.quickbuf.RepeatedMessage;

public class PhotonTrackedTargetProtoTest {
    @Test
    public void protobufTest() {
        var target =
                new PhotonTrackedTarget(
                        3.0,
                        4.0,
                        9.0,
                        -5.0,
                        -1,
                        -1,
                        -1f,
                        new Transform3d(new Translation3d(), new Rotation3d()),
                        new Transform3d(new Translation3d(), new Rotation3d()),
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
                                new TargetCorner(7, 8)));
        var serializedTarget = PhotonTrackedTarget.proto.createMessage();
        PhotonTrackedTarget.proto.pack(serializedTarget, target);
        var unpackedTarget = PhotonTrackedTarget.proto.unpack(serializedTarget);
        assertEquals(target, unpackedTarget);
    }

    @Test
    public void protobufListTest() {
        List<PhotonTrackedTarget> targets = List.of();
        var serializedTargets =
                RepeatedMessage.newEmptyInstance(ProtobufPhotonTrackedTarget.getFactory());
        PhotonTrackedTarget.proto.pack(serializedTargets, targets);
        var unpackedTargets = PhotonTrackedTarget.proto.unpack(serializedTargets);
        assertEquals(targets, unpackedTargets);

        targets =
                List.of(
                        new PhotonTrackedTarget(
                                3.0,
                                4.0,
                                9.0,
                                -5.0,
                                -1,
                                -1,
                                -1f,
                                new Transform3d(new Translation3d(), new Rotation3d()),
                                new Transform3d(new Translation3d(), new Rotation3d()),
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
                                7.0,
                                2.0,
                                1.0,
                                -9.0,
                                -1,
                                -1,
                                -1f,
                                new Transform3d(new Translation3d(), new Rotation3d()),
                                new Transform3d(new Translation3d(), new Rotation3d()),
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
                                        new TargetCorner(7, 8))));
        serializedTargets = RepeatedMessage.newEmptyInstance(ProtobufPhotonTrackedTarget.getFactory());
        PhotonTrackedTarget.proto.pack(serializedTargets, targets);
        unpackedTargets = PhotonTrackedTarget.proto.unpack(serializedTargets);
        assertEquals(targets, unpackedTargets);
    }
}
