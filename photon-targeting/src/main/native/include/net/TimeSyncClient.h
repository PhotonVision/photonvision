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

  // public for testability
  void UpdateStatistics(uint64_t pong_local_time, wpi::tsp::TspPing ping,
                        wpi::tsp::TspPong pong);
};

}  // namespace tsp
}  // namespace wpi
