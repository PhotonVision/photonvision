/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

#include <networktables/NetworkTable.h>
#include <networktables/NetworkTableEntry.h>
#include <networktables/NetworkTableInstance.h>
#include <units/angle.h>
#include <units/length.h>

#include "gtest/gtest.h"
#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonUtils.h"
#include "photonlib/SimVisionSystem.h"

TEST(SimVisionSystemTest, Empty) {
  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          320, 240, 0.0);

  for (int loopIdx = 0; loopIdx < 100; loopIdx++) {
    sysUnderTest.ProcessFrame(frc::Pose2d());
  }
}

class SimVisionSystemDistParamTest : public testing::TestWithParam<double> {};
INSTANTIATE_TEST_SUITE_P(SimVisionSystemDistParamTests,
                         SimVisionSystemDistParamTest,
                         testing::Values(5, 10, 15, 20, 25, 30));

TEST_P(SimVisionSystemDistParamTest, DistanceAligned) {
  double dist = GetParam();

  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          320, 240, 0.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 0.0_m, 1.0_m, 1.0_m));

  auto robotPose = frc::Pose2d(
      frc::Translation2d(units::meter_t(35.0 - dist), 0_m), frc::Rotation2d());

  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  ASSERT_TRUE(result.HasTargets());
  ASSERT_EQ(result.GetBestTarget()
                .GetCameraRelativePose()
                .Translation()
                .Norm()
                .value(),
            dist);
}

