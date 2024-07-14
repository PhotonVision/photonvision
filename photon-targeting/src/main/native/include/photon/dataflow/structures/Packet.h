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
#include <iostream>
#include <optional>
#include <span>
#include <string>
#include <vector>

#include <wpi/Demangle.h>
#include <wpi/struct/Struct.h>

namespace photon {

class Packet;

// Struct is where all our actual ser/de methods are implemented
template <typename T>
struct SerdeType {};

template <typename T>
concept PhotonStructSerializable = requires(Packet& packet, const T& value) {
  typename SerdeType<typename std::remove_cvref_t<T>>;

  // MD6sum of the message definition
  {
    SerdeType<typename std::remove_cvref_t<T>>::GetSchemaHash()
  } -> std::convertible_to<std::string_view>;
  // JSON-encoded message chema
  {
    SerdeType<typename std::remove_cvref_t<T>>::GetSchema()
  } -> std::convertible_to<std::string_view>;
  // Unpack myself from a packet
  {
    SerdeType<typename std::remove_cvref_t<T>>::Unpack(packet)
  } -> std::same_as<typename std::remove_cvref_t<T>>;
  // Pack myself into a packet
  {
    SerdeType<typename std::remove_cvref_t<T>>::Pack(packet, value)
  } -> std::same_as<void>;
};

/**
 * A packet that holds byte-packed data to be sent over NetworkTables.
 */
class Packet {
 public:
  /**
   * Constructs an empty packet.
   */
  explicit Packet(int initialCapacity = 0) : packetData(initialCapacity) {}

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

  template <typename T, typename... I>
    requires wpi::StructSerializable<T, I...>
  inline void Pack(const T& value) {
    // as WPI struct stuff assumes constant data length - reserve at least
    // enough new space for our new member
    size_t newWritePos = writePos + wpi::GetStructSize<T, I...>();
    packetData.resize(newWritePos);

    wpi::PackStruct(
        std::span<uint8_t>{packetData.begin() + writePos, packetData.end()},
        value);

    writePos = newWritePos;
  }

  template <typename T>
    requires(PhotonStructSerializable<T>)
  inline void Pack(const T& value) {
    SerdeType<typename std::remove_cvref_t<T>>::Pack(*this, value);
  }

  template <typename T, typename... I>
    requires wpi::StructSerializable<T, I...>
  inline T Unpack() {
    // Unpack this member, starting at readPos
    T ret = wpi::UnpackStruct<T, I...>(
        std::span<uint8_t>{packetData.begin() + readPos, packetData.end()});
    readPos += wpi::GetStructSize<T, I...>();
    return ret;
  }

  template <typename T>
    requires(PhotonStructSerializable<T>)
  inline T Unpack() {
    return SerdeType<typename std::remove_cvref_t<T>>::Unpack(*this);
  }

  bool operator==(const Packet& right) const;
  bool operator!=(const Packet& right) const;

 private:
  // Data stored in the packet
  std::vector<uint8_t> packetData{};

  size_t readPos = 0;
  size_t writePos = 0;
};

template <typename T>
concept arithmetic = std::integral<T> || std::floating_point<T>;

// support encoding vectors
template <typename T>
  requires(PhotonStructSerializable<T> || arithmetic<T>)
struct SerdeType<std::vector<T>> {
  static std::vector<T> Unpack(Packet& packet) {
    uint8_t len = packet.Unpack<uint8_t>();
    std::vector<T> ret;
    ret.reserve(len);
    for (size_t i = 0; i < len; i++) {
      ret.push_back(packet.Unpack<T>());
    }
    return ret;
  }
  static void Pack(Packet& packet, const std::vector<T>& value) {
    packet.Pack<uint8_t>(value.size());
    for (const auto& thing : value) {
      packet.Pack<T>(thing);
    }
  }
  static constexpr std::string_view GetSchemaHash() {
    // quick hack lol
    return SerdeType<T>::GetSchemaHash();
  }

  static constexpr std::string_view GetSchema() {
    // TODO: this gets us the plain type name of T, but this is not schema JSON
    // compliant!
    return wpi::Demangle(typeid(T).name());
  }
};

// support encoding optional types
template <typename T>
  requires(PhotonStructSerializable<T> || arithmetic<T>)
struct SerdeType<std::optional<T>> {
  static std::optional<T> Unpack(Packet& packet) {
    if (packet.Unpack<uint8_t>() == 1u) {
      return packet.Unpack<T>();
    } else {
      return std::nullopt;
    }
  }
  static void Pack(Packet& packet, const std::optional<T>& value) {
    packet.Pack<uint8_t>(value.has_value());
    if (value) {
      packet.Pack<T>(*value);
    }
  }
  static constexpr std::string_view GetSchemaHash() {
    // quick hack lol
    return SerdeType<T>::GetSchemaHash();
  }

  static constexpr std::string_view GetSchema() {
    // TODO: this gets us the plain type name of T, but this is not schema JSON
    // compliant!
    return wpi::Demangle(typeid(T).name());
  }
};

}  // namespace photon
