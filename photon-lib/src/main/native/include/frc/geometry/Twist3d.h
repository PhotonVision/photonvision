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

#include <frc/geometry/Rotation3d.h>
#include <units/angle.h>
#include <units/length.h>
#include <units/math.h>
#include <wpi/SymbolExports.h>

namespace frc {
/**
 * A change in distance along a 3D arc since the last pose update. We can use
 * ideas from differential calculus to create new Pose3ds from a Twist3d and
 * vise versa.
 *
 * A Twist can be used to represent a difference between two poses.
 */
struct WPILIB_DLLEXPORT Twist3d {
  /**
   * Linear "dx" component
   */
  units::meter_t dx = 0_m;

  /**
   * Linear "dy" component
   */
  units::meter_t dy = 0_m;

  /**
   * Linear "dz" component
   */
  units::meter_t dz = 0_m;

  /**
   * Rotation vector x component.
   */
  units::radian_t rx = 0_rad;

  /**
   * Rotation vector y component.
   */
  units::radian_t ry = 0_rad;

  /**
   * Rotation vector z component.
   */
  units::radian_t rz = 0_rad;

  /**
   * Checks equality between this Twist3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are equal.
   */
  bool operator==(const Twist3d& other) const {
    return units::math::abs(dx - other.dx) < 1E-9_m &&
           units::math::abs(dy - other.dy) < 1E-9_m &&
           units::math::abs(dz - other.dz) < 1E-9_m &&
           units::math::abs(rx - other.rx) < 1E-9_rad &&
           units::math::abs(ry - other.ry) < 1E-9_rad &&
           units::math::abs(rz - other.rz) < 1E-9_rad;
  }

  /**
   * Checks inequality between this Twist3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are not equal.
   */
  bool operator!=(const Twist3d& other) const { return !operator==(other); }

  /**
   * Scale this by a given factor.
   *
   * @param factor The factor by which to scale.
   * @return The scaled Twist3d.
   */
  Twist3d operator*(double factor) const {
    return Twist3d{dx * factor, dy * factor, dz * factor,
                   rx * factor, ry * factor, rz * factor};
  }
};
}  // namespace frc
