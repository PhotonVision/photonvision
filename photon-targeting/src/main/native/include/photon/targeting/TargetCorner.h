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

#include "photon/dataflow/structures/Packet.h"

namespace photon {

class TargetCorner {
 public:
  TargetCorner(double x, double y) : x(x), y(y) {}

  double x;
  double y;

  bool operator==(const TargetCorner& other) const;

  friend Packet& operator<<(Packet& packet, const TargetCorner& target);
  friend Packet& operator>>(Packet& packet, TargetCorner& target);
};
}  // namespace photon
