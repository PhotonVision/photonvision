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

#include "photon/simulation/VisionSystemSim.h"

#include <tuple>
#include <vector>

#include <catch2/catch_approx.hpp>
#include <catch2/catch_test_macros.hpp>
#include <catch2/generators/catch_generators_all.hpp>
#include <wpi/util/deprecated.hpp>

#include "photon/PhotonUtils.h"
#include "photon/estimation/VisionEstimation.h"

// Ignore GetLatestResult warnings
WPI_IGNORE_DEPRECATED

class VisionSystemSimTest {
 public:
  VisionSystemSimTest() {
    wpi::nt::NetworkTableInstance::GetDefault().StartServer();
    photon::PhotonCamera::SetVersionCheckEnabled(false);
  }
};

class VisionSystemSimTestWithParamsTest : public VisionSystemSimTest {};
class VisionSystemSimTestDistanceParamsTest : public VisionSystemSimTest {};

TEST_CASE_METHOD(VisionSystemSimTest, "TestVisibilityCupidShuffle",
                 "[photonlib]") {
  wpi::math::Pose3d targetPose{
      wpi::math::Translation3d{15.98_m, 0_m, 2_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{1.0_m, 1.0_m}, 3}});

  // To the right, to the right
  wpi::math::Pose2d robotPose{wpi::math::Translation2d{5_m, 0_m},
                              wpi::math::Rotation2d{-70_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());

  // To the right, to the right
  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{5_m, 0_m},
                                wpi::math::Rotation2d{-95_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());

  // To the left, to the left
  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{5_m, 0_m},
                                wpi::math::Rotation2d{90_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());

  // To the left, to the left
  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{5_m, 0_m},
                                wpi::math::Rotation2d{65_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());

  // Now kick, now kick
  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{2_m, 0_m},
                                wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());

  // Now kick, now kick
  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{2_m, 0_m},
                                wpi::math::Rotation2d{-5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());

  // Now walk it by yourself
  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{2_m, 0_m},
                                wpi::math::Rotation2d{-179_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());

  // Now walk it by yourself
  visionSysSim.AdjustCamera(
      &cameraSim,
      wpi::math::Transform3d{
          wpi::math::Translation3d{},
          wpi::math::Rotation3d{0_deg, 0_deg,
                                wpi::units::radian_t{std::numbers::pi}}});
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestBunchaTargets", "[photonlib]") {
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});

  std::vector<photon::VisionTargetSim> targets;
  for (int i = 0; i < 100; i++) {
    targets.emplace_back(
        wpi::math::Pose3d{
            wpi::math::Translation3d{15.98_m + i * 0.1_m, 0_m, 1_m},
            wpi::math::Rotation3d{0_rad, 0_rad,
                                  wpi::units::radian_t{std::numbers::pi}}},
        photon::TargetModel{0.5_m, 0.5_m}, i);
  }
  visionSysSim.AddVisionTargets(targets);

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{5_m, 0_m},
                              wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);

  REQUIRE(camera.GetLatestResult().targets.size() == 50u);
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestNotVisibleVert1", "[photonlib]") {
  wpi::math::Pose3d targetPose{
      wpi::math::Translation3d{15.98_m, 0_m, 1_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{3.0_m, 3.0_m}, 3}});

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{5_m, 0_m},
                              wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());

  visionSysSim.AdjustCamera(
      &cameraSim,
      wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 5000_m},
          wpi::math::Rotation3d{0_deg, 0_deg,
                                wpi::units::radian_t{std::numbers::pi}}});
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestNotVisibleVert2", "[photonlib]") {
  wpi::math::Pose3d targetPose{
      wpi::math::Translation3d{15.98_m, 0_m, 2_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};

  wpi::math::Transform3d robotToCamera{
      wpi::math::Translation3d{0_m, 0_m, 1_m},
      wpi::math::Rotation3d{0_rad, wpi::units::radian_t{-std::numbers::pi / 4},
                            0_rad}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, robotToCamera);
  cameraSim.prop.SetCalibration(1234, 1234, wpi::math::Rotation2d{80_deg});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 1736}});

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{13.98_m, 0_m},
                              wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());

  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{0_m, 0_m},
                                wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestNotVisibleTargetSize",
                 "[photonlib]") {
  wpi::math::Pose3d targetPose{
      wpi::math::Translation3d{15.98_m, 0_m, 1_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.1_m, 0.1_m}, 24}});

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{12_m, 0_m},
                              wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());

  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{0_m, 0_m},
                                wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestNotVisibleTooFarLeds",
                 "[photonlib]") {
  wpi::math::Pose3d targetPose{
      wpi::math::Translation3d{15.98_m, 0_m, 1_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(1.0);
  cameraSim.SetMaxSightRange(10_m);
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{targetPose, photon::TargetModel{1_m, 1_m}, 25}});

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{10_m, 0_m},
                              wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE(camera.GetLatestResult().HasTargets());

  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{0_m, 0_m},
                                wpi::math::Rotation2d{5_deg}};
  visionSysSim.Update(robotPose);
  REQUIRE_FALSE(camera.GetLatestResult().HasTargets());
}

