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

#include <frc/geometry/Quaternion.h>

using namespace frc;

Quaternion::Quaternion(double w, double x, double y, double z)
    : m_r{w}, m_v{x, y, z} {}

Quaternion Quaternion::operator*(const Quaternion& other) const {
  // https://en.wikipedia.org/wiki/Quaternion#Scalar_and_vector_parts
  const auto& r1 = m_r;
  const auto& v1 = m_v;
  const auto& r2 = other.m_r;
  const auto& v2 = other.m_v;

  // v₁ x v₂
  Eigen::Vector3d cross{v1(1) * v2(2) - v2(1) * v1(2),
                        v2(0) * v1(2) - v1(0) * v2(2),
                        v1(0) * v2(1) - v2(0) * v1(1)};

  // v = r₁v₂ + r₂v₁ + v₁ x v₂
  Eigen::Vector3d v = r1 * v2 + r2 * v1 + cross;

  return Quaternion{r1 * r2 - v1.dot(v2), v(0), v(1), v(2)};
}

bool Quaternion::operator==(const Quaternion& other) const {
  return std::abs(m_r * other.m_r + m_v.dot(other.m_v)) > 1.0 - 1E-9;
}

bool Quaternion::operator!=(const Quaternion& other) const {
  return !operator==(other);
}

Quaternion Quaternion::Inverse() const {
  return Quaternion{m_r, -m_v(0), -m_v(1), -m_v(2)};
}

Quaternion Quaternion::Normalize() const {
  double norm = std::sqrt(W() * W() + X() * X() + Y() * Y() + Z() * Z());
  if (norm == 0.0) {
    return Quaternion{};
  } else {
    return Quaternion{W() / norm, X() / norm, Y() / norm, Z() / norm};
  }
}

double Quaternion::W() const { return m_r; }

double Quaternion::X() const { return m_v(0); }

double Quaternion::Y() const { return m_v(1); }

double Quaternion::Z() const { return m_v(2); }

Eigen::Vector3d Quaternion::ToRotationVector() const {
  // See equation (31) in "Integrating Generic Sensor Fusion Algorithms with
  // Sound State Representation through Encapsulation of Manifolds"
  //
  // https://arxiv.org/pdf/1107.1119.pdf
  double norm = m_v.norm();

  if (norm < 1e-9) {
    return (2.0 / W() - 2.0 / 3.0 * norm * norm / (W() * W() * W())) * m_v;
  } else {
    if (W() < 0.0) {
      return 2.0 * std::atan2(-norm, -W()) / norm * m_v;
    } else {
      return 2.0 * std::atan2(norm, W()) / norm * m_v;
    }
  }
}
