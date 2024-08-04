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
import org.junit.jupiter.api.Test;

public class MultiTargetPNPResultTest {
    @Test
    public void equalityTest() {
        var a = new MultiTargetPNPResult();
        var b = new MultiTargetPNPResult();
        assertEquals(a, b);

        a =
                new MultiTargetPNPResult(
                        new PnpResult(
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of((short) 1, (short) 2, (short) 3));

        b =
                new MultiTargetPNPResult(
                        new PnpResult(
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of((short) 1, (short) 2, (short) 3));

        assertEquals(a, b);
    }

    @Test
    public void inequalityTest() {
        var a =
                new MultiTargetPNPResult(
                        new PnpResult(
                                new Transform3d(new Translation3d(1, 8, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of((short) 3, (short) 4, (short) 7));
        var b =
                new MultiTargetPNPResult(
                        new PnpResult(
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of((short) 1, (short) 2, (short) 3));

        assertNotEquals(a, b);
    }
}
