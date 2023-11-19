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

#pragma once

#include <frc/geometry/Transform3d.h>
#include <wpi/SmallVector.h>

#include "PNPResult.h"
#include "photon/dataflow/structures/Packet.h"

namespace photon {
class MultiTargetPNPResult {
 public:
  PNPResult result;
  wpi::SmallVector<int16_t, 32> fiducialIdsUsed;

  bool operator==(const MultiTargetPNPResult& other) const;

  friend Packet& operator<<(Packet& packet, const MultiTargetPNPResult& result);
  friend Packet& operator>>(Packet& packet, MultiTargetPNPResult& result);
};
}  // namespace photon
