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

#include <chrono>
#include <thread>

#include "gtest/gtest.h"
#include "photon/PhotonUtils.h"
#include "photon/simulation/VisionSystemSim.h"

class VisionSystemSimTest : public ::testing::Test {
  void SetUp() override {
    nt::NetworkTableInstance::GetDefault().StartServer();
    photon::PhotonCamera::SetVersionCheckEnabled(false);
  }

  void TearDown() override {}
};

class VisionSystemSimTestWithParamsTest
    : public VisionSystemSimTest,
      public testing::WithParamInterface<units::degree_t> {};
class VisionSystemSimTestDistanceParamsTest
    : public VisionSystemSimTest,
      public testing::WithParamInterface<
          std::tuple<units::foot_t, units::degree_t, units::foot_t>> {};

TEST_F(VisionSystemSimTest, TestVisibilityCupidShuffle) {
  frc::Pose3d targetPose{
      frc::Translation3d{15.98_m, 0_m, 2_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{80_deg});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{1.0_m, 1.0_m}, 3}});

  // To the right, to the right
  frc::Pose2d robotPose{frc::Translation2d{5_m, 0_m}, frc::Rotation2d{-70_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());

  // To the right, to the right
  robotPose =
      frc::Pose2d{frc::Translation2d{5_m, 0_m}, frc::Rotation2d{-95_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());

  // To the left, to the left
  robotPose =
      frc::Pose2d{frc::Translation2d{5_m, 0_m}, frc::Rotation2d{90_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());

  // To the left, to the left
  robotPose =
      frc::Pose2d{frc::Translation2d{5_m, 0_m}, frc::Rotation2d{65_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());

  // Now kick, now kick
  robotPose = frc::Pose2d{frc::Translation2d{2_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());

  // Now kick, now kick
  robotPose =
      frc::Pose2d{frc::Translation2d{2_m, 0_m}, frc::Rotation2d{-5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());

  // Now walk it by yourself
  robotPose =
      frc::Pose2d{frc::Translation2d{2_m, 0_m}, frc::Rotation2d{-179_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());

  // Now walk it by yourself
  visionSysSim.AdjustCamera(
      &cameraSim,
      frc::Transform3d{
          frc::Translation3d{},
          frc::Rotation3d{0_deg, 0_deg, units::radian_t{std::numbers::pi}}});
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());
}

TEST_F(VisionSystemSimTest, TestNotVisibleVert1) {
  frc::Pose3d targetPose{
      frc::Translation3d{15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{80_deg});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{3.0_m, 3.0_m}, 3}});

  frc::Pose2d robotPose{frc::Translation2d{5_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());

  visionSysSim.AdjustCamera(
      &cameraSim,
      frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 5000_m},
          frc::Rotation3d{0_deg, 0_deg, units::radian_t{std::numbers::pi}}});
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_F(VisionSystemSimTest, TestNotVisibleVert2) {
  frc::Pose3d targetPose{
      frc::Translation3d{15.98_m, 0_m, 2_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};

  frc::Transform3d robotToCamera{
      frc::Translation3d{0_m, 0_m, 1_m},
      frc::Rotation3d{0_rad, units::radian_t{-std::numbers::pi / 4}, 0_rad}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, robotToCamera);
  cameraSim.prop.SetCalibration(1234, 1234, frc::Rotation2d{80_deg});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 1736}});

  frc::Pose2d robotPose{frc::Translation2d{13.98_m, 0_m},
                        frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());

  robotPose = frc::Pose2d{frc::Translation2d{0_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_F(VisionSystemSimTest, TestNotVisibleTargetSize) {
  frc::Pose3d targetPose{
      frc::Translation3d{15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.1_m, 0.1_m}, 24}});

  frc::Pose2d robotPose{frc::Translation2d{12_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());

  robotPose = frc::Pose2d{frc::Translation2d{0_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_F(VisionSystemSimTest, TestNotVisibleTooFarLeds) {
  frc::Pose3d targetPose{
      frc::Translation3d{15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(1.0);
  cameraSim.SetMaxSightRange(10_m);
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{targetPose, photon::TargetModel{1_m, 1_m}, 25}});

  frc::Pose2d robotPose{frc::Translation2d{10_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());

  robotPose = frc::Pose2d{frc::Translation2d{0_m, 0_m}, frc::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  ASSERT_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_P(VisionSystemSimTestWithParamsTest, YawAngles) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg, units::radian_t{3 * std::numbers::pi / 4}}};
  frc::Pose2d robotPose{{10_m, 0_m}, frc::Rotation2d{GetParam() * -1.0}};
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(0.0);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 3}});

  // If the robot is rotated x deg (CCW+), the target yaw should be x deg (CW+)
  robotPose =
      frc::Pose2d{frc::Translation2d{10_m, 0_m}, frc::Rotation2d{GetParam()}};
  visionSysSim.Update(robotPose);
  ASSERT_TRUE(camera.GetLatestResult().HasTargets());
  ASSERT_NEAR(GetParam().to<double>(),
              camera.GetLatestResult().GetBestTarget().GetYaw(), 0.25);
}

TEST_P(VisionSystemSimTestWithParamsTest, PitchAngles) {
  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 0_m},
      frc::Rotation3d{0_deg, 0_deg, units::radian_t{3 * std::numbers::pi / 4}}};
  frc::Pose2d robotPose{{10_m, 0_m}, frc::Rotation2d{GetParam() * -1.0}};
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{120_deg});
  cameraSim.SetMinTargetAreaPixels(0.0);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 3}});

  robotPose = frc::Pose2d{frc::Translation2d{10_m, 0_m},
                          frc::Rotation2d{-1 * GetParam()}};
  visionSysSim.AdjustCamera(
      &cameraSim,
      frc::Transform3d{
          frc::Translation3d{},
          frc::Rotation3d{0_rad, units::degree_t{GetParam()}, 0_rad}});
  visionSysSim.Update(robotPose);

  ASSERT_TRUE(camera.GetLatestResult().HasTargets());
  ASSERT_NEAR(GetParam().to<double>(),
              camera.GetLatestResult().GetBestTarget().GetPitch(), 0.25);
}

