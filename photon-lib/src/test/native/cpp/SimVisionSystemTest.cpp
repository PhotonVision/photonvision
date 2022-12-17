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

#include "gtest/gtest.h"
#include "photonlib/PhotonUtils.h"
#include "photonlib/SimVisionSystem.h"

class SimVisionSystemTest : public ::testing::Test {
  void SetUp() override {
    nt::NetworkTableInstance::GetDefault().StartServer();
    photonlib::PhotonCamera::SetVersionCheckEnabled(false);
  }

  void TearDown() override {}
};

class SimVisionSystemTestWithParamsTest
    : public SimVisionSystemTest,
      public testing::WithParamInterface<units::degree_t> {};
class SimVisionSystemTestDistanceParamsTest
    : public SimVisionSystemTest,
      public testing::WithParamInterface<
          std::tuple<units::foot_t, units::degree_t, units::foot_t>> {};

TEST_F(SimVisionSystemTest, TestEmpty) {
  photonlib::SimVisionSystem sys{
      "Test", 80.0_deg, frc::Transform3d{}, 99999_m, 320, 240, 0};
  SUCCEED();
}

TEST_F(SimVisionSystemTest, TestVisibilityCupidShuffle) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 2_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  photonlib::SimVisionSystem sys{
      "Test", 80.0_deg, frc::Transform3d{}, 99999_m, 640, 480, 0};
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{targetPose, 1_m, 3_m, 3});

  // To the right, to the right
  frc::Pose2d robotPose{{5_m, 0_m}, frc::Rotation2d{-70_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());

  // To the right, to the right
  robotPose = frc::Pose2d{{5_m, 0_m}, frc::Rotation2d{-95_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());

  // To the left, to the left
  robotPose = frc::Pose2d{{5_m, 0_m}, frc::Rotation2d{90_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());

  // To the left, to the left
  robotPose = frc::Pose2d{{5_m, 0_m}, frc::Rotation2d{65_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());

  // now kick, now kick
  robotPose = frc::Pose2d{{2_m, 0_m}, frc::Rotation2d{5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());

  // now kick, now kick
  robotPose = frc::Pose2d{{2_m, 0_m}, frc::Rotation2d{-5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());

  // now walk it by yourself
  robotPose = frc::Pose2d{{2_m, 0_m}, frc::Rotation2d{-179_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());

  // now walk it by yourself
  sys.MoveCamera(frc::Transform3d{
      frc::Translation3d{},
      frc::Rotation3d{0_rad, 0_rad, units::constants::detail::PI_VAL * 1_rad}});
  sys.ProcessFrame(robotPose);
  ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());
}

TEST_F(SimVisionSystemTest, TestNotVisibleVertOne) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  photonlib::SimVisionSystem sys{
      "Test", 80.0_deg, frc::Transform3d{}, 99999_m, 640, 480, 0};
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{targetPose, 1_m, 3_m, 3});

  frc::Pose2d robotPose{{5_m, 0_m}, frc::Rotation2d{5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());

  sys.MoveCamera(frc::Transform3d{
      frc::Translation3d{0_m, 0_m, 5000_m},
      frc::Rotation3d{0_rad, 0_rad, units::constants::detail::PI_VAL * 1_rad}});
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());
}

// TEST_F(SimVisionSystemTest, TestNotVisibleVertTwo) {
//   const frc::Pose3d targetPose{
//       {15.98_m, 0_m, 2_m},
//       frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL *
//       1_rad}};
//   frc::Transform3d robotToCamera{
//       frc::Translation3d{0_m, 0_m, 1_m},
//       frc::Rotation3d{0_deg, (units::constants::detail::PI_VAL / 4) * 1_rad,
//                       0_deg}};
//   photonlib::SimVisionSystem sys{
//       "Test", 80.0_deg, robotToCamera.Inverse(), 99999_m, 1234, 1234, 0};
//   sys.AddSimVisionTarget(
//       photonlib::SimVisionTarget{targetPose, 3_m, 0.5_m, 1736});

//   frc::Pose2d robotPose{{14.98_m, 0_m}, frc::Rotation2d{5_deg}};
//   sys.ProcessFrame(robotPose);
//   ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());

//   robotPose = frc::Pose2d{frc::Translation2d{0_m, 0_m},
//   frc::Rotation2d{5_deg}}; sys.ProcessFrame(robotPose);
//   ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());
// }

TEST_F(SimVisionSystemTest, TestNotVisibleTargetSize) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  photonlib::SimVisionSystem sys{
      "Test", 80.0_deg, frc::Transform3d{}, 99999_m, 640, 480, 20.0};
  sys.AddSimVisionTarget(
      photonlib::SimVisionTarget{targetPose, 0.1_m, 0.025_m, 24});

  frc::Pose2d robotPose{{12_m, 0_m}, frc::Rotation2d{5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());

  robotPose = frc::Pose2d{frc::Translation2d{0_m, 0_m}, frc::Rotation2d{5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());
}

TEST_F(SimVisionSystemTest, TestNotVisibleTooFarForLEDs) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  photonlib::SimVisionSystem sys{
      "Test", 80.0_deg, frc::Transform3d{}, 10_m, 640, 480, 1.0};
  sys.AddSimVisionTarget(
      photonlib::SimVisionTarget{targetPose, 1_m, 0.25_m, 78});

  frc::Pose2d robotPose{{10_m, 0_m}, frc::Rotation2d{5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_TRUE(sys.cam.GetLatestResult().HasTargets());

  robotPose = frc::Pose2d{frc::Translation2d{0_m, 0_m}, frc::Rotation2d{5_deg}};
  sys.ProcessFrame(robotPose);
  ASSERT_FALSE(sys.cam.GetLatestResult().HasTargets());
}

TEST_P(SimVisionSystemTestWithParamsTest, YawAngles) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg,
                      (3 * units::constants::detail::PI_VAL / 4) * 1_rad}};
  frc::Pose2d robotPose{{10_m, 0_m}, frc::Rotation2d{GetParam() * -1.0}};
  photonlib::SimVisionSystem sys{
      "Test", 120.0_deg, frc::Transform3d{}, 99999_m, 640, 480, 0};
  sys.AddSimVisionTarget(
      photonlib::SimVisionTarget{targetPose, 0.5_m, 0.5_m, 23});

  sys.ProcessFrame(robotPose);

  auto results = sys.cam.GetLatestResult();

  ASSERT_TRUE(results.HasTargets());

  photonlib::PhotonTrackedTarget target = results.GetBestTarget();

  ASSERT_NEAR(GetParam().to<double>(), target.GetYaw(), 0.0001);
}

TEST_P(SimVisionSystemTestWithParamsTest, PitchAngles) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg,
                      (3 * units::constants::detail::PI_VAL / 4) * 1_rad}};
  frc::Pose2d robotPose{{10_m, 0_m}, frc::Rotation2d{0_deg}};
  photonlib::SimVisionSystem sys{
      "Test", 120.0_deg, frc::Transform3d{}, 99999_m, 640, 480, 0};
  sys.AddSimVisionTarget(
      photonlib::SimVisionTarget{targetPose, 0.5_m, 0.5_m, 23});

  sys.MoveCamera(frc::Transform3d{frc::Translation3d{},
                                  frc::Rotation3d{0_deg, GetParam(), 0_deg}});
  sys.ProcessFrame(robotPose);

  auto results = sys.cam.GetLatestResult();

  ASSERT_TRUE(results.HasTargets());

  photonlib::PhotonTrackedTarget target = results.GetBestTarget();

  ASSERT_NEAR(GetParam().to<double>(), target.GetPitch(), 0.0001);
}

