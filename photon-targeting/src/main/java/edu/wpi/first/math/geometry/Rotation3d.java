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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.interpolation.Interpolatable;
import edu.wpi.first.math.numbers.N3;
import java.util.Objects;

/** A rotation in a 3D coordinate frame represented by a quaternion. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Rotation3d implements Interpolatable<Rotation3d> {
    private Quaternion m_q = new Quaternion();

    /** Constructs a Rotation3d with a default angle of 0 degrees. */
    public Rotation3d() {}

    /**
     * Constructs a Rotation3d from a quaternion.
     *
     * @param q The quaternion.
     */
    @JsonCreator
    public Rotation3d(@JsonProperty(required = true, value = "quaternion") Quaternion q) {
        m_q = q.normalize();
    }

    /**
     * Constructs a Rotation3d from extrinsic roll, pitch, and yaw.
     *
     * <p>Extrinsic rotations occur in that order around the axes in the fixed global frame rather
     * than the body frame.
     *
     * @param roll The roll angle in radians.
     * @param pitch The pitch angle in radians.
     * @param yaw The yaw angle in radians.
     */
    public Rotation3d(double roll, double pitch, double yaw) {
        // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Euler_angles_to_quaternion_conversion
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);

        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);

        m_q =
                new Quaternion(
                        cr * cp * cy + sr * sp * sy,
                        sr * cp * cy - cr * sp * sy,
                        cr * sp * cy + sr * cp * sy,
                        cr * cp * sy - sr * sp * cy);
    }

    /**
     * Constructs a Rotation3d with the given axis and angle. The axis doesn't have to be normalized.
     *
     * @param axis The rotation axis.
     * @param angleRadians The rotation around the axis in radians.
     */
    public Rotation3d(Vector<N3> axis, double angleRadians) {
        double norm = axis.normF();
        if (norm == 0.0) {
            return;
        }

        // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Definition
        var v = axis.times(1.0 / norm).times(Math.sin(angleRadians / 2.0));
        m_q = new Quaternion(Math.cos(angleRadians / 2.0), v.get(0, 0), v.get(1, 0), v.get(2, 0));
    }

    /**
     * Constructs a quaternion from a 3x3, row-major direction cosine matrix
     * https://intra.ece.ucr.edu/~farrell/AidedNavigation/D_App_Quaternions/Rot2Quat.pdf
     *
     * @param dcm A 3x3 direction cosine matrix
     */
    public Rotation3d(Matrix<N3, N3> dcm) {
        double b1 = 0.5 * Math.sqrt(1 + dcm.get(0, 0) + dcm.get(1, 1) + dcm.get(2, 2));
        double b2 = (dcm.get(2, 1) - dcm.get(1, 2)) / (4 * b1);
        double b3 = (dcm.get(0, 2) - dcm.get(2, 0)) / (4 * b1);
        double b4 = (dcm.get(1, 0) - dcm.get(0, 1)) / (4 * b1);

        m_q = new Quaternion(b1, b2, b3, b4).normalize();
    }

    /**
     * Adds two rotations together.
     *
     * @param other The rotation to add.
     * @return The sum of the two rotations.
     */
    public Rotation3d plus(Rotation3d other) {
        return rotateBy(other);
    }

    /**
     * Subtracts the new rotation from the current rotation and returns the new rotation.
     *
     * @param other The rotation to subtract.
     * @return The difference between the two rotations.
     */
    public Rotation3d minus(Rotation3d other) {
        return rotateBy(other.unaryMinus());
    }

    /**
     * Takes the inverse of the current rotation.
     *
     * @return The inverse of the current rotation.
     */
    public Rotation3d unaryMinus() {
        return new Rotation3d(m_q.inverse());
    }

    /**
     * Multiplies the current rotation by a scalar.
     *
     * @param scalar The scalar.
     * @return The new scaled Rotation3d.
     */
    public Rotation3d times(double scalar) {
        // https://en.wikipedia.org/wiki/Slerp#Quaternion_Slerp
        if (m_q.getW() >= 0.0) {
            return new Rotation3d(
                    VecBuilder.fill(m_q.getX(), m_q.getY(), m_q.getZ()),
                    2.0 * scalar * Math.acos(m_q.getW()));
        } else {
            return new Rotation3d(
                    VecBuilder.fill(-m_q.getX(), -m_q.getY(), -m_q.getZ()),
                    2.0 * scalar * Math.acos(-m_q.getW()));
        }
    }

    /**
     * Returns a Rotation2d representing this Rotation3d projected into the X-Y plane.
     *
     * @return A Rotation2d representing this Rotation3d projected into the X-Y plane.
     */
    public Rotation2d toRotation2d() {
        return new Rotation2d(getZ());
    }

    /**
     * Adds the new rotation to the current rotation.
     *
     * @param other The rotation to rotate by.
     * @return The new rotated Rotation3d.
     */
    public Rotation3d rotateBy(Rotation3d other) {
        return new Rotation3d(other.m_q.times(m_q));
    }

    /**
     * Returns the quaternion representation of the Rotation3d.
     *
     * @return The quaternion representation of the Rotation3d.
     */
    @JsonProperty(value = "quaternion")
    public Quaternion getQuaternion() {
        return m_q;
    }

    /**
     * Returns the counterclockwise rotation angle around the X axis (roll) in radians.
     *
     * @return The counterclockwise rotation angle around the X axis (roll) in radians.
     */
    public double getX() {
        final var w = m_q.getW();
        final var x = m_q.getX();
        final var y = m_q.getY();
        final var z = m_q.getZ();

        // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Quaternion_to_Euler_angles_conversion
        return Math.atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y));
    }

    /**
     * Returns the counterclockwise rotation angle around the Y axis (pitch) in radians.
     *
     * @return The counterclockwise rotation angle around the Y axis (pitch) in radians.
     */
    public double getY() {
        final var w = m_q.getW();
        final var x = m_q.getX();
        final var y = m_q.getY();
        final var z = m_q.getZ();

        // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Quaternion_to_Euler_angles_conversion
        double ratio = 2.0 * (w * y - z * x);
        if (Math.abs(ratio) >= 1.0) {
            return Math.copySign(Math.PI / 2.0, ratio);
        } else {
            return Math.asin(ratio);
        }
    }

    /**
     * Returns the counterclockwise rotation angle around the Z axis (yaw) in radians.
     *
     * @return The counterclockwise rotation angle around the Z axis (yaw) in radians.
     */
    public double getZ() {
        final var w = m_q.getW();
        final var x = m_q.getX();
        final var y = m_q.getY();
        final var z = m_q.getZ();

        // https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Quaternion_to_Euler_angles_conversion
        return Math.atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z));
    }

    /**
     * Returns the axis in the axis-angle representation of this rotation.
     *
     * @return The axis in the axis-angle representation.
     */
    public Vector<N3> getAxis() {
        double norm =
                Math.sqrt(m_q.getX() * m_q.getX() + m_q.getY() * m_q.getY() + m_q.getZ() * m_q.getZ());
        return VecBuilder.fill(m_q.getX() / norm, m_q.getY() / norm, m_q.getZ() / norm);
    }

    /**
     * Returns the angle in radians in the axis-angle representation of this rotation.
     *
     * @return The angle in radians in the axis-angle representation of this rotation.
     */
    public double getAngle() {
        double norm =
                Math.sqrt(m_q.getX() * m_q.getX() + m_q.getY() * m_q.getY() + m_q.getZ() * m_q.getZ());
        return 2.0 * Math.atan2(norm, m_q.getW());
    }

    /**
     * Checks equality between this Rotation3d and another object.
     *
     * @param obj The other object.
     * @return Whether the two objects are equal or not.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rotation3d) {
            var other = (Rotation3d) obj;
            return m_q.equals(other.m_q);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_q);
    }

    @Override
    @SuppressWarnings("ParameterName")
    public Rotation3d interpolate(Rotation3d endValue, double t) {
        return plus(endValue.minus(this).times(MathUtil.clamp(t, 0, 1)));
    }

    @Override
    public String toString() {
        return String.format("Rotation3d(%s)", m_q);
    }
}
