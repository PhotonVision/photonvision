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

#include "photonlib/PhotonTrackedTarget.h"

#include <iostream>
#include <utility>

#include <frc/geometry/Translation2d.h>
#include <wpi/SmallVector.h>

namespace photonlib {

PhotonTrackedTarget::PhotonTrackedTarget(
    double yaw, double pitch, double area, double skew, int id,
    const frc::Transform3d& pose,
    const wpi::SmallVector<std::pair<double, double>, 4> corners)
    : yaw(yaw),
      pitch(pitch),
      area(area),
      skew(skew),
      fiducialId(id),
      cameraToTarget(pose),
      corners(corners) {}

bool PhotonTrackedTarget::operator==(const PhotonTrackedTarget& other) const {
  return other.yaw == yaw && other.pitch == pitch && other.area == area &&
         other.skew == skew && other.cameraToTarget == cameraToTarget &&
         other.corners == corners;
}

bool PhotonTrackedTarget::operator!=(const PhotonTrackedTarget& other) const {
  return !operator==(other);
}

Packet& operator<<(Packet& packet, const PhotonTrackedTarget& target) {
  packet << target.yaw << target.pitch << target.area << target.skew
         << target.fiducialId << target.cameraToTarget.Translation().X().value()
         << target.cameraToTarget.Translation().Y().value()
         << target.cameraToTarget.Translation().Z().value()
         << target.cameraToTarget.Rotation().GetQuaternion().W()
         << target.cameraToTarget.Rotation().GetQuaternion().X()
         << target.cameraToTarget.Rotation().GetQuaternion().Y()
         << target.cameraToTarget.Rotation().GetQuaternion().Z()
         << target.poseAmbiguity;

  for (int i = 0; i < 4; i++) {
    packet << target.corners[i].first << target.corners[i].second;
  }

  return packet;
}

Packet& operator>>(Packet& packet, PhotonTrackedTarget& target) {
  packet >> target.yaw >> target.pitch >> target.area >> target.skew >>
      target.fiducialId;
  double x = 0;
  double y = 0;
  double z = 0;
  double w = 0;
  packet >> x >> y >> z;
  const auto translation = frc::Translation3d(
      units::meter_t(x), units::meter_t(y), units::meter_t(z));
  packet >> w >> x >> y >> z;
  const auto rotation = frc::Rotation3d(frc::Quaternion(w, x, y, z));

  target.cameraToTarget = frc::Transform3d(translation, rotation);

  packet >> target.poseAmbiguity;

  target.corners.clear();
  for (int i = 0; i < 4; i++) {
    double first = 0;
    double second = 0;
    packet >> first >> second;
    target.corners.emplace_back(first, second);
  }

  return packet;
}

}  // namespace photonlib
