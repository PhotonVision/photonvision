/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#pragma once

#include <frc/geometry/Translation2d.h>
#include <units/length.h>
#include <wpi/SymbolExports.h>

#include "Rotation3d.h"

namespace frc {

/**
 * Represents a translation in 3D space.
 * This object can be used to represent a point or a vector.
 *
 * This assumes that you are using conventional mathematical axes. When the
 * robot is at the origin facing in the positive X direction, forward is
 * positive X, left is positive Y, and up is positive Z.
 */
class WPILIB_DLLEXPORT Translation3d {
 public:
  /**
   * Constructs a Translation3d with X, Y, and Z components equal to zero.
   */
  constexpr Translation3d() = default;

  /**
   * Constructs a Translation3d with the X, Y, and Z components equal to the
   * provided values.
   *
   * @param x The x component of the translation.
   * @param y The y component of the translation.
   * @param z The z component of the translation.
   */
  Translation3d(units::meter_t x, units::meter_t y, units::meter_t z);

  /**
   * Constructs a Translation3d with the provided distance and angle. This is
   * essentially converting from polar coordinates to Cartesian coordinates.
   *
   * @param distance The distance from the origin to the end of the translation.
   * @param angle The angle between the x-axis and the translation vector.
   */
  Translation3d(units::meter_t distance, const Rotation3d& angle);

  /**
   * Calculates the distance between two translations in 3D space.
   *
   * The distance between translations is defined as
   * √((x₂−x₁)²+(y₂−y₁)²+(z₂−z₁)²).
   *
   * @param other The translation to compute the distance to.
   *
   * @return The distance between the two translations.
   */
  units::meter_t Distance(const Translation3d& other) const;

  /**
   * Returns the X component of the translation.
   *
   * @return The Z component of the translation.
   */
  units::meter_t X() const { return m_x; }

  /**
   * Returns the Y component of the translation.
   *
   * @return The Y component of the translation.
   */
  units::meter_t Y() const { return m_y; }

  /**
   * Returns the Z component of the translation.
   *
   * @return The Z component of the translation.
   */
  units::meter_t Z() const { return m_z; }

  /**
   * Returns the norm, or distance from the origin to the translation.
   *
   * @return The norm of the translation.
   */
  units::meter_t Norm() const;

  /**
   * Applies a rotation to the translation in 3D space.
   *
   * For example, rotating a Translation3d of &lt;2, 0, 0&gt; by 90 degrees
   * around the Z axis will return a Translation3d of &lt;0, 2, 0&gt;.
   *
   * @param other The rotation to rotate the translation by.
   *
   * @return The new rotated translation.
   */
  Translation3d RotateBy(const Rotation3d& other) const;

  /**
   * Returns a Translation2d representing this Translation3d projected into the
   * X-Y plane.
   */
  Translation2d ToTranslation2d() const;

  /**
   * Returns the sum of two translations in 3D space.
   *
   * For example, Translation3d{1.0, 2.5, 3.5} + Translation3d{2.0, 5.5, 7.5} =
   * Translation3d{3.0, 8.0, 11.0}.
   *
   * @param other The translation to add.
   *
   * @return The sum of the translations.
   */
  Translation3d operator+(const Translation3d& other) const;

  /**
   * Returns the difference between two translations.
   *
   * For example, Translation3d{5.0, 4.0, 3.0} - Translation3d{1.0, 2.0, 3.0} =
   * Translation3d{4.0, 2.0, 0.0}.
   *
   * @param other The translation to subtract.
   *
   * @return The difference between the two translations.
   */
  Translation3d operator-(const Translation3d& other) const;

  /**
   * Returns the inverse of the current translation. This is equivalent to
   * negating all components of the translation.
   *
   * @return The inverse of the current translation.
   */
  Translation3d operator-() const;

  /**
   * Returns the translation multiplied by a scalar.
   *
   * For example, Translation3d{2.0, 2.5, 4.5} * 2 = Translation3d{4.0, 5.0,
   * 9.0}.
   *
   * @param scalar The scalar to multiply by.
   *
   * @return The scaled translation.
   */
  Translation3d operator*(double scalar) const;

  /**
   * Returns the translation divided by a scalar.
   *
   * For example, Translation3d{2.0, 2.5, 4.5} / 2 = Translation3d{1.0, 1.25,
   * 2.25}.
   *
   * @param scalar The scalar to divide by.
   *
   * @return The scaled translation.
   */
  Translation3d operator/(double scalar) const;

  /**
   * Checks equality between this Translation3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are equal.
   */
  bool operator==(const Translation3d& other) const;

  /**
   * Checks inequality between this Translation3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are not equal.
   */
  bool operator!=(const Translation3d& other) const;

 private:
  units::meter_t m_x = 0_m;
  units::meter_t m_y = 0_m;
  units::meter_t m_z = 0_m;
};

}  // namespace frc
