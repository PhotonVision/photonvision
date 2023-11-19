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

#include "photon/targeting/MultiTargetPNPResult.h"

namespace photon {

bool MultiTargetPNPResult::operator==(const MultiTargetPNPResult& other) const {
  return other.result == result && other.fiducialIdsUsed == fiducialIdsUsed;
}

Packet& operator<<(Packet& packet, const MultiTargetPNPResult& result) {
  packet << result.result;

  size_t i;
  for (i = 0; i < result.fiducialIdsUsed.capacity(); i++) {
    if (i < result.fiducialIdsUsed.size()) {
      packet << static_cast<int16_t>(result.fiducialIdsUsed[i]);
    } else {
      packet << static_cast<int16_t>(-1);
    }
  }

  return packet;
}

Packet& operator>>(Packet& packet, MultiTargetPNPResult& result) {
  packet >> result.result;

  result.fiducialIdsUsed.clear();
  for (size_t i = 0; i < result.fiducialIdsUsed.capacity(); i++) {
    int16_t id = 0;
    packet >> id;

    if (id > -1) {
      result.fiducialIdsUsed.push_back(id);
    }
  }

  return packet;
}
}  // namespace photon