INSTANTIATE_TEST_SUITE_P(AnglesTests, SimVisionSystemTestWithParamsTest,
                         testing::Values(-10_deg, -5_deg, -0_deg, -1_deg,
                                         -2_deg, 5_deg, 7_deg, 10.23_deg,
                                         20.21_deg, -19.999_deg));

TEST_P(SimVisionSystemTestDistanceParamsTest, DistanceCalc) {
  units::foot_t distParam;
  units::degree_t pitchParam;
  units::foot_t heightParam;
  std::tie(distParam, pitchParam, heightParam) = GetParam();

  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_deg, 0_deg,
                      (units::constants::detail::PI_VAL * 0.98) * 1_rad}};
  frc::Pose3d robotPose{{15.98_m - distParam, 0_m, 0_m}, frc::Rotation3d{}};
  frc::Transform3d robotToCamera{frc::Translation3d{0_m, 0_m, heightParam},
                                 frc::Rotation3d{0_deg, pitchParam, 0_deg}};
  photonlib::SimVisionSystem sys{
      "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysoho"
      "wsyourdaygoingihopegoodhaveagreatrestofyourlife",
      160.0_deg,
      robotToCamera.Inverse(),
      99999_m,
      640,
      480,
      0};
  sys.AddSimVisionTarget(
      photonlib::SimVisionTarget{targetPose, 0.5_m, 0.5_m, 0});
  sys.ProcessFrame(robotPose);

  auto results = sys.cam.GetLatestResult();

  ASSERT_TRUE(results.HasTargets());

  photonlib::PhotonTrackedTarget target = results.GetBestTarget();

  ASSERT_NEAR(target.GetYaw(), 0.0, 0.0001);

  units::meter_t dist = photonlib::PhotonUtils::CalculateDistanceToTarget(
      robotToCamera.Z(), targetPose.Z(), pitchParam,
      units::degree_t{target.GetPitch()});
  ASSERT_NEAR(dist.to<double>(),
              distParam.convert<units::meters>().to<double>(), 0.001);
}

