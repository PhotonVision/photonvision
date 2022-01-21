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

import java.util.EnumSet;
import java.util.HashMap;

public enum ContourShape {
    Circle(0),
    Custom(-1),
    Triangle(3),
    Quadrilateral(4);

    public final int sides;

    ContourShape(int sides) {
        this.sides = sides;
    }

    private static final HashMap<Integer, ContourShape> sidesToValueMap = new HashMap<>();

    static {
        for (var value : EnumSet.allOf(ContourShape.class)) {
            sidesToValueMap.put(value.sides, value);
        }
    }

    public static ContourShape fromSides(int sides) {
        return sidesToValueMap.get(sides);
    }
}
