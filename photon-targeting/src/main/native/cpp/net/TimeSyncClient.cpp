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

#include "net/TimeSyncClient.h"

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

#include <Eigen/Core>
#include <wpi/Logger.h>
#include <wpi/struct/Struct.h>

#include "ntcore_cpp.h"

static void ClientLoggerFunc(unsigned int level, const char* file,
                             unsigned int line, const char* msg) {
  if (level == 20) {
    fmt::print(stderr, "TimeSyncClient: {}\n", msg);
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
  fmt::print(stderr, "TimeSyncClient: {}: {} ({}:{})\n", levelmsg, msg, file,
             line);
}

void wpi::tsp::TimeSyncClient::Tick() {
  // fmt::println("wpi::tsp::TimeSyncClient::Tick");
  // Regardless of if we've gotten a pong back yet, we'll ping again. this is
  // pretty naive but should be "fine" for now?

  uint64_t ping_local_time{m_timeProvider()};

  TspPing ping{.version = 1, .message_id = 1, .client_time = ping_local_time};

  wpi::SmallVector<uint8_t, wpi::Struct<TspPing>::GetSize()> pingData(
      wpi::Struct<TspPing>::GetSize());
  wpi::PackStruct(pingData, ping);

  // Wrap our buffer - pingData should free itself
  wpi::uv::Buffer pingBuf{pingData};
  int sent = m_udp->TrySend(wpi::SmallVector<wpi::uv::Buffer, 1>{pingBuf});

  if (static_cast<size_t>(sent) != wpi::Struct<TspPing>::GetSize()) {
    WPI_ERROR(m_logger, "Didn't send the whole ping out? sent {} bytes", sent);
    return;
  }

  {
    std::lock_guard lock{m_offsetMutex};
    m_metadata.pingsSent++;
  }

  m_lastPing = ping;
}

void wpi::tsp::TimeSyncClient::UdpCallback(uv::Buffer& buf, size_t nbytes,
                                           const sockaddr& sender,
                                           unsigned flags) {
  uint64_t pong_local_time = m_timeProvider();

  if (static_cast<size_t>(nbytes) != wpi::Struct<TspPong>::GetSize()) {
    WPI_ERROR(m_logger, "Got {} bytes for pong?", nbytes);
    return;
  }

  TspPong pong{
      wpi::UnpackStruct<TspPong>(buf.bytes()),
  };

  // fmt::println("->[client] Got pong: {} {} {} {}", pong.version,
  //              pong.message_id, pong.client_time, pong.server_time);

  if (pong.version != 1) {
    fmt::println("Bad version from server?");
    return;
  }
  if (pong.message_id != 2) {
    fmt::println("Bad message id from server?");
    return;
  }

  TspPing ping = m_lastPing;

  if (pong.client_time != ping.client_time) {
    WPI_WARNING(m_logger,
                "Pong was not a reply to our ping? Got ping {} vs pong {}",
                ping.client_time, pong.client_time);
    return;
  }

  // when time = send_time+rtt2/2, server time = server time
  // server time = local time + offset
  // offset = (server time - local time) = (server time) - (send_time +
  // rtt2/2)
  auto rtt2 = pong_local_time - ping.client_time;
  int64_t serverTimeOffsetUs = pong.server_time - rtt2 / 2 - ping.client_time;

  auto filtered = m_lastOffsets.Calculate(serverTimeOffsetUs);

  // fmt::println("Ping-ponged! RTT2 {} uS, offset {}/filtered offset {} uS",
  // rtt2,
  //              serverTimeOffsetUs, filtered);

  {
    std::lock_guard lock{m_offsetMutex};
    m_metadata.offset = filtered;
    m_metadata.rtt2 = rtt2;
    m_metadata.pongsReceived++;
    m_metadata.lastPongTime = pong_local_time;
  }

  using std::cout;
  // fmt::println("Ping-ponged! RTT2 {} uS, offset {} uS", rtt2,
  //              serverTimeOffsetUs);
  // fmt::println("Estimated server time {} s",
  //              (m_timeProvider() + serverTimeOffsetUs) / 1000000.0);
}

wpi::tsp::TimeSyncClient::TimeSyncClient(std::string_view server,
                                         int remote_port,
                                         std::chrono::milliseconds ping_delay,
                                         std::function<uint64_t()> timeProvider)
    : m_logger(::ClientLoggerFunc),
      m_timeProvider(timeProvider),
      m_udp{},
      m_pingTimer{},
      m_serverIP{server},
      m_serverPort{remote_port},
      m_loopDelay(ping_delay) {
  // fmt::println("Starting client (with server address {}:{})", server,
  //              remote_port);
}

void wpi::tsp::TimeSyncClient::Start() {
  // fmt::println("Connecting received");

  m_loopRunner.ExecSync([this](uv::Loop&) {
    struct sockaddr_in serverAddr;
    uv::NameToAddr(m_serverIP, m_serverPort, &serverAddr);

    m_udp = {wpi::uv::Udp::Create(m_loopRunner.GetLoop(), AF_INET)};
    m_pingTimer = {wpi::uv::Timer::Create(m_loopRunner.GetLoop())};

    m_udp->Connect(serverAddr);
    m_udp->received.connect(&wpi::tsp::TimeSyncClient::UdpCallback, this);
    m_udp->StartRecv();
  });

  // fmt::println("Starting pinger");
  using namespace std::chrono_literals;
  m_pingTimer->timeout.connect(&wpi::tsp::TimeSyncClient::Tick, this);

  m_loopRunner.ExecSync(
      [this](uv::Loop&) { m_pingTimer->Start(m_loopDelay, m_loopDelay); });
}

void wpi::tsp::TimeSyncClient::Stop() { m_loopRunner.Stop(); }

int64_t wpi::tsp::TimeSyncClient::GetOffset() {
  std::lock_guard lock{m_offsetMutex};
  return m_metadata.offset;
}

wpi::tsp::TimeSyncClient::Metadata wpi::tsp::TimeSyncClient::GetMetadata() {
  std::lock_guard lock{m_offsetMutex};
  return m_metadata;
}
