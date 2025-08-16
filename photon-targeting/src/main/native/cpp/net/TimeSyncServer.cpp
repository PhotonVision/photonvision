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

#include "net/TimeSyncServer.h"

#include <wpinet/UDPClient.h>
#include <wpinet/uv/util.h>

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <iostream>
#include <mutex>
#include <thread>

#include <wpi/Logger.h>
#include <wpi/print.h>
#include <wpi/struct/Struct.h>

#include "ntcore_cpp.h"

static void ServerLoggerFunc(unsigned int level, const char* file,
                             unsigned int line, const char* msg) {
  if (level == 20) {
    fmt::print(stderr, "TimeSyncServer: {}\n", msg);
    return;
  }

  std::string_view levelmsg;
  if (level >= 50) {
    levelmsg = "CRITICAL";
  } else if (level >= 40) {
    levelmsg = "ERROR";
  } else if (level >= 30) {
    levelmsg = "WARNING";
  } else {
    return;
  }
  fmt::print(stderr, "TimeSyncServer: {}: {} ({}:{})\n", levelmsg, msg, file,
             line);
}

void wpi::tsp::TimeSyncServer::UdpCallback(uv::Buffer& data, size_t n,
                                           const sockaddr& sender,
                                           unsigned flags) {
  // wpi::println("TimeSyncServer got ping!");

  TspPing ping{wpi::UnpackStruct<TspPing>(data.bytes())};

  if (ping.version != 1) {
    WPI_ERROR(m_logger, "Bad version from client?");
    return;
  }
  if (ping.message_id != 1) {
    WPI_ERROR(m_logger, "Bad message id from client?");
    return;
  }

  uint64_t current_time = m_timeProvider();

  TspPong pong{ping, current_time};
  pong.message_id = 2;

  wpi::SmallVector<uint8_t, wpi::Struct<TspPong>::GetSize()> pongData(
      wpi::Struct<TspPong>::GetSize());
  wpi::PackStruct(pongData, pong);

  // Wrap our buffer - pongData should free itself for free
  wpi::uv::Buffer pongBuf{pongData};
  int sent =
      m_udp->TrySend(sender, wpi::SmallVector<wpi::uv::Buffer, 1>{pongBuf});
  // wpi::println("Pong ret: {}", sent);
  if (static_cast<size_t>(sent) != wpi::Struct<TspPong>::GetSize()) {
    WPI_ERROR(m_logger, "Didn't send the whole pong back?");
    return;
  }

  // WPI_INFO(m_logger, "Got ping: {} {} {}", ping.version, ping.message_id,
  //          ping.client_time);
  // WPI_INFO(m_logger, "Sent pong: {} {} {} {}", pong.version, pong.message_id,
  //          pong.client_time, pong.server_time);
}

wpi::tsp::TimeSyncServer::TimeSyncServer(int port)
    : m_logger{::ServerLoggerFunc},
      m_timeProvider{nt::Now},
      m_udp{},
      m_port(port) {}

void wpi::tsp::TimeSyncServer::Start() {
  m_loopRunner.ExecSync([this](uv::Loop&) {
    m_udp = {wpi::uv::Udp::Create(m_loopRunner.GetLoop(), AF_INET)};
    m_udp->Bind("0.0.0.0", m_port);
    m_udp->received.connect(&wpi::tsp::TimeSyncServer::UdpCallback, this);
    m_udp->StartRecv();
  });
}

void wpi::tsp::TimeSyncServer::Stop() { m_loopRunner.Stop(); }
