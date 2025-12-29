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

#include <cstdlib>
#include <cstring>
#include <ctime>
#include <functional>
#include <memory>
#include <thread>

#include <wpi/net/EventLoopRunner.hpp>
#include <wpi/net/uv/Buffer.hpp>
#include <wpi/net/uv/Udp.hpp>
#include <wpi/util/Logger.hpp>

namespace wpi {
namespace tsp {

class TimeSyncServer {
  using SharedUdpPtr = std::shared_ptr<wpi::net::uv::Udp>;

  wpi::net::EventLoopRunner m_loopRunner{};

  wpi::util::Logger m_logger;
  std::function<uint64_t()> m_timeProvider;
  SharedUdpPtr m_udp;
  int m_port;

  std::thread m_listener;

 private:
  void UdpCallback(wpi::net::uv::Buffer& buf, size_t nbytes, const sockaddr& sender,
                   unsigned flags);

 public:
  explicit TimeSyncServer(int port = 5810);

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
