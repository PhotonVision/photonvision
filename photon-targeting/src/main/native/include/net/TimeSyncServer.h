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

#include "TimeSyncStructs.h"
#include "ntcore_cpp.h"

namespace wpi {
namespace tsp {

class TimeSyncServer {
  using SharedUdpPtr = std::shared_ptr<uv::Udp>;

  EventLoopRunner m_loopRunner{};

  wpi::Logger m_logger;
  std::function<uint64_t()> m_timeProvider;
  SharedUdpPtr m_udp;
  int m_port;

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

}  // namespace tsp
}  // namespace wpi
