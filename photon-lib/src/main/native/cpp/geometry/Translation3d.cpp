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

#include <frc/geometry/Translation3d.h>
#include <units/math.h>

using namespace frc;

Translation3d::Translation3d(units::meter_t x, units::meter_t y,
                             units::meter_t z)
    : m_x(x), m_y(y), m_z(z) {}

Translation3d::Translation3d(units::meter_t distance, const Rotation3d& angle) {
  auto rectangular = Translation3d{distance, 0_m, 0_m}.RotateBy(angle);
  m_x = rectangular.X();
  m_y = rectangular.Y();
  m_z = rectangular.Z();
}

units::meter_t Translation3d::Distance(const Translation3d& other) const {
  return units::math::sqrt(units::math::pow<2>(other.m_x - m_x) +
                           units::math::pow<2>(other.m_y - m_y) +
                           units::math::pow<2>(other.m_z - m_z));
}

units::meter_t Translation3d::Norm() const {
  return units::math::sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
}

Translation3d Translation3d::RotateBy(const Rotation3d& other) const {
  Quaternion p{0.0, m_x.value(), m_y.value(), m_z.value()};
  auto qprime = other.GetQuaternion() * p * other.GetQuaternion().Inverse();
  return Translation3d{units::meter_t{qprime.X()}, units::meter_t{qprime.Y()},
                       units::meter_t{qprime.Z()}};
}

Translation2d Translation3d::ToTranslation2d() const {
  return Translation2d{m_x, m_y};
}

Translation3d Translation3d::operator+(const Translation3d& other) const {
  return {X() + other.X(), Y() + other.Y(), Z() + other.Z()};
}

Translation3d Translation3d::operator-(const Translation3d& other) const {
  return *this + -other;
}

Translation3d Translation3d::operator-() const { return {-m_x, -m_y, -m_z}; }

Translation3d Translation3d::operator*(double scalar) const {
  return {scalar * m_x, scalar * m_y, scalar * m_z};
}

Translation3d Translation3d::operator/(double scalar) const {
  return *this * (1.0 / scalar);
}

bool Translation3d::operator==(const Translation3d& other) const {
  return units::math::abs(m_x - other.m_x) < 1E-9_m &&
         units::math::abs(m_y - other.m_y) < 1E-9_m &&
         units::math::abs(m_z - other.m_z) < 1E-9_m;
}

bool Translation3d::operator!=(const Translation3d& other) const {
  return !operator==(other);
}
