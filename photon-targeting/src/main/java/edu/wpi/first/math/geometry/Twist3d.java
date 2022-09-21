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

import java.util.Objects;

/**
 * A change in distance along a 3D arc since the last pose update. We can use ideas from
 * differential calculus to create new Pose3ds from a Twist3d and vise versa.
 *
 * <p>A Twist can be used to represent a difference between two poses.
 */
@SuppressWarnings("MemberName")
public class Twist3d {
    /** Linear "dx" component. */
    public double dx;

    /** Linear "dy" component. */
    public double dy;

    /** Linear "dz" component. */
    public double dz;

    /** Angular "droll" component (radians). */
    public double droll;

    /** Angular "dpitch" component (radians). */
    public double dpitch;

    /** Angular "dyaw" component (radians). */
    public double dyaw;

    public Twist3d() {}

    /**
     * Constructs a Twist3d with the given values.
     *
     * @param dx Change in x direction relative to robot.
     * @param dy Change in y direction relative to robot.
     * @param dz Change in z direction relative to robot.
     * @param droll Change in roll relative to the robot.
     * @param dpitch Change in pitch relative to the robot.
     * @param dyaw Change in yaw relative to the robot.
     */
    public Twist3d(double dx, double dy, double dz, double droll, double dpitch, double dyaw) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.droll = droll;
        this.dpitch = dpitch;
        this.dyaw = dyaw;
    }

    @Override
    public String toString() {
        return String.format(
                "Twist3d(dX: %.2f, dY: %.2f, dZ: %.2f, dRoll: %.2f, dPitch: %.2f, dYaw: %.2f)",
                dx, dy, dz, droll, dpitch, dyaw);
    }

    /**
     * Checks equality between this Twist3d and another object.
     *
     * @param obj The other object.
     * @return Whether the two objects are equal or not.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Twist3d) {
            return Math.abs(((Twist3d) obj).dx - dx) < 1E-9
                    && Math.abs(((Twist3d) obj).dy - dy) < 1E-9
                    && Math.abs(((Twist3d) obj).dz - dz) < 1E-9
                    && Math.abs(((Twist3d) obj).droll - droll) < 1E-9
                    && Math.abs(((Twist3d) obj).dpitch - dpitch) < 1E-9
                    && Math.abs(((Twist3d) obj).dyaw - dyaw) < 1E-9;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dx, dy, dz, droll, dpitch, dyaw);
    }
}
