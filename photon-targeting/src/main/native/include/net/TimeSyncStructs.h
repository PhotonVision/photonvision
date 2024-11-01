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
#include <stdint.h>

#include <wpi/struct/Struct.h>

namespace wpi {
namespace tsp {

struct TspPing {
  uint8_t version;
  uint8_t message_id;
  uint64_t client_time;
};

struct TspPong : public TspPing {
  TspPong(TspPing ping, uint64_t servertime)
      : TspPing{ping}, server_time{servertime} {}
  uint64_t server_time;
};

}  // namespace tsp
}  // namespace wpi

template <>
struct wpi::Struct<wpi::tsp::TspPing> {
  static constexpr std::string_view GetTypeName() { return "TspPing"; }
  static constexpr size_t GetSize() { return 10; }
  static constexpr std::string_view GetSchema() {
    return "uint8 version;uint8 message_id;uint64 client_time";
  }

  static wpi::tsp::TspPing Unpack(std::span<const uint8_t> data) {
    return wpi::tsp::TspPing{
        wpi::UnpackStruct<uint8_t, 0>(data),
        wpi::UnpackStruct<uint8_t, 1>(data),
        wpi::UnpackStruct<uint64_t, 2>(data),
    };
  }
  static void Pack(std::span<uint8_t> data, const wpi::tsp::TspPing& value) {
    wpi::PackStruct<0>(data, value.version);
    wpi::PackStruct<1>(data, value.message_id);
    wpi::PackStruct<2>(data, value.client_time);
  }
};

template <>
struct wpi::Struct<wpi::tsp::TspPong> {
  static constexpr std::string_view GetTypeName() { return "TspPong"; }
  static constexpr size_t GetSize() { return 18; }
  static constexpr std::string_view GetSchema() {
    return "uint8 version;uint8 message_id;uint64 client_time;uint64_t "
           "server_time";
  }

  static wpi::tsp::TspPong Unpack(std::span<const uint8_t> data) {
    return wpi::tsp::TspPong{
        wpi::tsp::TspPing{
            wpi::UnpackStruct<uint8_t, 0>(data),
            wpi::UnpackStruct<uint8_t, 1>(data),
            wpi::UnpackStruct<uint64_t, 2>(data),
        },
        wpi::UnpackStruct<uint64_t, 10>(data),
    };
  }
  static void Pack(std::span<uint8_t> data, const wpi::tsp::TspPong& value) {
    wpi::PackStruct<0>(data, value.version);
    wpi::PackStruct<1>(data, value.message_id);
    wpi::PackStruct<2>(data, value.client_time);
    wpi::PackStruct<10>(data, value.server_time);
  }
};

static_assert(wpi::StructSerializable<wpi::tsp::TspPong>);
static_assert(wpi::StructSerializable<wpi::tsp::TspPing>);