INSTANTIATE_TEST_SUITE_P(AnglesTests, VisionSystemSimTestWithParamsTest,
                         testing::Values(-10_deg, -5_deg, -0_deg, -1_deg,
                                         -2_deg, 5_deg, 7_deg, 10.23_deg));

TEST_P(VisionSystemSimTestDistanceParamsTest, DistanceCalc) {
  units::foot_t distParam;
  units::degree_t pitchParam;
  units::foot_t heightParam;
  std::tie(distParam, pitchParam, heightParam) = GetParam();

  const frc::Pose3d targetPose{
      {15.98_m, 0_m, 1_m},
      frc::Rotation3d{0_deg, 0_deg, units::radian_t{std::numbers::pi * 0.98}}};
  frc::Pose3d robotPose{{15.98_m - distParam, 0_m, 0_m}, frc::Rotation3d{}};
  frc::Transform3d robotToCamera{frc::Translation3d{0_m, 0_m, heightParam},
                                 frc::Rotation3d{0_deg, pitchParam, 0_deg}};
  photon::VisionSystemSim visionSysSim{
      "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysoho"
      "wsyourdaygoingihopegoodhaveagreatrestofyourlife"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{160_deg});
  cameraSim.SetMinTargetAreaPixels(0.0);
  visionSysSim.AdjustCamera(&cameraSim, robotToCamera);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 0}});
  visionSysSim.Update(robotPose);

  photon::PhotonPipelineResult res = camera.GetLatestResult();
  ASSERT_TRUE(res.HasTargets());
  photon::PhotonTrackedTarget target = res.GetBestTarget();

  ASSERT_NEAR(0.0, target.GetYaw(), 0.5);

  units::meter_t dist = photon::PhotonUtils::CalculateDistanceToTarget(
      robotToCamera.Z(), targetPose.Z(), -pitchParam,
      units::degree_t{target.GetPitch()});
  ASSERT_NEAR(dist.to<double>(),
              distParam.convert<units::meters>().to<double>(), 0.25);
}

INSTANTIATE_TEST_SUITE_P(
    DistanceParamsTests, VisionSystemSimTestDistanceParamsTest,
    testing::Values(std::make_tuple(5_ft, -15.98_deg, 0_ft),
                    std::make_tuple(6_ft, -15.98_deg, 1_ft),
                    std::make_tuple(10_ft, -15.98_deg, 0_ft),
                    std::make_tuple(15_ft, -15.98_deg, 2_ft),
                    std::make_tuple(19.95_ft, -15.98_deg, 0_ft),
                    std::make_tuple(20_ft, -15.98_deg, 0_ft),
                    std::make_tuple(5_ft, -42_deg, 1_ft),
                    std::make_tuple(6_ft, -42_deg, 0_ft),
                    std::make_tuple(10_ft, -42_deg, 2_ft),
                    std::make_tuple(15_ft, -42_deg, 0.5_ft),
                    std::make_tuple(19.42_ft, -15.98_deg, 0_ft),
                    std::make_tuple(20_ft, -42_deg, 0_ft),
                    std::make_tuple(5_ft, -55_deg, 2_ft),
                    std::make_tuple(6_ft, -55_deg, 0_ft),
                    std::make_tuple(10_ft, -54_deg, 2.2_ft),
                    std::make_tuple(15_ft, -53_deg, 0_ft),
                    std::make_tuple(19.52_ft, -15.98_deg, 1.1_ft)));

