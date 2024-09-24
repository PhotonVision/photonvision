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

package org.photonvision.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class PhotonPipelineResultTest {
    @Test
    public void equalityTest() {
        var a = new PhotonPipelineResult();
        var b = new PhotonPipelineResult();
        assertEquals(a, b);

        a =
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
        b =
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
        assertEquals(a, b);

        a =
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
        b =
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
        assertEquals(a, b);
    }

    @Test
    public void inequalityTest() {
        var a =
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
                                        7.0,
                                        2.0,
                                        1.0,
                                        -9.0,
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
        var b =
                new PhotonPipelineResult(
                        3,
                        4,
                        5,
                        List.of(
                                new PhotonTrackedTarget(
                                        7.0,
                                        2.0,
                                        1.0,
                                        -9.0,
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
        assertNotEquals(a, b);

        a =
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
                                                new Transform3d(new Translation3d(1, 8, 3), new Rotation3d(1, 2, 3)), 0.1),
                                        List.of((short) 3, (short) 4, (short) 7))));
        b =
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
        assertNotEquals(a, b);
    }
}
