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

#include "photonlib/PhotonTrackedTarget.h"

#include <iostream>

#include <frc/geometry/Translation2d.h>

namespace photonlib {

PhotonTrackedTarget::PhotonTrackedTarget(double yaw, double pitch, double area,
                                         double skew,
                                         const frc::Transform2d& pose)
    : yaw(yaw), pitch(pitch), area(area), skew(skew), cameraToTarget(pose) {}

bool PhotonTrackedTarget::operator==(const PhotonTrackedTarget& other) const {
  return other.yaw == yaw && other.pitch == pitch && other.area == area &&
         other.skew == skew && other.cameraToTarget == cameraToTarget;
}

bool PhotonTrackedTarget::operator!=(const PhotonTrackedTarget& other) const {
  return !operator==(other);
}

Packet& operator<<(Packet& packet, const PhotonTrackedTarget& target) {
  return packet << target.yaw << target.pitch << target.area << target.skew
                << target.cameraToTarget.Translation().X().to<double>()
                << target.cameraToTarget.Translation().Y().to<double>()
                << target.cameraToTarget.Rotation().Degrees().to<double>();
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
  return packet;
}

}  // namespace photonlib
