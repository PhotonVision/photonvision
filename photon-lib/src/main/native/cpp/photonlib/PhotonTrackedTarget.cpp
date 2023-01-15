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

static constexpr const uint8_t MAX_CORNERS = 8;

namespace photonlib {

PhotonTrackedTarget::PhotonTrackedTarget(
    double yaw, double pitch, double area, double skew, int id,
    const frc::Transform3d& pose, const frc::Transform3d& alternatePose,
    double ambiguity,
    const wpi::SmallVector<std::pair<double, double>, 4> minAreaRectCorners,
    const std::vector<std::pair<double, double>> detectedCorners)
    : yaw(yaw),
      pitch(pitch),
      area(area),
      skew(skew),
      fiducialId(id),
      bestCameraToTarget(pose),
      altCameraToTarget(alternatePose),
      poseAmbiguity(ambiguity),
      minAreaRectCorners(minAreaRectCorners),
      detectedCorners(detectedCorners) {}

bool PhotonTrackedTarget::operator==(const PhotonTrackedTarget& other) const {
  return other.yaw == yaw && other.pitch == pitch && other.area == area &&
         other.skew == skew && other.bestCameraToTarget == bestCameraToTarget &&
         other.minAreaRectCorners == minAreaRectCorners;
}

bool PhotonTrackedTarget::operator!=(const PhotonTrackedTarget& other) const {
  return !operator==(other);
}

Packet& operator<<(Packet& packet, const PhotonTrackedTarget& target) {
  packet << target.yaw << target.pitch << target.area << target.skew
         << target.fiducialId
         << target.bestCameraToTarget.Translation().X().value()
         << target.bestCameraToTarget.Translation().Y().value()
         << target.bestCameraToTarget.Translation().Z().value()
         << target.bestCameraToTarget.Rotation().GetQuaternion().W()
         << target.bestCameraToTarget.Rotation().GetQuaternion().X()
         << target.bestCameraToTarget.Rotation().GetQuaternion().Y()
         << target.bestCameraToTarget.Rotation().GetQuaternion().Z()
         << target.altCameraToTarget.Translation().X().value()
         << target.altCameraToTarget.Translation().Y().value()
         << target.altCameraToTarget.Translation().Z().value()
         << target.altCameraToTarget.Rotation().GetQuaternion().W()
         << target.altCameraToTarget.Rotation().GetQuaternion().X()
         << target.altCameraToTarget.Rotation().GetQuaternion().Y()
         << target.altCameraToTarget.Rotation().GetQuaternion().Z()
         << target.poseAmbiguity;

  for (int i = 0; i < 4; i++) {
    packet << target.minAreaRectCorners[i].first
           << target.minAreaRectCorners[i].second;
  }

  uint8_t num_corners =
      std::min<uint8_t>(target.detectedCorners.size(), MAX_CORNERS);
  packet << num_corners;
  for (size_t i = 0; i < target.detectedCorners.size(); i++) {
    packet << target.detectedCorners[i].first
           << target.detectedCorners[i].second;
  }

  return packet;
}

Packet& operator>>(Packet& packet, PhotonTrackedTarget& target) {
  packet >> target.yaw >> target.pitch >> target.area >> target.skew >>
      target.fiducialId;

  // We use these for best and alt transforms below
  double x = 0;
  double y = 0;
  double z = 0;
  double w = 0;

  // First transform is the "best" pose
  packet >> x >> y >> z;
  const auto bestTranslation = frc::Translation3d(
      units::meter_t(x), units::meter_t(y), units::meter_t(z));
  packet >> w >> x >> y >> z;
  const auto bestRotation = frc::Rotation3d(frc::Quaternion(w, x, y, z));
  target.bestCameraToTarget = frc::Transform3d(bestTranslation, bestRotation);

  // Second transform is the "alternate" pose
  packet >> x >> y >> z;
  const auto altTranslation = frc::Translation3d(
      units::meter_t(x), units::meter_t(y), units::meter_t(z));
  packet >> w >> x >> y >> z;
  const auto altRotation = frc::Rotation3d(frc::Quaternion(w, x, y, z));
  target.altCameraToTarget = frc::Transform3d(altTranslation, altRotation);

  packet >> target.poseAmbiguity;

  target.minAreaRectCorners.clear();
  double first = 0;
  double second = 0;
  for (int i = 0; i < 4; i++) {
    packet >> first >> second;
    target.minAreaRectCorners.emplace_back(first, second);
  }

  uint8_t numCorners = 0;
  packet >> numCorners;
  target.detectedCorners.clear();
  target.detectedCorners.reserve(numCorners);
  for (size_t i = 0; i < numCorners; i++) {
    packet >> first >> second;
    target.detectedCorners.emplace_back(first, second);
  }

  return packet;
}

}  // namespace photonlib
