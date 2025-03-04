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

package org.photonvision.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import org.junit.jupiter.api.Test;

public class PNPResultTest {
    @Test
    public void equalityTest() {
        var a = new PnpResult();
        var b = new PnpResult();
        assertEquals(a, b);

        a = new PnpResult(new Transform3d(0, 1, 2, new Rotation3d()), 0.0);
        b = new PnpResult(new Transform3d(0, 1, 2, new Rotation3d()), 0.0);
        assertEquals(a, b);

        a =
                new PnpResult(
                        new Transform3d(0, 1, 2, new Rotation3d()),
                        new Transform3d(3, 4, 5, new Rotation3d()),
                        0.5,
                        0.1,
                        0.1);
        b =
                new PnpResult(
                        new Transform3d(0, 1, 2, new Rotation3d()),
                        new Transform3d(3, 4, 5, new Rotation3d()),
                        0.5,
                        0.1,
                        0.1);
        assertEquals(a, b);
    }

    @Test
    public void inequalityTest() {
        var a = new PnpResult(new Transform3d(0, 1, 2, new Rotation3d()), 0.0);
        var b = new PnpResult(new Transform3d(3, 4, 5, new Rotation3d()), 0.1);
        assertNotEquals(a, b);

        a =
                new PnpResult(
                        new Transform3d(3, 4, 5, new Rotation3d()),
                        new Transform3d(0, 1, 2, new Rotation3d()),
                        0.5,
                        0.1,
                        0.1);
        b =
                new PnpResult(
                        new Transform3d(3, 4, 5, new Rotation3d()),
                        new Transform3d(0, 1, 2, new Rotation3d()),
                        0.5,
                        0.1,
                        0.2);
        assertNotEquals(a, b);
    }
}