TEST_CASE_METHOD(VisionSystemSimTestWithParamsTest, "YawAngles",
                 "[photonlib]") {
  const wpi::math::Pose3d targetPose{
      {15.98_m, 0_m, 0_m},
      wpi::math::Rotation3d{0_deg, 0_deg,
                            wpi::units::radian_t{3 * std::numbers::pi / 4}}};
  auto param = GENERATE(-10_deg, -5_deg, -0_deg, -1_deg, -2_deg, 5_deg, 7_deg,
                        10.23_deg);

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(0.0);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 3}});

  // If the robot is rotated x deg (CCW+), the target yaw should be x deg (CW+)
  wpi::math::Pose2d robotPose{wpi::math::Translation2d{10_m, 0_m},
                              wpi::math::Rotation2d{param}};
  visionSysSim.Update(robotPose);

  const auto result = camera.GetLatestResult();
  REQUIRE(result.HasTargets());
  REQUIRE(param.to<double>() ==
          Catch::Approx(result.GetBestTarget().GetYaw()).margin(0.25));
}

TEST_CASE_METHOD(VisionSystemSimTestWithParamsTest, "PitchAngles",
                 "[photonlib]") {
  const wpi::math::Pose3d targetPose{
      {15.98_m, 0_m, 0_m},
      wpi::math::Rotation3d{0_deg, 0_deg,
                            wpi::units::radian_t{3 * std::numbers::pi / 4}}};
  auto param = GENERATE(-10_deg, -5_deg, -0_deg, -1_deg, -2_deg, 5_deg, 7_deg,
                        10.23_deg);

  wpi::math::Pose2d robotPose{{10_m, 0_m}, wpi::math::Rotation2d{param * -1.0}};
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{120_deg});
  cameraSim.SetMinTargetAreaPixels(0.0);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 3}});

  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{10_m, 0_m},
                                wpi::math::Rotation2d{-1 * param}};
  visionSysSim.AdjustCamera(
      &cameraSim,
      wpi::math::Transform3d{
          wpi::math::Translation3d{},
          wpi::math::Rotation3d{0_rad, wpi::units::degree_t{param}, 0_rad}});
  visionSysSim.Update(robotPose);

  const auto result = camera.GetLatestResult();
  REQUIRE(result.HasTargets());
  REQUIRE(param.to<double>() ==
          Catch::Approx(result.GetBestTarget().GetPitch()).margin(0.25));
}

