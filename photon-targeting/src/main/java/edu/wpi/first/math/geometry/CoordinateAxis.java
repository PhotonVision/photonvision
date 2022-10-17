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

package edu.wpi.first.math.geometry;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.numbers.N3;

/** A class representing a coordinate system axis within the NWU coordinate system. */
public class CoordinateAxis {
    final Vector<N3> m_axis;

    /**
     * Constructs a coordinate system axis within the NWU coordinate system and normalizes it.
     *
     * @param x The x component.
     * @param y The y component.
     * @param z The z component.
     */
    public CoordinateAxis(double x, double y, double z) {
        double norm = Math.sqrt(x * x + y * y + z * z);
        m_axis = VecBuilder.fill(x / norm, y / norm, z / norm);
    }

    /**
     * Returns a coordinate axis corresponding to +X in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to +X in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis N() {
        return new CoordinateAxis(1.0, 0.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to -X in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to -X in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis S() {
        return new CoordinateAxis(-1.0, 0.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to -Y in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to -Y in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis E() {
        return new CoordinateAxis(0.0, -1.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to +Y in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to +Y in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis W() {
        return new CoordinateAxis(0.0, 1.0, 0.0);
    }

    /**
     * Returns a coordinate axis corresponding to +Z in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to +Z in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis U() {
        return new CoordinateAxis(0.0, 0.0, 1.0);
    }

    /**
     * Returns a coordinate axis corresponding to -Z in the NWU coordinate system.
     *
     * @return A coordinate axis corresponding to -Z in the NWU coordinate system.
     */
    @SuppressWarnings("MethodName")
    public static CoordinateAxis D() {
        return new CoordinateAxis(0.0, 0.0, -1.0);
    }
}
