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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Point3;
import org.photonvision.common.LoadJNI;
import org.wpilib.math.util.Units;

public class TargetModelTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    @Test
    void testCircleTargetGeneration() {
        assertApproxEquals(
                List.of(
                        new Point3(
                                -Units.inchesToMeters(7) / 2,
                                -Units.inchesToMeters(7) / 2,
                                -Units.inchesToMeters(7) / 2),
                        new Point3(
                                -Units.inchesToMeters(7) / 2,
                                Units.inchesToMeters(7) / 2,
                                -Units.inchesToMeters(7) / 2),
                        new Point3(
                                Units.inchesToMeters(7) / 2,
                                Units.inchesToMeters(7) / 2,
                                -Units.inchesToMeters(7) / 2),
                        new Point3(
                                Units.inchesToMeters(7) / 2,
                                -Units.inchesToMeters(7) / 2,
                                -Units.inchesToMeters(7) / 2)),
                TargetModel.kCircularPowerCell7in.getRealWorldTargetCoordinates().toList(),
                1E-6);
    }

    @Test
    void testSquareTargetGeneration() {
        assertApproxEquals(
                List.of(
                        new Point3(-Units.inchesToMeters(6.5 / 2.0), -Units.inchesToMeters(6.5 / 2.0), 0),
                        new Point3(-Units.inchesToMeters(6.5 / 2.0), Units.inchesToMeters(6.5 / 2.0), 0),
                        new Point3(Units.inchesToMeters(6.5 / 2.0), Units.inchesToMeters(6.5 / 2.0), 0),
                        new Point3(Units.inchesToMeters(6.5 / 2.0), -Units.inchesToMeters(6.5 / 2.0), 0)),
                TargetModel.kAprilTag6p5in_36h11.getRealWorldTargetCoordinates().toList(),
                1E-6);
    }

    static void assertApproxEquals(List<Point3> expected, List<Point3> actual, double delta) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < actual.size(); i++) {
            assertEquals(expected.get(i).x, actual.get(i).x, delta, "Bad x for point %d".formatted(i));
            assertEquals(expected.get(i).y, actual.get(i).y, delta, "Bad y for point %d".formatted(i));
            assertEquals(expected.get(i).z, actual.get(i).z, delta, "Bad z for point %d".formatted(i));
        }
    }
}
