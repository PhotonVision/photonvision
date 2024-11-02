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

#include "photon/PhotonCamera.h"

TEST(TimeSyncProtocolTest, Smoketest) {
  using namespace wpi::tsp;
  using namespace std::chrono_literals;

  // start a server implicitly
  photon::PhotonCamera camera{"camera"};

  TimeSyncClient client{"127.0.0.1", 5810, 100ms};
  client.Start();

  for (int i = 0; i < 10; i++) {
    std::this_thread::sleep_for(100ms);
    TimeSyncClient::Metadata m = client.GetMetadata();

    // give us time to warm up
    if (i > 5) {
      EXPECT_TRUE(m.rtt2 > 0);
      EXPECT_TRUE(m.pongsReceived > 0);
    }
  }

  client.Stop();
}