TEST(SimVisionSystemTest, VisibilityCupidShuffle) {
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          320, 240, 0.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 0.0_m, 3.0_m, 3.0_m));

  // To the right, to the right
  auto robotPose =
      frc::Pose2d(frc::Translation2d(5.0_m, 0.0_m), frc::Rotation2d(-70.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());

  // To the right, to the right
  robotPose =
      frc::Pose2d(frc::Translation2d(5.0_m, 0.0_m), frc::Rotation2d(-95.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());

  // To the left, to the left
  robotPose =
      frc::Pose2d(frc::Translation2d(5.0_m, 0.0_m), frc::Rotation2d(90.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());

  // To the left, to the left
  robotPose =
      frc::Pose2d(frc::Translation2d(5.0_m, 0.0_m), frc::Rotation2d(65.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());

  // now kick, now kick
  robotPose =
      frc::Pose2d(frc::Translation2d(2.0_m, 0.0_m), frc::Rotation2d(5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_TRUE(result.HasTargets());

  // now kick, now kick
  robotPose =
      frc::Pose2d(frc::Translation2d(2.0_m, 0.0_m), frc::Rotation2d(-5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_TRUE(result.HasTargets());

  // now walk it by yourself
  robotPose = frc::Pose2d(frc::Translation2d(2.0_m, 0.0_m),
                          frc::Rotation2d(-179.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());

  // now walk it by yourself
  sysUnderTest.MoveCamera(
      frc::Transform2d(frc::Translation2d(), frc::Rotation2d(180_deg)), 0.0_m,
      1.0_deg);
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_TRUE(result.HasTargets());
}

TEST(SimVisionSystemTest, NotVisibleVert1) {
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          640, 480, 0.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 1.0_m, 3.0_m, 2.0_m));

  auto robotPose =
      frc::Pose2d(frc::Translation2d(5.0_m, 0.0_m), frc::Rotation2d(5.0_deg));

  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  ASSERT_TRUE(result.HasTargets());

  sysUnderTest.MoveCamera(
      frc::Transform2d(frc::Translation2d(), frc::Rotation2d(0_deg)), 5000.0_m,
      1.0_deg);
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());
}

TEST(SimVisionSystemTest, NotVisibleVert2) {
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 45.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          1234, 1234, 0.5);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 3.0_m, 0.5_m, 0.5_m));

  auto robotPose =
      frc::Pose2d(frc::Translation2d(32.0_m, 0.0_m), frc::Rotation2d(5.0_deg));

  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  EXPECT_TRUE(result.HasTargets());

  robotPose =
      frc::Pose2d(frc::Translation2d(0.0_m, 0.0_m), frc::Rotation2d(5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());
}

TEST(SimVisionSystemTest, NotVisibleTgtSize) {
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          640, 480, 20.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 1.10_m, 0.25_m, 0.1_m));

  auto robotPose =
      frc::Pose2d(frc::Translation2d(32.0_m, 0.0_m), frc::Rotation2d(5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  EXPECT_TRUE(result.HasTargets());

  robotPose =
      frc::Pose2d(frc::Translation2d(0.0_m, 0.0_m), frc::Rotation2d(5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());
}

TEST(SimVisionSystemTest, NotVisibleTooFarForLEDs) {
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 10.0_m,
                                          640, 480, 1.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 1.10_m, 0.25_m, 0.1_m));

  auto robotPose =
      frc::Pose2d(frc::Translation2d(28.0_m, 0.0_m), frc::Rotation2d(5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  EXPECT_TRUE(result.HasTargets());

  robotPose =
      frc::Pose2d(frc::Translation2d(0.0_m, 0.0_m), frc::Rotation2d(5.0_deg));
  sysUnderTest.ProcessFrame(robotPose);
  result = sysUnderTest.cam.GetLatestResult();
  EXPECT_FALSE(result.HasTargets());
}

class SimVisionSystemYawParamTest : public testing::TestWithParam<double> {};
INSTANTIATE_TEST_SUITE_P(SimVisionSystemYawParamTests,
                         SimVisionSystemYawParamTest,
                         testing::Values(-10, -5, -0, -1, -2, 5, 7, 10.23));
TEST_P(SimVisionSystemYawParamTest, YawAngles) {
  double testYaw = GetParam();  // Nope, Chuck testYaw
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d(45_deg));

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          640, 480, 0.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 0.0_m, 0.5_m, 0.5_m));

  auto robotPose = frc::Pose2d(frc::Translation2d(32_m, 0.0_m),
                               frc::Rotation2d(units::degree_t(testYaw)));

  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  ASSERT_TRUE(result.HasTargets());
  auto tgt = result.GetBestTarget();
  EXPECT_DOUBLE_EQ(tgt.GetYaw(), testYaw);
}

class SimVisionSystemCameraPitchParamTest
    : public testing::TestWithParam<double> {};
INSTANTIATE_TEST_SUITE_P(SimVisionSystemCameraPitchParamTests,
                         SimVisionSystemCameraPitchParamTest,
                         testing::Values(-10, -5, -0, -1, -2, 5, 7, 10.23,
                                         20.21, -19.999));
TEST_P(SimVisionSystemCameraPitchParamTest, CameraPitch) {
  double testPitch = GetParam();
  auto targetPose =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d(45_deg));

  auto robotPose =
      frc::Pose2d(frc::Translation2d(30_m, 0.0_m), frc::Rotation2d(0.0_deg));

  photonlib::SimVisionSystem sysUnderTest("Test", 80.0_deg, 0.0_deg,
                                          frc::Transform2d(), 1.0_m, 99999.0_m,
                                          640, 480, 0.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPose, 0.0_m, 0.5_m, 0.5_m));

  sysUnderTest.MoveCamera(
      frc::Transform2d(frc::Translation2d(), frc::Rotation2d()), 0.0_m,
      units::degree_t(testPitch));

  photonlib::PhotonCamera::SetVersionCheckEnabled(false);
  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  ASSERT_TRUE(result.HasTargets());
  auto tgt = result.GetBestTarget();
  // If the camera is pitched down by 10 degrees, the target should appear
  // in the upper part of the image (ie, pitch positive). Therefor,
  // pass/fail involves -1.0.
  EXPECT_DOUBLE_EQ(tgt.GetPitch(), -testPitch);
}

class SimVisionSystemDistCalcParamTest
    : public testing::TestWithParam<std::tuple<double, double, double>> {};