INSTANTIATE_TEST_SUITE_P(
    DistanceParamsTests, SimVisionSystemTestDistanceParamsTest,
    testing::Values(std::make_tuple(5_ft, 15.98_deg, 0_ft),
                    std::make_tuple(6_ft, 15.98_deg, 1_ft),
                    std::make_tuple(10_ft, 15.98_deg, 0_ft),
                    std::make_tuple(15_ft, 15.98_deg, 2_ft),
                    std::make_tuple(19.95_ft, 15.98_deg, 0_ft),
                    std::make_tuple(20_ft, 15.98_deg, 0_ft),
                    std::make_tuple(5_ft, 42_deg, 1_ft),
                    std::make_tuple(6_ft, 42_deg, 0_ft),
                    std::make_tuple(10_ft, 42_deg, 2_ft),
                    std::make_tuple(15_ft, 42_deg, 0.5_ft),
                    std::make_tuple(19.42_ft, 15.98_deg, 0_ft),
                    std::make_tuple(20_ft, 42_deg, 0_ft),
                    std::make_tuple(5_ft, 55_deg, 2_ft),
                    std::make_tuple(6_ft, 55_deg, 0_ft),
                    std::make_tuple(10_ft, 54_deg, 2.2_ft),
                    std::make_tuple(15_ft, 53_deg, 0_ft),
                    std::make_tuple(19.52_ft, 15.98_deg, 1.1_ft)));

TEST_F(SimVisionSystemTest, TestMultipleTargets) {
  const frc::Pose3d targetPoseL{
      {15.98_m, 2_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  const frc::Pose3d targetPoseC{
      {15.98_m, 0_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  const frc::Pose3d targetPoseR{
      {15.98_m, -2_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg, units::constants::detail::PI_VAL * 1_rad}};
  photonlib::SimVisionSystem sys{
      "Test", 160.0_deg, frc::Transform3d{}, 99999_m, 640, 480, 20.0};
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 1});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseC.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 2});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseR.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 3});

  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 1_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 4});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseC.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 1_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 5});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseR.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 1_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 6});

  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.5_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 7});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseC.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.5_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 8});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.75_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 9});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseR.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.75_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 10});
  sys.AddSimVisionTarget(photonlib::SimVisionTarget{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.25_m}, frc::Rotation3d{}}),
      0.25_m, 0.25_m, 11});

  frc::Pose2d robotPose{{6_m, 0_m}, frc::Rotation2d{.25_deg}};
  sys.ProcessFrame(robotPose);

  auto results = sys.cam.GetLatestResult();

  ASSERT_TRUE(results.HasTargets());

  auto targetList = results.GetTargets();

  ASSERT_EQ(targetList.size(), size_t(11));
}
