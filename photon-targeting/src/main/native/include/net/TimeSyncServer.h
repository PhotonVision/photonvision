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
#include <wpi/print.h>
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
