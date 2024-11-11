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

#include <photon/PhotonCamera.h>

#include <iostream>

#include <frc/Timer.h>
#include <networktables/NetworkTableInstance.h>
#include <units/time.h>

#include "PhotonVersion.h"
#include "gtest/gtest.h"

TEST(PhotonCameraTest, ListenToTestMode) {
  auto inst = nt::NetworkTableInstance::GetDefault();
  inst.StopServer();
  inst.SetServer("localhost");
  inst.StartClient4("PhotonLib Integration Test");

  photon::PhotonCamera camera{"WPI2024"};

  size_t total = 0;
  for (size_t i = 0; i < 5; i++) {
    frc::Wait(1_s);

    auto results = camera.GetAllUnreadResults();
    total += results.size();
  }

  // we should have gotten a handful of messages by now
  fmt::println("got {} new results", total);
  EXPECT_GT(total, 20U);
}
