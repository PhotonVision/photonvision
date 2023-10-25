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

package org.photonvision.vision.opencv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.photonvision.common.util.TestUtils;

public class ContourTest {
    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void simpleContourTest() {
        var mat = new MatOfPoint();
        mat.fromList(List.of(new Point(0, 0), new Point(10, 0), new Point(10, 10), new Point(0, 10)));
        var contour = new Contour(mat);
        assertEquals(100, contour.getArea());
        assertEquals(40, contour.getPerimeter());
        assertEquals(new Point(5, 5), contour.getCenterPoint());
    }

    @Test
    public void test2019() {
        var firstMat = new MatOfPoint();
        // contour 0 and 1 data from kCargoStraightDark72in_HighRes
        firstMat.fromList(
                List.of(
                        new Point(1328, 976),
                        new Point(1272, 985),
                        new Point(1230, 832),
                        new Point(1326, 948),
                        new Point(1328, 971)));

        var secondMat = new MatOfPoint();
        secondMat.fromList(
                List.of(
                        new Point(956, 832),
                        new Point(882, 978),
                        new Point(927, 810),
                        new Point(954, 821),
                        new Point(956, 825)));
        var firstContour = new Contour(firstMat);
        var secondContour = new Contour(secondMat);
        boolean result = firstContour.isIntersecting(secondContour, ContourIntersectionDirection.Up);
        assertTrue(result);
    }
}
