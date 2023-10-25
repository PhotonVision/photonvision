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
package org.photonvision.vision.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.DualOffsetValues;

public class TrackedTargetTest {
    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    void axisTest() {
        MatOfPoint mat = new MatOfPoint();
        mat.fromList(
                List.of(
                        new Point(400, 298),
                        new Point(426.22, 298),
                        new Point(426.22, 302),
                        new Point(400, 302))); // gives contour with center of 426, 300
        Contour contour = new Contour(mat);

        var pTarget = new PotentialTarget(contour);

        var imageSize = new Size(800, 600);

        var setting =
                new TrackedTarget.TargetCalculationParameters(
                        false,
                        TargetOffsetPointEdge.Center,
                        RobotOffsetPointMode.None,
                        new Point(0, 0),
                        new DualOffsetValues(),
                        new Point(imageSize.width / 2, imageSize.height / 2),
                        61,
                        34.3,
                        imageSize.area());

        var trackedTarget = new TrackedTarget(pTarget, setting, null);
        // TODO change these hardcoded values
        assertEquals(12.0, trackedTarget.getYaw(), 0.05, "Yaw was incorrect");
        assertEquals(0, trackedTarget.getPitch(), 0.05, "Pitch was incorrect");
    }
}
