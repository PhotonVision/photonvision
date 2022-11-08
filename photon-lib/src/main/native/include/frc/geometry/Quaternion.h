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
#include <wpi/SymbolExports.h>

namespace frc {

class WPILIB_DLLEXPORT Quaternion {
 public:
  /**
   * Constructs a quaternion with a default angle of 0 degrees.
   */
  Quaternion() = default;

  /**
   * Constructs a quaternion with the given components.
   *
   * @param w W component of the quaternion.
   * @param x X component of the quaternion.
   * @param y Y component of the quaternion.
   * @param z Z component of the quaternion.
   */
  Quaternion(double w, double x, double y, double z);

  /**
   * Multiply with another quaternion.
   *
   * @param other The other quaternion.
   */
  Quaternion operator*(const Quaternion& other) const;

  /**
   * Checks equality between this Quaternion and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are equal.
   */
  bool operator==(const Quaternion& other) const;

  /**
   * Checks inequality between this Quaternion and another object.
   *
   * @param other The other object.
   * @return Whether the two objects are not equal.
   */
  bool operator!=(const Quaternion& other) const;

  /**
   * Returns the inverse of the quaternion.
   */
  Quaternion Inverse() const;

  /**
   * Normalizes the quaternion.
   */
  Quaternion Normalize() const;

  /**
   * Returns W component of the quaternion.
   */
  double W() const;

  /**
   * Returns X component of the quaternion.
   */
  double X() const;

  /**
   * Returns Y component of the quaternion.
   */
  double Y() const;

  /**
   * Returns Z component of the quaternion.
   */
  double Z() const;

  /**
   * Returns the rotation vector representation of this quaternion.
   *
   * This is also the log operator of SO(3).
   */
  Eigen::Vector3d ToRotationVector() const;

 private:
  double m_r = 1.0;
  Eigen::Vector3d m_v{0.0, 0.0, 0.0};
};

}  // namespace frc
