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
import edu.wpi.first.math.interpolation.Interpolatable;
import java.util.Objects;

/**
 * Represents a translation in 3D space. This object can be used to represent a point or a vector.
 *
 * <p>This assumes that you are using conventional mathematical axes. When the robot is at the
 * origin facing in the positive X direction, forward is positive X, left is positive Y, and up is
 * positive Z.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Translation3d implements Interpolatable<Translation3d> {
    private final double m_x;
    private final double m_y;
    private final double m_z;

    /** Constructs a Translation3d with X, Y, and Z components equal to zero. */
    public Translation3d() {
        this(0.0, 0.0, 0.0);
    }

    /**
     * Constructs a Translation3d with the X, Y, and Z components equal to the provided values.
     *
     * @param x The x component of the translation.
     * @param y The y component of the translation.
     * @param z The z component of the translation.
     */
    @JsonCreator
    public Translation3d(
            @JsonProperty(required = true, value = "x") double x,
            @JsonProperty(required = true, value = "y") double y,
            @JsonProperty(required = true, value = "z") double z) {
        m_x = x;
        m_y = y;
        m_z = z;
    }

    /**
     * Constructs a Translation3d with the provided distance and angle. This is essentially converting
     * from polar coordinates to Cartesian coordinates.
     *
     * @param distance The distance from the origin to the end of the translation.
     * @param angle The angle between the x-axis and the translation vector.
     */
    public Translation3d(double distance, Rotation3d angle) {
        final var rectangular = new Translation3d(distance, 0.0, 0.0).rotateBy(angle);
        m_x = rectangular.getX();
        m_y = rectangular.getY();
        m_z = rectangular.getZ();
    }

    /**
     * Calculates the distance between two translations in 3D space.
     *
     * <p>The distance between translations is defined as √((x2−x1)²+(y2−y1)²+(z2−z1)²).
     *
     * @param other The translation to compute the distance to.
     * @return The distance between the two translations.
     */
    public double getDistance(Translation3d other) {
        return Math.sqrt(
                Math.pow(other.m_x - m_x, 2) + Math.pow(other.m_y - m_y, 2) + Math.pow(other.m_z - m_z, 2));
    }

    /**
     * Returns the X component of the translation.
     *
     * @return The X component of the translation.
     */
    @JsonProperty
    public double getX() {
        return m_x;
    }

    /**
     * Returns the Y component of the translation.
     *
     * @return The Y component of the translation.
     */
    @JsonProperty
    public double getY() {
        return m_y;
    }

    /**
     * Returns the Z component of the translation.
     *
     * @return The Z component of the translation.
     */
    @JsonProperty
    public double getZ() {
        return m_z;
    }

    /**
     * Returns the norm, or distance from the origin to the translation.
     *
     * @return The norm of the translation.
     */
    public double getNorm() {
        return Math.sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
    }

    /**
     * Applies a rotation to the translation in 3D space.
     *
     * <p>For example, rotating a Translation3d of &lt;2, 0, 0&gt; by 90 degrees around the Z axis
     * will return a Translation3d of &lt;0, 2, 0&gt;.
     *
     * @param other The rotation to rotate the translation by.
     * @return The new rotated translation.
     */
    public Translation3d rotateBy(Rotation3d other) {
        final var p = new Quaternion(0.0, m_x, m_y, m_z);
        final var qprime = other.getQuaternion().times(p).times(other.getQuaternion().inverse());
        return new Translation3d(qprime.getX(), qprime.getY(), qprime.getZ());
    }

    /**
     * Returns the sum of two translations in 3D space.
     *
     * <p>For example, Translation3d(1.0, 2.5, 3.5) + Translation3d(2.0, 5.5, 7.5) =
     * Translation3d{3.0, 8.0, 11.0).
     *
     * @param other The translation to add.
     * @return The sum of the translations.
     */
    public Translation3d plus(Translation3d other) {
        return new Translation3d(m_x + other.m_x, m_y + other.m_y, m_z + other.m_z);
    }

    /**
     * Returns the difference between two translations.
     *
     * <p>For example, Translation3d(5.0, 4.0, 3.0) - Translation3d(1.0, 2.0, 3.0) =
     * Translation3d(4.0, 2.0, 0.0).
     *
     * @param other The translation to subtract.
     * @return The difference between the two translations.
     */
    public Translation3d minus(Translation3d other) {
        return new Translation3d(m_x - other.m_x, m_y - other.m_y, m_z - other.m_z);
    }

    /**
     * Returns the inverse of the current translation. This is equivalent to negating all components
     * of the translation.
     *
     * @return The inverse of the current translation.
     */
    public Translation3d unaryMinus() {
        return new Translation3d(-m_x, -m_y, -m_z);
    }

    /**
     * Returns the translation multiplied by a scalar.
     *
     * <p>For example, Translation3d(2.0, 2.5, 4.5) * 2 = Translation3d(4.0, 5.0, 9.0).
     *
     * @param scalar The scalar to multiply by.
     * @return The scaled translation.
     */
    public Translation3d times(double scalar) {
        return new Translation3d(m_x * scalar, m_y * scalar, m_z * scalar);
    }

    /**
     * Returns the translation divided by a scalar.
     *
     * <p>For example, Translation3d(2.0, 2.5, 4.5) / 2 = Translation3d(1.0, 1.25, 2.25).
     *
     * @param scalar The scalar to multiply by.
     * @return The reference to the new mutated object.
     */
    public Translation3d div(double scalar) {
        return new Translation3d(m_x / scalar, m_y / scalar, m_z / scalar);
    }

    @Override
    public String toString() {
        return String.format("Translation3d(X: %.2f, Y: %.2f, Z: %.2f)", m_x, m_y, m_z);
    }

    /**
     * Checks equality between this Translation3d and another object.
     *
     * @param obj The other object.
     * @return Whether the two objects are equal or not.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Translation3d) {
            return Math.abs(((Translation3d) obj).m_x - m_x) < 1E-9
                    && Math.abs(((Translation3d) obj).m_y - m_y) < 1E-9
                    && Math.abs(((Translation3d) obj).m_z - m_z) < 1E-9;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_x, m_y, m_z);
    }

    @Override
    public Translation3d interpolate(Translation3d endValue, double t) {
        return new Translation3d(
                MathUtil.interpolate(this.getX(), endValue.getX(), t),
                MathUtil.interpolate(this.getY(), endValue.getY(), t),
                MathUtil.interpolate(this.getZ(), endValue.getZ(), t));
    }

    /**
     * Returns a Translation2d representing this Translation3d projected into the X-Y plane.
     *
     * @return A Translation2d representing this Translation3d projected into the X-Y plane.
     */
    public Translation2d toTranslation2d() {
        return new Translation2d(m_x, m_y);
    }
}
