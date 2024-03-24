/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "photon/targeting/PhotonTrackedTarget.h"

#include <iostream>
#include <utility>

#include <frc/geometry/Translation2d.h>
#include <wpi/SmallVector.h>

static constexpr const uint8_t MAX_CORNERS = 8;

namespace photon {

PhotonTrackedTarget::PhotonTrackedTarget(
    double yaw, double pitch, double area, double skew, int id, int objdetid,
    float objdetconf, const frc::Transform3d& pose,
    const frc::Transform3d& alternatePose, double ambiguity,
    const wpi::SmallVector<std::pair<double, double>, 4> minAreaRectCorners,
    const std::vector<std::pair<double, double>> detectedCorners)
    : yaw(yaw),
      pitch(pitch),
      area(area),
      skew(skew),
      fiducialId(id),
      objDetectId(objdetid),
      objDetectConf(objdetconf),
      bestCameraToTarget(pose),
      altCameraToTarget(alternatePose),
      poseAmbiguity(ambiguity),
      minAreaRectCorners(minAreaRectCorners),
      detectedCorners(detectedCorners) {}

bool PhotonTrackedTarget::operator==(const PhotonTrackedTarget& other) const {
  return other.yaw == yaw && other.pitch == pitch && other.area == area &&
         other.skew == skew && other.bestCameraToTarget == bestCameraToTarget &&
         other.objDetectConf == objDetectConf &&
         other.objDetectId == objDetectId &&
         other.minAreaRectCorners == minAreaRectCorners;
}

Packet& operator<<(Packet& packet, const PhotonTrackedTarget& target) {
  packet << target.yaw << target.pitch << target.area << target.skew
         << target.fiducialId << target.objDetectId << target.objDetectConf
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
      target.fiducialId >> target.objDetectId >> target.objDetectConf;

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
}  // namespace photon
