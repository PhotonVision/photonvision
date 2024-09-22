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
#include <net/TimeSyncClientServer.h>

TEST(TimeSyncProtocolTest, TestClient) {
  using namespace wpi;
  using namespace std::chrono_literals;

  HAL_Initialize(500, 0);

  TimeSyncServer server{5812};

  server.Start();

  for (int i = 0; i < 10; i++) {
    std::this_thread::sleep_for(50ms);
    // busywait
  }

  server.Stop();
}
