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

#include "net/TimeSyncServer.h"

#include <fmt/core.h>
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
  // fmt::println("TimeSyncServer got ping!");

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
  // fmt::println("Pong ret: {}", sent);
  if (static_cast<size_t>(sent) != wpi::Struct<TspPong>::GetSize()) {
    WPI_ERROR(m_logger, "Didn't send the whole pong back?");
    return;
  }

  // WPI_INFO(m_logger, "Got ping: {} {} {}", ping.version, ping.message_id,
  //          ping.client_time);
  // WPI_INFO(m_logger, "Sent pong: {} {} {} {}", pong.version, pong.message_id,
  //          pong.client_time, pong.server_time);
}

wpi::tsp::TimeSyncServer::TimeSyncServer(int port,
                                         std::function<uint64_t()> timeProvider)
    : m_logger{::ServerLoggerFunc},
      m_timeProvider{timeProvider},
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
