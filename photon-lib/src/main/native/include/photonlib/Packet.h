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

#pragma once

#include <algorithm>
#include <string>
#include <vector>

#include <wpi/Endian.h>

namespace photonlib {

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
  explicit Packet(std::vector<uint8_t> data) : packetData(data) {}

  /**
   * Clears the packet and resets the read and write positions.
   */
  void Clear() {
    packetData.clear();
    readPos = 0;
    writePos = 0;
  }

  /**
   * Returns the packet data.
   * @return The packet data.
   */
  const std::vector<uint8_t>& GetData() { return packetData; }

  /**
   * Returns the number of bytes in the data.
   * @return The number of bytes in the data.
   */
  size_t GetDataSize() const { return packetData.size(); }

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

    if constexpr (wpi::support::endian::system_endianness() ==
                  wpi::support::endianness::little) {
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

      if constexpr (wpi::support::endian::system_endianness() ==
                    wpi::support::endianness::little) {
        // Reverse to little endian for host.
        uint8_t& raw = reinterpret_cast<uint8_t&>(value);
        std::reverse(&raw, &raw + sizeof(T));
      }
    }

    readPos += sizeof(T);
    return *this;
  }

  bool operator==(const Packet& right) const {
    return packetData == right.packetData;
  }
  bool operator!=(const Packet& right) const { return !operator==(right); }

 private:
  // Data stored in the packet
  std::vector<uint8_t> packetData;

  size_t readPos = 0;
  size_t writePos = 0;
};

}  // namespace photonlib
