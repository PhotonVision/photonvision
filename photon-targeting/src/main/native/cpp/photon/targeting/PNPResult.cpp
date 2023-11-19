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

#include "photon/targeting/PNPResult.h"

namespace photon {
bool PNPResult::operator==(const PNPResult& other) const {
  return other.isPresent == isPresent && other.best == best &&
         other.bestReprojErr == bestReprojErr && other.alt == alt &&
         other.altReprojErr == altReprojErr && other.ambiguity == ambiguity;
}

// Encode a transform3d
Packet& operator<<(Packet& packet, const frc::Transform3d& transform) {
  packet << transform.Translation().X().value()
         << transform.Translation().Y().value()
         << transform.Translation().Z().value()
         << transform.Rotation().GetQuaternion().W()
         << transform.Rotation().GetQuaternion().X()
         << transform.Rotation().GetQuaternion().Y()
         << transform.Rotation().GetQuaternion().Z();

  return packet;
}

// Decode a transform3d
Packet& operator>>(Packet& packet, frc::Transform3d& transform) {
  frc::Transform3d ret;

  // We use these for best and alt transforms below
  double x = 0;
  double y = 0;
  double z = 0;
  double w = 0;

  // decode and unitify translation
  packet >> x >> y >> z;
  const auto translation = frc::Translation3d(
      units::meter_t(x), units::meter_t(y), units::meter_t(z));

  // decode and add units to rotation
  packet >> w >> x >> y >> z;
  const auto rotation = frc::Rotation3d(frc::Quaternion(w, x, y, z));

  transform = frc::Transform3d(translation, rotation);

  return packet;
}

Packet& operator<<(Packet& packet, PNPResult const& result) {
  packet << result.isPresent << result.best << result.alt
         << result.bestReprojErr << result.altReprojErr << result.ambiguity;

  return packet;
}

Packet& operator>>(Packet& packet, PNPResult& result) {
  packet >> result.isPresent >> result.best >> result.alt >>
      result.bestReprojErr >> result.altReprojErr >> result.ambiguity;

  return packet;
}
}  // namespace photon
