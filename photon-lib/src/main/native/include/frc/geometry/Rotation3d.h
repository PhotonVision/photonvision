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

#include <frc/EigenCore.h>
#include <frc/geometry/Rotation2d.h>
#include <units/angle.h>
#include <wpi/SymbolExports.h>

#include "Quaternion.h"

namespace frc {

/**
 * A rotation in a 3D coordinate frame represented by a quaternion.
 */
class WPILIB_DLLEXPORT Rotation3d {
 public:
  /**
   * Constructs a Rotation3d with a default angle of 0 degrees.
   */
  Rotation3d() = default;

  /**
   * Constructs a Rotation3d from a quaternion.
   *
   * @param q The quaternion.
   */
  explicit Rotation3d(const Quaternion& q);

  /**
   * Constructs a Rotation3d from extrinsic roll, pitch, and yaw.
   *
   * Extrinsic rotations occur in that order around the axes in the fixed global
   * frame rather than the body frame.
   *
   * @param roll The counterclockwise rotation angle around the X axis (roll).
   * @param pitch The counterclockwise rotation angle around the Y axis (pitch).
   * @param yaw The counterclockwise rotation angle around the Z axis (yaw).
   */
  Rotation3d(units::radian_t roll, units::radian_t pitch, units::radian_t yaw);

  /**
   * Constructs a Rotation3d with the given axis-angle representation. The axis
   * doesn't have to be normalized.
   *
   * @param axis The rotation axis.
   * @param angle The rotation around the axis.
   */
  Rotation3d(const Vectord<3>& axis, units::radian_t angle);

  /**
   * Constructs a Rotation3d from a rotation matrix.
   *
   * @param rotationMatrix The rotation matrix.
   * @throws std::domain_error if the rotation matrix isn't special orthogonal.
   */
  explicit Rotation3d(const Matrixd<3, 3>& rotationMatrix);

  /**
   * Constructs a Rotation3d that rotates the initial vector onto the final
   * vector.
   *
   * This is useful for turning a 3D vector (final) into an orientation relative
   * to a coordinate system vector (initial).
   *
   * @param initial The initial vector.
   * @param final The final vector.
   */
  Rotation3d(const Vectord<3>& initial, const Vectord<3>& final);

  /**
   * Adds two rotations together.
   *
   * @param other The rotation to add.
   *
   * @return The sum of the two rotations.
   */
  Rotation3d operator+(const Rotation3d& other) const;

  /**
   * Subtracts the new rotation from the current rotation and returns the new
   * rotation.
   *
   * @param other The rotation to subtract.
   *
   * @return The difference between the two rotations.
   */
  Rotation3d operator-(const Rotation3d& other) const;

  /**
   * Takes the inverse of the current rotation.
   *
   * @return The inverse of the current rotation.
   */
  Rotation3d operator-() const;

  /**
   * Multiplies the current rotation by a scalar.
   * @param scalar The scalar.
   *
   * @return The new scaled Rotation3d.
   */
  Rotation3d operator*(double scalar) const;

  /**
   * Checks equality between this Rotation3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are equal.
   */
  bool operator==(const Rotation3d& other) const;

  /**
   * Checks inequality between this Rotation3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are not equal.
   */
  bool operator!=(const Rotation3d& other) const;

  /**
   * Adds the new rotation to the current rotation.
   *
   * @param other The rotation to rotate by.
   *
   * @return The new rotated Rotation3d.
   */
  Rotation3d RotateBy(const Rotation3d& other) const;

  /**
   * Returns the quaternion representation of the Rotation3d.
   */
  const Quaternion& GetQuaternion() const;

  /**
   * Returns the counterclockwise rotation angle around the X axis (roll).
   */
  units::radian_t X() const;

  /**
   * Returns the counterclockwise rotation angle around the Y axis (pitch).
   */
  units::radian_t Y() const;

  /**
   * Returns the counterclockwise rotation angle around the Z axis (yaw).
   */
  units::radian_t Z() const;

  /**
   * Returns the axis in the axis-angle representation of this rotation.
   */
  Vectord<3> Axis() const;

  /**
   * Returns the angle in the axis-angle representation of this rotation.
   */
  units::radian_t Angle() const;

  /**
   * Returns a Rotation2d representing this Rotation3d projected into the X-Y
   * plane.
   */
  Rotation2d ToRotation2d() const;

 private:
  Quaternion m_q;
};

}  // namespace frc
