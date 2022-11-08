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

#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Transform3d.h>

using namespace frc;

Transform3d::Transform3d(Pose3d initial, Pose3d final) {
  // We are rotating the difference between the translations
  // using a clockwise rotation matrix. This transforms the global
  // delta into a local delta (relative to the initial pose).
  m_translation = (final.Translation() - initial.Translation())
                      .RotateBy(-initial.Rotation());

  m_rotation = final.Rotation() - initial.Rotation();
}

Transform3d::Transform3d(Translation3d translation, Rotation3d rotation)
    : m_translation(std::move(translation)), m_rotation(std::move(rotation)) {}

Transform3d Transform3d::Inverse() const {
  // We are rotating the difference between the translations
  // using a clockwise rotation matrix. This transforms the global
  // delta into a local delta (relative to the initial pose).
  return Transform3d{(-Translation()).RotateBy(-Rotation()), -Rotation()};
}

Transform3d Transform3d::operator+(const Transform3d& other) const {
  return Transform3d{Pose3d{}, Pose3d{}.TransformBy(*this).TransformBy(other)};
}

bool Transform3d::operator==(const Transform3d& other) const {
  return m_translation == other.m_translation && m_rotation == other.m_rotation;
}

bool Transform3d::operator!=(const Transform3d& other) const {
  return !operator==(other);
}