TEST_CASE_METHOD(VisionSystemSimTestDistanceParamsTest, "DistanceCalc",
                 "[photonlib]") {
  auto [distParam, pitchParam, heightParam] =
      GENERATE(std::make_tuple(5_ft, -15.98_deg, 0_ft),
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
               std::make_tuple(19.52_ft, -15.98_deg, 1.1_ft));

  const wpi::math::Pose3d targetPose{
      {15.98_m, 0_m, 1_m},
      wpi::math::Rotation3d{0_deg, 0_deg,
                            wpi::units::radian_t{std::numbers::pi * 0.98}}};
  wpi::math::Pose3d robotPose{{15.98_m - distParam, 0_m, 0_m},
                              wpi::math::Rotation3d{}};
  wpi::math::Transform3d robotToCamera{
      wpi::math::Translation3d{0_m, 0_m, heightParam},
      wpi::math::Rotation3d{0_deg, pitchParam, 0_deg}};
  photon::VisionSystemSim visionSysSim{
      "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysoho"
      "wsyourdaygoingihopegoodhaveagreatrestofyourlife"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{160_deg});
  cameraSim.SetMinTargetAreaPixels(0.0);
  visionSysSim.AdjustCamera(&cameraSim, robotToCamera);
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPose, photon::TargetModel{0.5_m, 0.5_m}, 0}});
  visionSysSim.Update(robotPose);

  photon::PhotonPipelineResult res = camera.GetLatestResult();
  REQUIRE(res.HasTargets());
  photon::PhotonTrackedTarget target = res.GetBestTarget();

  REQUIRE(0.0 == Catch::Approx(target.GetYaw()).margin(0.5));

  wpi::units::meter_t dist = photon::PhotonUtils::CalculateDistanceToTarget(
      robotToCamera.Z(), targetPose.Z(), -pitchParam,
      wpi::units::degree_t{target.GetPitch()});
  REQUIRE(dist.to<double>() ==
          Catch::Approx(distParam.convert<wpi::units::meters>().to<double>())
              .margin(0.25));
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestMultipleTargets", "[photonlib]") {
  wpi::math::Pose3d targetPoseL{
      wpi::math::Translation3d{15.98_m, 2_m, 0_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};
  wpi::math::Pose3d targetPoseC{
      wpi::math::Translation3d{15.98_m, 0_m, 0_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};
  wpi::math::Pose3d targetPoseR{
      wpi::math::Translation3d{15.98_m, -2_m, 0_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);

  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 1}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseC.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 2}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseR.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 3}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 1_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 4}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseC.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 1_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 5}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseR.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 1_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 6}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0.5_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 7}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseC.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0.5_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 8}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0.75_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 9}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseR.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0.75_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 10}});
  visionSysSim.AddVisionTargets({photon::VisionTargetSim{
      targetPoseL.TransformBy(wpi::math::Transform3d{
          wpi::math::Translation3d{0_m, 0_m, 0.25_m}, wpi::math::Rotation3d{}}),
      photon::kAprilTag16h5, 11}});

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{6_m, 0_m},
                              wpi::math::Rotation2d{.25_deg}};
  visionSysSim.Update(robotPose);
  photon::PhotonPipelineResult res = camera.GetLatestResult();
  REQUIRE(res.HasTargets());
  std::span<const photon::PhotonTrackedTarget> tgtList = res.GetTargets();
  REQUIRE(static_cast<size_t>(11) == tgtList.size());
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestPoseEstimation", "[photonlib]") {
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{90_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);

  std::vector<wpi::apriltag::AprilTag> tagList;
  tagList.emplace_back(wpi::apriltag::AprilTag{
      0, wpi::math::Pose3d{
             12_m, 3_m, 1_m,
             wpi::math::Rotation3d{0_rad, 0_rad,
                                   wpi::units::radian_t{std::numbers::pi}}}});
  tagList.emplace_back(wpi::apriltag::AprilTag{
      1, wpi::math::Pose3d{
             12_m, 1_m, -1_m,
             wpi::math::Rotation3d{0_rad, 0_rad,
                                   wpi::units::radian_t{std::numbers::pi}}}});
  tagList.emplace_back(wpi::apriltag::AprilTag{
      2, wpi::math::Pose3d{
             11_m, 0_m, 2_m,
             wpi::math::Rotation3d{0_rad, 0_rad,
                                   wpi::units::radian_t{std::numbers::pi}}}});
  wpi::units::meter_t fieldLength{54};
  wpi::units::meter_t fieldWidth{27};
  wpi::apriltag::AprilTagFieldLayout layout{tagList, fieldLength, fieldWidth};
  wpi::math::Pose2d robotPose{wpi::math::Translation2d{5_m, 1_m},
                              wpi::math::Rotation2d{5_deg}};
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
  auto results = photon::VisionEstimation::EstimateCamPosePNP(
      camEigen, distEigen, targets, layout, photon::kAprilTag16h5);
  REQUIRE(results);
  wpi::math::Pose3d pose = wpi::math::Pose3d{} + results->best;
  REQUIRE(5 == Catch::Approx(pose.X().to<double>()).margin(0.01));
  REQUIRE(1 == Catch::Approx(pose.Y().to<double>()).margin(0.01));
  REQUIRE(0 == Catch::Approx(pose.Z().to<double>()).margin(0.01));
  REQUIRE(wpi::units::degree_t{5}.convert<wpi::units::radians>().to<double>() ==
          Catch::Approx(pose.Rotation().Z().to<double>()).margin(0.01));

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
  auto results2 = photon::VisionEstimation::EstimateCamPosePNP(
      camEigen, distEigen, targets2, layout, photon::kAprilTag16h5);
  REQUIRE(results2);
  wpi::math::Pose3d pose2 = wpi::math::Pose3d{} + results2->best;
  REQUIRE(robotPose.X().to<double>() ==
          Catch::Approx(pose2.X().to<double>()).margin(0.01));
  REQUIRE(robotPose.Y().to<double>() ==
          Catch::Approx(pose2.Y().to<double>()).margin(0.01));
  REQUIRE(0 == Catch::Approx(pose2.Z().to<double>()).margin(0.01));
  REQUIRE(wpi::units::degree_t{5}.convert<wpi::units::radians>().to<double>() ==
          Catch::Approx(pose2.Rotation().Z().to<double>()).margin(0.01));
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestPoseEstimationRotated",
                 "[photonlib]") {
  wpi::math::Transform3d robotToCamera{
      wpi::math::Translation3d{6_in, 6_in, 6_in},
      wpi::math::Rotation3d{0_deg, -30_deg, 25.5_deg}};

  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"cameraRotated"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, robotToCamera);
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{90_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);

  std::vector<wpi::apriltag::AprilTag> tagList;
  tagList.emplace_back(wpi::apriltag::AprilTag{
      0, wpi::math::Pose3d{
             12_m, 3_m, 1_m,
             wpi::math::Rotation3d{0_rad, 0_rad,
                                   wpi::units::radian_t{std::numbers::pi}}}});
  tagList.emplace_back(wpi::apriltag::AprilTag{
      1, wpi::math::Pose3d{
             12_m, 1_m, -1_m,
             wpi::math::Rotation3d{0_rad, 0_rad,
                                   wpi::units::radian_t{std::numbers::pi}}}});
  tagList.emplace_back(wpi::apriltag::AprilTag{
      2, wpi::math::Pose3d{
             11_m, 0_m, 2_m,
             wpi::math::Rotation3d{0_rad, 0_rad,
                                   wpi::units::radian_t{std::numbers::pi}}}});
  wpi::units::meter_t fieldLength{54};
  wpi::units::meter_t fieldWidth{27};
  wpi::apriltag::AprilTagFieldLayout layout{tagList, fieldLength, fieldWidth};
  wpi::math::Pose2d robotPose{wpi::math::Translation2d{5_m, 1_m},
                              wpi::math::Rotation2d{-5_deg}};
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{tagList[0].pose, photon::kAprilTag36h11, 0}});
  visionSysSim.Update(robotPose);

  Eigen::Matrix<double, 3, 3> camEigen = camera.GetCameraMatrix().value();
  Eigen::Matrix<double, 8, 1> distEigen = camera.GetDistCoeffs().value();

  auto camResults = camera.GetLatestResult();
  auto targetSpan = camResults.GetTargets();

  // We need to see at least one target
  REQUIRE(targetSpan.size() > static_cast<size_t>(0));

  std::vector<photon::PhotonTrackedTarget> targets;
  for (photon::PhotonTrackedTarget tar : targetSpan) {
    targets.push_back(tar);
  }
  auto results = photon::VisionEstimation::EstimateCamPosePNP(
      camEigen, distEigen, targets, layout, photon::kAprilTag36h11);
  REQUIRE(results);
  wpi::math::Pose3d pose = wpi::math::Pose3d{} + results->best;
  pose = pose.TransformBy(robotToCamera.Inverse());
  REQUIRE(5 == Catch::Approx(pose.X().to<double>()).margin(0.01));
  REQUIRE(1 == Catch::Approx(pose.Y().to<double>()).margin(0.01));
  REQUIRE(0 == Catch::Approx(pose.Z().to<double>()).margin(0.01));
  REQUIRE(
      wpi::units::degree_t{-5}.convert<wpi::units::radians>().to<double>() ==
      Catch::Approx(pose.Rotation().Z().to<double>()).margin(0.01));

  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{tagList[1].pose, photon::kAprilTag36h11, 1}});
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{tagList[2].pose, photon::kAprilTag36h11, 2}});
  visionSysSim.Update(robotPose);

  auto camResults2 = camera.GetLatestResult();
  auto targetSpan2 = camResults2.GetTargets();
  std::vector<photon::PhotonTrackedTarget> targets2;
  for (photon::PhotonTrackedTarget tar : targetSpan2) {
    targets2.push_back(tar);
  }
  auto results2 = photon::VisionEstimation::EstimateCamPosePNP(
      camEigen, distEigen, targets2, layout, photon::kAprilTag36h11);
  REQUIRE(results2);
  wpi::math::Pose3d pose2 = wpi::math::Pose3d{} + results2->best;
  pose2 = pose2.TransformBy(robotToCamera.Inverse());
  REQUIRE(robotPose.X().to<double>() ==
          Catch::Approx(pose2.X().to<double>()).margin(0.01));
  REQUIRE(robotPose.Y().to<double>() ==
          Catch::Approx(pose2.Y().to<double>()).margin(0.01));
  REQUIRE(0 == Catch::Approx(pose2.Z().to<double>()).margin(0.01));
  REQUIRE(
      wpi::units::degree_t{-5}.convert<wpi::units::radians>().to<double>() ==
      Catch::Approx(pose2.Rotation().Z().to<double>()).margin(0.01));
}

