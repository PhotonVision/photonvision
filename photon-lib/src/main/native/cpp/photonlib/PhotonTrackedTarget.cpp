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
    double yaw, double pitch, double area, double skew,
    const frc::Transform2d& pose,
    const wpi::SmallVector<std::pair<double, double>, 4> corners)
    : yaw(yaw),
      pitch(pitch),
      area(area),
      skew(skew),
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
         << target.cameraToTarget.Translation().X().value()
         << target.cameraToTarget.Translation().Y().value()
         << target.cameraToTarget.Rotation().Degrees().value();

  for (int i = 0; i < 4; i++) {
    packet << target.corners[i].first << target.corners[i].second;
  }

  return packet;
}

Packet& operator>>(Packet& packet, PhotonTrackedTarget& target) {
  packet >> target.yaw >> target.pitch >> target.area >> target.skew;
  double x = 0;
  double y = 0;
  double rot = 0;
  packet >> x >> y >> rot;

  target.cameraToTarget =
      frc::Transform2d(frc::Translation2d(units::meter_t(x), units::meter_t(y)),
                       units::degree_t(rot));

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
