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

#include "photon/dataflow/structures/Packet.h"

using namespace photon;

Packet::Packet(std::vector<uint8_t> data) : packetData(data) {}

void Packet::Clear() {
  packetData.clear();
  readPos = 0;
  writePos = 0;
}

bool Packet::operator==(const Packet& right) const {
  return packetData == right.packetData;
}
bool Packet::operator!=(const Packet& right) const {
  return !operator==(right);
}
