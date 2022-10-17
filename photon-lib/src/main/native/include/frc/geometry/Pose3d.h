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

#include <frc/geometry/Pose2d.h>
#include <wpi/SymbolExports.h>

#include "Transform3d.h"
#include "Translation3d.h"
#include "Twist3d.h"

namespace frc {

/**
 * Represents a 3D pose containing translational and rotational elements.
 */
class WPILIB_DLLEXPORT Pose3d {
 public:
  /**
   * Constructs a pose at the origin facing toward the positive X axis.
   */
  constexpr Pose3d() = default;

  /**
   * Constructs a pose with the specified translation and rotation.
   *
   * @param translation The translational component of the pose.
   * @param rotation The rotational component of the pose.
   */
  Pose3d(Translation3d translation, Rotation3d rotation);

  /**
   * Constructs a pose with x, y, and z translations instead of a separate
   * Translation3d.
   *
   * @param x The x component of the translational component of the pose.
   * @param y The y component of the translational component of the pose.
   * @param z The z component of the translational component of the pose.
   * @param rotation The rotational component of the pose.
   */
  Pose3d(units::meter_t x, units::meter_t y, units::meter_t z,
         Rotation3d rotation);

  /**
   * Transforms the pose by the given transformation and returns the new
   * transformed pose.
   *
   * @param other The transform to transform the pose by.
   *
   * @return The transformed pose.
   */
  Pose3d operator+(const Transform3d& other) const;

  /**
   * Returns the Transform3d that maps the one pose to another.
   *
   * @param other The initial pose of the transformation.
   * @return The transform that maps the other pose to the current pose.
   */
  Transform3d operator-(const Pose3d& other) const;

  /**
   * Checks equality between this Pose3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are equal.
   */
  bool operator==(const Pose3d& other) const;

  /**
   * Checks inequality between this Pose3d and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are not equal.
   */
  bool operator!=(const Pose3d& other) const;

  /**
   * Returns the underlying translation.
   *
   * @return Reference to the translational component of the pose.
   */
  const Translation3d& Translation() const { return m_translation; }

  /**
   * Returns the X component of the pose's translation.
   *
   * @return The x component of the pose's translation.
   */
  units::meter_t X() const { return m_translation.X(); }

  /**
   * Returns the Y component of the pose's translation.
   *
   * @return The y component of the pose's translation.
   */
  units::meter_t Y() const { return m_translation.Y(); }

  /**
   * Returns the Z component of the pose's translation.
   *
   * @return The z component of the pose's translation.
   */
  units::meter_t Z() const { return m_translation.Z(); }

  /**
   * Returns the underlying rotation.
   *
   * @return Reference to the rotational component of the pose.
   */
  const Rotation3d& Rotation() const { return m_rotation; }

  /**
   * Transforms the pose by the given transformation and returns the new pose.
   * See + operator for the matrix multiplication performed.
   *
   * @param other The transform to transform the pose by.
   *
   * @return The transformed pose.
   */
  Pose3d TransformBy(const Transform3d& other) const;

  /**
   * Returns the other pose relative to the current pose.
   *
   * This function can often be used for trajectory tracking or pose
   * stabilization algorithms to get the error between the reference and the
   * current pose.
   *
   * @param other The pose that is the origin of the new coordinate frame that
   * the current pose will be converted into.
   *
   * @return The current pose relative to the new origin pose.
   */
  Pose3d RelativeTo(const Pose3d& other) const;

  /**
   * Obtain a new Pose3d from a (constant curvature) velocity.
   *
   * The twist is a change in pose in the robot's coordinate frame since the
   * previous pose update. When the user runs exp() on the previous known
   * field-relative pose with the argument being the twist, the user will
   * receive the new field-relative pose.
   *
   * "Exp" represents the pose exponential, which is solving a differential
   * equation moving the pose forward in time.
   *
   * @param twist The change in pose in the robot's coordinate frame since the
   * previous pose update. For example, if a non-holonomic robot moves forward
   * 0.01 meters and changes angle by 0.5 degrees since the previous pose
   * update, the twist would be Twist3d{0.01_m, 0_m, 0_m, Rotation3d{0.0, 0.0,
   * 0.5_deg}}.
   *
   * @return The new pose of the robot.
   */
  Pose3d Exp(const Twist3d& twist) const;

  /**
   * Returns a Twist3d that maps this pose to the end pose. If c is the output
   * of a.Log(b), then a.Exp(c) would yield b.
   *
   * @param end The end pose for the transformation.
   *
   * @return The twist that maps this to end.
   */
  Twist3d Log(const Pose3d& end) const;

  /**
   * Returns a Pose2d representing this Pose3d projected into the X-Y plane.
   */
  Pose2d ToPose2d() const;

 private:
  Translation3d m_translation;
  Rotation3d m_rotation;
};

}  // namespace frc
