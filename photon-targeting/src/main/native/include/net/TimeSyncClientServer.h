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

#include <fmt/core.h>
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

#include <wpi/Logger.h>
#include <wpi/struct/Struct.h>

#include "ntcore_cpp.h"

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

namespace wpi {

class TimeSyncServer {
  using SharedUdpPtr = std::shared_ptr<uv::Udp>;

  EventLoopRunner m_loopRunner{};

  wpi::Logger m_logger;
  std::function<uint64_t()> m_timeProvider;
  SharedUdpPtr m_udp;

  std::thread m_listener;

 private:
  void UdpCallback(uv::Buffer& buf, size_t nbytes, const sockaddr& sender,
                   unsigned flags);

 public:
  explicit TimeSyncServer(int port = 5810,
                          std::function<uint64_t()> timeProvider = nt::Now);

  /**
   * Start listening for pings
   */
  void Start();
  /**
   * Stop our loop runner. After stopping, we cannot restart.
   */
  void Stop();
};

class TimeSyncClient {
 public:
  struct Metadata {
    int64_t offset{0};
    size_t pingsSent;
    size_t pongsRecieved;
    uint64_t lastPongTime;
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

  std::mutex m_offsetMutex;
  Metadata m_metadata;

  // We only allow the most recent ping to stay alive, so only keep track of it
  TspPing m_lastPing;

  void Tick();

  void UdpCallback(uv::Buffer& buf, size_t nbytes, const sockaddr& sender,
                   unsigned flags);

 public:
  TimeSyncClient(std::string_view server, int remote_port,
                 std::chrono::milliseconds ping_delay,
                 std::function<uint64_t()> timeProvider = nt::Now);

  void Start();
  void Stop();
  int64_t GetOffset();
  Metadata GetMetadata();
};

}  // namespace wpi
