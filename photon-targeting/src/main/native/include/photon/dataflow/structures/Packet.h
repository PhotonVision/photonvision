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

#include <algorithm>
#include <bit>
#include <cstring>
#include <string>
#include <vector>

namespace photon {

/**
 * A packet that holds byte-packed data to be sent over NetworkTables.
 */
class Packet {
 public:
  /**
   * Constructs an empty packet.
   */
  Packet() = default;

  /**
   * Constructs a packet with the given data.
   * @param data The packet data.
   */
  explicit Packet(std::vector<uint8_t> data);

  /**
   * Clears the packet and resets the read and write positions.
   */
  void Clear();

  /**
   * Returns the packet data.
   * @return The packet data.
   */
  inline const std::vector<uint8_t>& GetData() { return packetData; }

  /**
   * Returns the number of bytes in the data.
   * @return The number of bytes in the data.
   */
  inline size_t GetDataSize() const { return packetData.size(); }

  /**
   * Adds a value to the data buffer. This should only be used with PODs.
   * @tparam T The data type.
   * @param src The data source.
   * @return A reference to the current object.
   */
  template <typename T>
  Packet& operator<<(T src) {
    packetData.resize(packetData.size() + sizeof(T));
    std::memcpy(packetData.data() + writePos, &src, sizeof(T));

    if constexpr (std::endian::native == std::endian::little) {
      // Reverse to big endian for network conventions.
      std::reverse(packetData.data() + writePos,
                   packetData.data() + writePos + sizeof(T));
    }

    writePos += sizeof(T);
    return *this;
  }

  /**
   * Extracts a value to the provided destination.
   * @tparam T The type of value to extract.
   * @param value The value to extract.
   * @return A reference to the current object.
   */
  template <typename T>
  Packet& operator>>(T& value) {
    if (!packetData.empty()) {
      std::memcpy(&value, packetData.data() + readPos, sizeof(T));

      if constexpr (std::endian::native == std::endian::little) {
        // Reverse to little endian for host.
        uint8_t& raw = reinterpret_cast<uint8_t&>(value);
        std::reverse(&raw, &raw + sizeof(T));
      }
    }

    readPos += sizeof(T);
    return *this;
  }

  bool operator==(const Packet& right) const;
  bool operator!=(const Packet& right) const;

 private:
  // Data stored in the packet
  std::vector<uint8_t> packetData;

  size_t readPos = 0;
  size_t writePos = 0;
};
}  // namespace photon