INSTANTIATE_TEST_SUITE_P(
    SimVisionSystemDistCalcParamTests, SimVisionSystemDistCalcParamTest,
    testing::Values(std::tuple<double, double, double>(5, 35, 0),
                    std::tuple<double, double, double>(6, 35, 1),
                    std::tuple<double, double, double>(10, 35, 0),
                    std::tuple<double, double, double>(15, 35, 2),
                    std::tuple<double, double, double>(19.95, 35, 0),
                    std::tuple<double, double, double>(20, 35, 0),
                    std::tuple<double, double, double>(5, 42, 1),
                    std::tuple<double, double, double>(6, 42, 0),
                    std::tuple<double, double, double>(10, 42, 2),
                    std::tuple<double, double, double>(15, 42, 0.5),
                    std::tuple<double, double, double>(19.42, 35, 0),
                    std::tuple<double, double, double>(20, 42, 0),
                    std::tuple<double, double, double>(5, 55, 2),
                    std::tuple<double, double, double>(6, 55, 0),
                    std::tuple<double, double, double>(10, 54, 2.2),
                    std::tuple<double, double, double>(15, 53, 0),
                    std::tuple<double, double, double>(19.52, 35, 1.1),
                    std::tuple<double, double, double>(20, 51, 2.87),
                    std::tuple<double, double, double>(20, 55, 3)));
// TEST_P(SimVisionSystemDistCalcParamTest, DistanceCalc) {
//   std::tuple<double, double, double> testArgs = GetParam();
//   double testDist = std::get<0>(testArgs);
//   double testPitch = std::get<1>(testArgs);
//   double testHeight = std::get<2>(testArgs);

//   auto targetPose = frc::Pose2d(frc::Translation2d(35_m, 0_m),
//                                 frc::Rotation2d(units::radian_t(3.14159 /
//                                                                      42)));

//   auto robotPose =
//       frc::Pose2d(frc::Translation2d(units::meter_t(35 - testDist), 0.0_m),
//                   frc::Rotation2d(0.0_deg));

//   photonlib::SimVisionSystem sysUnderTest(
//       "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysoho"
//       "wsyourdaygoingihopegoodhaveagreatrestofyourlife",
//       160.0_deg, units::degree_t(testPitch), frc::Transform2d(),
//       units::meter_t(testHeight), 99999.0_m, 640, 480, 0.0);

//   sysUnderTest.AddSimVisionTarget(photonlib::SimVisionTarget(
//       targetPose, units::meter_t(testDist), 0.5_m, 0.5_m));

//   sysUnderTest.ProcessFrame(robotPose);
//   auto result = sysUnderTest.cam.GetLatestResult();
//   ASSERT_TRUE(result.HasTargets());
//   auto tgt = result.GetBestTarget();
//   EXPECT_DOUBLE_EQ(tgt.GetYaw(), 0.0);
//   units::meter_t distMeas =
//   photonlib::PhotonUtils::CalculateDistanceToTarget(
//       units::meter_t(testHeight), units::meter_t(testDist),
//       units::degree_t(testPitch), units::degree_t(tgt.GetPitch()));
//   EXPECT_DOUBLE_EQ(distMeas.value(), testDist);
// }

TEST(SimVisionSystemTest, MultipleTargets) {
  auto targetPoseL =
      frc::Pose2d(frc::Translation2d(35_m, 2_m), frc::Rotation2d());
  auto targetPoseC =
      frc::Pose2d(frc::Translation2d(35_m, 0_m), frc::Rotation2d());
  auto targetPoseR =
      frc::Pose2d(frc::Translation2d(35_m, -2_m), frc::Rotation2d());

  photonlib::SimVisionSystem sysUnderTest("test", 160.0_deg, 0.0_deg,
                                          frc::Transform2d(), 5.0_m, 99999.0_m,
                                          640, 480, 20.0);

  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseL, 0.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseC, 1.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseR, 2.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseL, 3.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseC, 4.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseR, 5.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseL, 6.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseC, 7.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseL, 8.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseR, 9.0_m, 0.25_m, 0.1_m));
  sysUnderTest.AddSimVisionTarget(
      photonlib::SimVisionTarget(targetPoseL, 10.0_m, 0.25_m, 0.1_m));

  auto robotPose =
      frc::Pose2d(frc::Translation2d(30_m, 0.0_m), frc::Rotation2d(5.0_deg));

  sysUnderTest.ProcessFrame(robotPose);
  auto result = sysUnderTest.cam.GetLatestResult();
  ASSERT_TRUE(result.HasTargets());

  auto tgtList = result.GetTargets();
  EXPECT_EQ(11ul, tgtList.size());
}
