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

#include <gtest/gtest.h>
#include <hal/HAL.h>
#include <net/TimeSyncClient.h>
#include <net/TimeSyncServer.h>

TEST(TimeSyncProtocolTest, Smoketest) {
  using namespace wpi::tsp;
  using namespace std::chrono_literals;

  TimeSyncServer server{5812};
  TimeSyncClient client{"127.0.0.1", 5812, 100ms};

  server.Start();
  client.Start();

  for (int i = 0; i < 10; i++) {
    std::this_thread::sleep_for(100ms);
    TimeSyncClient::Metadata m = client.GetMetadata();
    wpi::println("Offset={} rtt={}", m.offset, m.rtt2);
  }

  server.Stop();
}

TEST(TimeSyncClientTest, CalculateZero) {
  using namespace wpi::tsp;
  using namespace std::chrono_literals;

  // GIVEN a fresh client
  TimeSyncClient client{"127.0.0.1", 5812, 100ms};

  // AND a ping-pong sent with no delay
  // client -> server -> client
  uint64_t ping_client_time{100};
  uint64_t pong_server_time{100};
  uint64_t pong_client_time{100};

  // setup our ping/pong packets
  TspPing ping{.version = 1, .message_id = 1, .client_time = ping_client_time};
  TspPong pong{ping, pong_server_time};

  // WHEN we update statistics
  client.UpdateStatistics(pong_client_time, ping, pong);

  // THEN the statistics will reflect no delay
  EXPECT_EQ(0, client.GetMetadata().offset);
  EXPECT_EQ(0, client.GetMetadata().rtt2);
  EXPECT_EQ(1u, client.GetMetadata().pongsReceived);
  EXPECT_EQ(pong_client_time, client.GetMetadata().lastPongTime);
}

TEST(TimeSyncClientTest, CalculateZeroOffset) {
  using namespace wpi::tsp;
  using namespace std::chrono_literals;

  // GIVEN a fresh client
  TimeSyncClient client{"127.0.0.1", 5812, 100ms};

  // AND a ping-pong sent with 10ms delay each way
  // client -> server -> client
  uint64_t ping_client_time{100};
  uint64_t pong_server_time{110};
  uint64_t pong_client_time{120};

  // setup our ping/pong packets
  TspPing ping{.version = 1, .message_id = 1, .client_time = ping_client_time};
  TspPong pong{ping, pong_server_time};

  // WHEN we update statistics
  client.UpdateStatistics(pong_client_time, ping, pong);

  // THEN the statistics will reflect no offset, and the expected rtt2
  // (client-to-client) latency
  EXPECT_EQ(0, client.GetMetadata().offset);
  EXPECT_EQ(20, client.GetMetadata().rtt2);
  EXPECT_EQ(1u, client.GetMetadata().pongsReceived);
  EXPECT_EQ(pong_client_time, client.GetMetadata().lastPongTime);
}

TEST(TimeSyncClientTest, CalculateZeroRtt) {
  using namespace wpi::tsp;
  using namespace std::chrono_literals;

  // GIVEN a fresh client
  TimeSyncClient client{"127.0.0.1", 5812, 100ms};

  // AND a ping-pong sent with no delay
  // client -> server -> client
  uint64_t ping_client_time{100};
  uint64_t pong_server_time{123};
  uint64_t pong_client_time{100};

  // setup our ping/pong packets
  TspPing ping{.version = 1, .message_id = 1, .client_time = ping_client_time};
  TspPong pong{ping, pong_server_time};

  // WHEN we update statistics
  client.UpdateStatistics(pong_client_time, ping, pong);

  // THEN the statistics will reflect the expected 23ms offset
  EXPECT_EQ(23, client.GetMetadata().offset);
  EXPECT_EQ(0, client.GetMetadata().rtt2);
  EXPECT_EQ(1u, client.GetMetadata().pongsReceived);
  EXPECT_EQ(pong_client_time, client.GetMetadata().lastPongTime);
}

TEST(TimeSyncClientTest, CalculateBoth) {
  using namespace wpi::tsp;
  using namespace std::chrono_literals;

  // GIVEN a fresh client
  TimeSyncClient client{"127.0.0.1", 5812, 100ms};

  // AND a ping-pong sent with no delay
  // client -> server -> client
  int64_t offset{-234};
  int64_t network_latency{23};

  uint64_t ping_client_time{100};
  uint64_t pong_server_time{ping_client_time + offset + network_latency};
  uint64_t pong_client_time{ping_client_time + 2 * network_latency};

  // setup our ping/pong packets
  TspPing ping{.version = 1, .message_id = 1, .client_time = ping_client_time};
  TspPong pong{ping, pong_server_time};

  // WHEN we update statistics
  client.UpdateStatistics(pong_client_time, ping, pong);

  // THEN the statistics will reflect the expected latency and RTT
  EXPECT_EQ(offset, client.GetMetadata().offset);
  EXPECT_EQ(network_latency * 2, client.GetMetadata().rtt2);
  EXPECT_EQ(1u, client.GetMetadata().pongsReceived);
  EXPECT_EQ(pong_client_time, client.GetMetadata().lastPongTime);
}