TEST_CASE_METHOD(VisionSystemSimTest, "TestTagAmbiguity", "[photonlib]") {
  photon::VisionSystemSim visionSysSim{"Test"};
  photon::PhotonCamera camera{"camera"};
  photon::PhotonCameraSim cameraSim{&camera};
  visionSysSim.AddCamera(&cameraSim, wpi::math::Transform3d{});
  cameraSim.prop.SetCalibration(640, 480, wpi::math::Rotation2d{80_deg});
  cameraSim.SetMinTargetAreaPixels(20.0);

  wpi::math::Pose3d targetPose{
      wpi::math::Translation3d{2_m, 0_m, 0_m},
      wpi::math::Rotation3d{0_rad, 0_rad,
                            wpi::units::radian_t{std::numbers::pi}}};
  visionSysSim.AddVisionTargets(
      {photon::VisionTargetSim{targetPose, photon::kAprilTag36h11, 3}});

  wpi::math::Pose2d robotPose{wpi::math::Translation2d{0_m, 0_m},
                              wpi::math::Rotation2d{0_deg}};
  visionSysSim.Update(robotPose);
  double ambiguity =
      camera.GetLatestResult().GetBestTarget().GetPoseAmbiguity();
  REQUIRE(ambiguity > 0.5);

  robotPose = wpi::math::Pose2d{wpi::math::Translation2d{-2_m, -2_m},
                                wpi::math::Rotation2d{30_deg}};
  visionSysSim.Update(robotPose);
  ambiguity = camera.GetLatestResult().GetBestTarget().GetPoseAmbiguity();
  REQUIRE((0 < ambiguity && ambiguity < 0.2));
}
