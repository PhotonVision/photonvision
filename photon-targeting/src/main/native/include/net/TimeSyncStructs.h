/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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
