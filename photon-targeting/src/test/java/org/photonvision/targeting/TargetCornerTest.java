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

import org.junit.jupiter.api.Test;

public class TargetCornerTest {
    @Test
    public void equalityTest() {
        var a = new TargetCorner(0, 1);
        var b = new TargetCorner(0, 1);

        assertEquals(a, b);
    }

    @Test
    public void inequalityTest() {
        var a = new TargetCorner(0, 1);
        var b = new TargetCorner(2, 4);

        assertNotEquals(a, b);
    }
}