TEST_F(VisionSystemSimTest, TestMultipleTargets) {
  frc::Pose3d targetPoseL{
      frc::Translation3d{15.98_m, 2_m, 0_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};
  frc::Pose3d targetPoseC{
      frc::Translation3d{15.98_m, 0_m, 0_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};
  frc::Pose3d targetPoseR{
      frc::Translation3d{15.98_m, -2_m, 0_m},
      frc::Rotation3d{0_rad, 0_rad, units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);

  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 1}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseC.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 2}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseR.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 3}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 1_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 4}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseC.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 1_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 5}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseR.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 1_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 6}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.5_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 7}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseC.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.5_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 8}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.75_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 9}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseR.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.75_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 10}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(frc::Transform3d{
          frc::Translation3d{0_m, 0_m, 0.25_m}, frc::Rotation3d{}}),
      photon::kAprilTag16h5, 11}});

  frc::Pose2d robotPose{frc::Translation2d{6_m, 0_m}, frc::Rotation2d{.25_deg}};
  visionSysSim.Update(robotPose);
  photon::PhotonPipelineResult res = camera.GetLatestResult();
  ASSERT_TRUE(res.HasTargets());
  std::span<const photon::PhotonTrackedTarget> tgtList = res.GetTargets();
  ASSERT_EQ(static_cast<size_t>(11), tgtList.size());
}

TEST_F(VisionSystemSimTest, TestPoseEstimation) {
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, frc::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, frc::Rotation2d{90_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);

  std::vector<frc::AprilTag> tagList;
  tagList.emplace_back(frc::AprilTag{
      0, frc::Pose3d{12_m, 3_m, 1_m,
                     frc::Rotation3d{0_rad, 0_rad,
                                     units::radian_t{std::numbers::pi}}}});
  tagList.emplace_back(frc::AprilTag{
      1, frc::Pose3d{12_m, 1_m, -1_m,
                     frc::Rotation3d{0_rad, 0_rad,
                                     units::radian_t{std::numbers::pi}}}});
  tagList.emplace_back(frc::AprilTag{
      2, frc::Pose3d{11_m, 0_m, 2_m,
                     frc::Rotation3d{0_rad, 0_rad,
                                     units::radian_t{std::numbers::pi}}}});
  units::meter_t fieldLength{54};
  units::meter_t fieldWidth{27};
  frc::AprilTagFieldLayout layout{tagList, fieldLength, fieldWidth};
  frc::Pose2d robotPose{frc::Translation2d{5_m, 1_m}, frc::Rotation2d{5_deg}};
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{tagList[0].pose, photon::kAprilTag16h5, 0}});
  visionSysSim.Update(robotPose);

  Eigen::Matrix<double, 3, 3> camEigen = camera.GetCameraMatrix().value();
  Eigen::Matrix<double, 8, 1> distEigen = camera.GetDistCoeffs().value();

  auto camResults = camera.GetLatestResult();
  auto targetSpan = camResults.GetTargets();
  std::vector<photon::PhotonTrackedTarget> targets;
  for (photon::PhotonTrackedTarget tar : targetSpan) {
    targets.push_back(tar);
  }
  photon::PnpResult results = photon::VisionEstimation::EstimateCamPosePNP(
      camEigen, distEigen, targets, layout, photon::kAprilTag16h5);
  frc::Pose3d pose = frc::Pose3d{} + results.best;
  ASSERT_NEAR(5, pose.X().to<double>(), 0.01);
  ASSERT_NEAR(1, pose.Y().to<double>(), 0.01);
  ASSERT_NEAR(0, pose.Z().to<double>(), 0.01);
  ASSERT_NEAR(units::degree_t{5}.convert<units::radians>().to<double>(),
              pose.Rotation().Z().to<double>(), 0.01);

  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{tagList[1].pose, photon::kAprilTag16h5, 1}});
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{tagList[2].pose, photon::kAprilTag16h5, 2}});
  visionSysSim.Update(robotPose);

  auto camResults2 = camera.GetLatestResult();
  auto targetSpan2 = camResults2.GetTargets();
  std::vector<photon::PhotonTrackedTarget> targets2;
  for (photon::PhotonTrackedTarget tar : targetSpan2) {
    targets2.push_back(tar);
  }
  photon::PnpResult results2 = photon::VisionEstimation::EstimateCamPosePNP(
      camEigen, distEigen, targets2, layout, photon::kAprilTag16h5);
  frc::Pose3d pose2 = frc::Pose3d{} + results2.best;
  ASSERT_NEAR(5, pose2.X().to<double>(), 0.01);
  ASSERT_NEAR(1, pose2.Y().to<double>(), 0.01);
  ASSERT_NEAR(0, pose2.Z().to<double>(), 0.01);
  ASSERT_NEAR(units::degree_t{5}.convert<units::radians>().to<double>(),
              pose2.Rotation().Z().to<double>(), 0.01);
}
