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

#include <wpinet/EventLoopRunner.h>
#include <wpinet/UDPClient.h>
#include <wpinet/uv/Buffer.h>
#include <wpinet/uv/Timer.h>
#include <wpinet/uv/Udp.h>

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <functional>
#include <iostream>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

#include <frc/filter/MedianFilter.h>
#include <wpi/Logger.h>
#include <wpi/print.h>
#include <wpi/static_circular_buffer.h>
#include <wpi/struct/Struct.h>

#include "TimeSyncStructs.h"
#include "ntcore_cpp.h"

namespace wpi {
namespace tsp {

class TimeSyncClient {
 public:
  struct Metadata {
    int64_t offset{0};
    int64_t rtt2{0};
    size_t pingsSent{0};
    size_t pongsReceived{0};
    uint64_t lastPongTime{0};
  };

 private:
  using SharedUdpPtr = std::shared_ptr<uv::Udp>;
  using SharedTimerPtr = std::shared_ptr<uv::Timer>;

  EventLoopRunner m_loopRunner{};

  wpi::Logger m_logger;
  std::function<uint64_t()> m_timeProvider;

  SharedUdpPtr m_udp;
  SharedTimerPtr m_pingTimer;

  std::string m_serverIP;
  int m_serverPort;

  std::chrono::milliseconds m_loopDelay;

  std::mutex m_offsetMutex{};
  Metadata m_metadata{};

  // We only allow the most recent ping to stay alive, so only keep track of it
  TspPing m_lastPing{};

  // 30s is a reasonable guess
  frc::MedianFilter<int64_t> m_lastOffsets{30};

  void Tick();

  void UdpCallback(uv::Buffer& buf, size_t nbytes, const sockaddr& sender,
                   unsigned flags);

 public:
  TimeSyncClient(std::string_view server, int remote_port,
                 std::chrono::milliseconds ping_delay);

  void Start();
  void Stop();
  int64_t GetOffset();
  Metadata GetMetadata();
};

}  // namespace tsp
}  // namespace wpi
