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
#include <photon/PhotonCamera.h>
#include <photon/simulation/PhotonCameraSim.h>

#include <string>
#include <vector>

#include <frc/smartdashboard/SmartDashboard.h>
#include <networktables/NetworkTableInstance.h>

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

TEST(PhotonCameraTest, Alerts) {
  using frc::SmartDashboard;

  // GIVEN a local-only NT instance
  auto inst = nt::NetworkTableInstance::GetDefault();
  inst.StopClient();
  inst.StopServer();
  inst.StartLocal();
  // (We can't create our own instance, SmartDashboard will always use the
  // default)

  const std::string cameraName = "foobar";

  // AND a PhotonCamera that is disconnected
  photon::PhotonCamera camera(inst, cameraName);
  EXPECT_FALSE(camera.IsConnected());
  std::string disconnectedCameraString =
      "PhotonCamera '" + cameraName + "' is disconnected.";

  // Loop to hit cases past first iteration
  for (int i = 0; i < 10; i++) {
    // WHEN we update the camera
    camera.GetAllUnreadResults();
    // AND we tick SmartDashboard
    SmartDashboard::UpdateValues();

    // The alert state will be set (hard-coded here)
    auto alerts = SmartDashboard::GetStringArray("PhotonAlerts/warnings", {});
    EXPECT_TRUE(
        std::any_of(alerts.begin(), alerts.end(),
                    [&disconnectedCameraString](const std::string& alert) {
                      return alert == disconnectedCameraString;
                    }));

    std::this_thread::sleep_for(std::chrono::milliseconds(20));
  }

  // GIVEN a simulated camera
  photon::PhotonCameraSim sim(&camera);
  // AND a result with a timeSinceLastPong in the past
  photon::PhotonPipelineMetadata metadata{1, 2, 3, 10 * 1000000};
  photon::PhotonPipelineResult noPongResult{
      metadata, std::vector<photon::PhotonTrackedTarget>{}, std::nullopt};

  // Loop to hit cases past first iteration
  for (int i = 0; i < 10; i++) {
    // AND a PhotonCamera with a "new" result
    sim.SubmitProcessedFrame(noPongResult);
    // WHEN we update the camera
    camera.GetAllUnreadResults();
    // AND we tick SmartDashboard
    SmartDashboard::UpdateValues();

    // THEN the camera isn't disconnected
    auto alerts = SmartDashboard::GetStringArray("PhotonAlerts/warnings", {});
    EXPECT_TRUE(
        std::none_of(alerts.begin(), alerts.end(),
                     [&disconnectedCameraString](const std::string& alert) {
                       return alert == disconnectedCameraString;
                     }));

    // AND the alert string looks like a timesync warning
    EXPECT_EQ(
        1, std::count_if(
               alerts.begin(), alerts.end(), [](const std::string& alert) {
                 return alert.find("is not connected to the TimeSyncServer") !=
                        std::string::npos;
               }));

    std::this_thread::sleep_for(std::chrono::milliseconds(20));
  }
}
