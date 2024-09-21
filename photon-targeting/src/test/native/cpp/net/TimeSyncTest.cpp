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
#include <net/TimeSyncClientServer.h>

TEST(TimeSyncProtocolTest, TestClient) {
  using namespace wpi;
  using namespace std::chrono_literals;

  static auto server_bogus_offset = -nt::Now();
  TimeSyncServer server{5810, []() { return nt::Now() + server_bogus_offset; }};
  TimeSyncClient client{"127.0.0.1", 5810, 1s};

  server.Start();
  std::this_thread::sleep_for(0.5s);
  client.Start();

  for (int i = 0; i < 5; i++) {
    auto off = client.GetOffset();
    fmt::println("Unit Test: current offset = {} uS, error = {} uS", off,
                 off - static_cast<int64_t>(server_bogus_offset));
    std::this_thread::sleep_for(1s);
  }

  server.Stop();
  client.Stop();
}
