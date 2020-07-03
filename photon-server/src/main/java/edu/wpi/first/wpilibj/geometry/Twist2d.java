/*
 * Copyright (C) 2020 Photon Vision.
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

package edu.wpi.first.wpilibj.geometry;

import java.util.Objects;

/**
* A change in distance along arc since the last pose update. We can use ideas from differential
* calculus to create new Pose2ds from a Twist2d and vise versa.
*
* <p>A Twist can be used to represent a difference between two poses.
*/
@SuppressWarnings("MemberName")
public class Twist2d {
    /** Linear "dx" component. */
    public double dx;

    /** Linear "dy" component. */
    public double dy;

    /** Angular "dtheta" component (radians). */
    public double dtheta;

    public Twist2d() {}

    /**
    * Constructs a Twist2d with the given values.
    *
    * @param dx Change in x direction relative to robot.
    * @param dy Change in y direction relative to robot.
    * @param dtheta Change in angle relative to robot.
    */
    public Twist2d(double dx, double dy, double dtheta) {
        this.dx = dx;
        this.dy = dy;
        this.dtheta = dtheta;
    }

    @Override
    public String toString() {
        return String.format("Twist2d(dX: %.2f, dY: %.2f, dTheta: %.2f)", dx, dy, dtheta);
    }

    /**
    * Checks equality between this Twist2d and another object.
    *
    * @param obj The other object.
    * @return Whether the two objects are equal or not.
    */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Twist2d) {
            return Math.abs(((Twist2d) obj).dx - dx) < 1E-9
                    && Math.abs(((Twist2d) obj).dy - dy) < 1E-9
                    && Math.abs(((Twist2d) obj).dtheta - dtheta) < 1E-9;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dx, dy, dtheta);
    }
}
