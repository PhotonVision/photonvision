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

#include <cmath>

#include <frc/geometry/Pose3d.h>

using namespace frc;

namespace {

/**
 * Applies the hat operator to a rotation vector.
 *
 * It takes a rotation vector and returns the corresponding matrix
 * representation of the Lie algebra element (a 3x3 rotation matrix).
 *
 * @param rotation The rotation vector.
 * @return The rotation vector as a 3x3 rotation matrix.
 */
Matrixd<3, 3> RotationVectorToMatrix(const Vectord<3>& rotation) {
  // Given a rotation vector <a, b, c>,
  //         [ 0 -c  b]
  // Omega = [ c  0 -a]
  //         [-b  a  0]
  return Matrixd<3, 3>{{0.0, -rotation(2), rotation(1)},
                       {rotation(2), 0.0, -rotation(0)},
                       {-rotation(1), rotation(0), 0.0}};
}
}  // namespace

Pose3d::Pose3d(Translation3d translation, Rotation3d rotation)
    : m_translation(std::move(translation)), m_rotation(std::move(rotation)) {}

Pose3d::Pose3d(units::meter_t x, units::meter_t y, units::meter_t z,
               Rotation3d rotation)
    : m_translation(x, y, z), m_rotation(std::move(rotation)) {}

Pose3d Pose3d::operator+(const Transform3d& other) const {
  return TransformBy(other);
}

Transform3d Pose3d::operator-(const Pose3d& other) const {
  const auto pose = this->RelativeTo(other);
  return Transform3d{pose.Translation(), pose.Rotation()};
}

bool Pose3d::operator==(const Pose3d& other) const {
  return m_translation == other.m_translation && m_rotation == other.m_rotation;
}

bool Pose3d::operator!=(const Pose3d& other) const {
  return !operator==(other);
}

Pose3d Pose3d::TransformBy(const Transform3d& other) const {
  return {m_translation + (other.Translation().RotateBy(m_rotation)),
          m_rotation + other.Rotation()};
}

Pose3d Pose3d::RelativeTo(const Pose3d& other) const {
  const Transform3d transform{other, *this};
  return {transform.Translation(), transform.Rotation()};
}

Pose3d Pose3d::Exp(const Twist3d& twist) const {
  Matrixd<3, 3> Omega = RotationVectorToMatrix(
      Vectord<3>{twist.rx.value(), twist.ry.value(), twist.rz.value()});
  Matrixd<3, 3> OmegaSq = Omega * Omega;

  double thetaSq =
      (twist.rx * twist.rx + twist.ry * twist.ry + twist.rz * twist.rz).value();

  // Get left Jacobian of SO3. See first line in right column of
  // http://asrl.utias.utoronto.ca/~tdb/bib/barfoot_ser17_identities.pdf
  Matrixd<3, 3> J;
  if (thetaSq < 1E-9 * 1E-9) {
    // V = I + 0.5ω
    J = Matrixd<3, 3>::Identity() + 0.5 * Omega;
  } else {
    double theta = std::sqrt(thetaSq);
    // J = I + (1 − std::cos(θ))/θ² ω + (θ − std::sin(θ))/θ³ ω²
    J = Matrixd<3, 3>::Identity() + (1.0 - std::cos(theta)) / thetaSq * Omega +
        (theta - std::sin(theta)) / (thetaSq * theta) * OmegaSq;
  }

  // Get translation component
  Vectord<3> translation =
      J * Vectord<3>{twist.dx.value(), twist.dy.value(), twist.dz.value()};

  const Transform3d transform{Translation3d{units::meter_t{translation(0)},
                                            units::meter_t{translation(1)},
                                            units::meter_t{translation(2)}},
                              Rotation3d{twist.rx, twist.ry, twist.rz}};

  return *this + transform;
}

Twist3d Pose3d::Log(const Pose3d& end) const {
  const auto transform = end.RelativeTo(*this);

  Vectord<3> rotVec = transform.Rotation().GetQuaternion().ToRotationVector();

  Matrixd<3, 3> Omega = RotationVectorToMatrix(rotVec);
  Matrixd<3, 3> OmegaSq = Omega * Omega;

  double thetaSq = rotVec.squaredNorm();

  // Get left Jacobian inverse of SO3. See fourth line in right column of
  // http://asrl.utias.utoronto.ca/~tdb/bib/barfoot_ser17_identities.pdf
  Matrixd<3, 3> Jinv;
  if (thetaSq < 1E-9 * 1E-9) {
    // J⁻¹ = I − 0.5ω + 1/12 ω²
    Jinv = Matrixd<3, 3>::Identity() - 0.5 * Omega + 1.0 / 12.0 * OmegaSq;
  } else {
    double theta = std::sqrt(thetaSq);
    double halfTheta = 0.5 * theta;

    // J⁻¹ = I − 0.5ω + (1 − 0.5θ std::cos(θ/2) / std::sin(θ/2))/θ² ω²
    Jinv = Matrixd<3, 3>::Identity() - 0.5 * Omega +
           (1.0 - 0.5 * theta * std::cos(halfTheta) / std::sin(halfTheta)) /
               thetaSq * OmegaSq;
  }

  // Get dtranslation component
  Vectord<3> dtranslation =
      Jinv * Vectord<3>{transform.X().value(), transform.Y().value(),
                        transform.Z().value()};

  return Twist3d{
      units::meter_t{dtranslation(0)}, units::meter_t{dtranslation(1)},
      units::meter_t{dtranslation(2)}, units::radian_t{rotVec(0)},
      units::radian_t{rotVec(1)},      units::radian_t{rotVec(2)}};
}

Pose2d Pose3d::ToPose2d() const {
  return Pose2d{m_translation.X(), m_translation.Y(), m_rotation.Z()};
}
