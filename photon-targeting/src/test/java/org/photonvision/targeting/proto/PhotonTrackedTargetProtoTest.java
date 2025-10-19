/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
