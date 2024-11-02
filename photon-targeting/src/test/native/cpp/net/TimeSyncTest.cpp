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
    fmt::println("Offset={} rtt={}", m.offset, m.rtt2);
  }

  server.Stop();
}
