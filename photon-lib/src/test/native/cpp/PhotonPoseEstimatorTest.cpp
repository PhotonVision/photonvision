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

#include "photon/PhotonPoseEstimator.h"

#include <utility>
#include <vector>

#include <gtest/gtest.h>
#include <wpi/apriltag/AprilTagFieldLayout.hpp>
#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Rotation3d.hpp>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/units/angle.hpp>
#include <wpi/units/length.hpp>
#include <wpi/util/SmallVector.hpp>

#include "photon/PhotonCamera.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/simulation/PhotonCameraSim.h"
#include "photon/simulation/SimCameraProperties.h"
#include "photon/simulation/VisionTargetSim.h"
#include "photon/targeting/MultiTargetPNPResult.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"
#include "photon/targeting/PnpResult.h"

static std::vector<wpi::apriltag::AprilTag> tags = {
    {0, wpi::math::Pose3d(wpi::units::meter_t(3), wpi::units::meter_t(3),
                          wpi::units::meter_t(3), wpi::math::Rotation3d())},
    {1, wpi::math::Pose3d(wpi::units::meter_t(5), wpi::units::meter_t(5),
                          wpi::units::meter_t(5), wpi::math::Rotation3d())}};

static wpi::apriltag::AprilTagFieldLayout aprilTags{tags, 54_ft, 27_ft};

static std::vector<photon::TargetCorner> corners{
    photon::TargetCorner{1., 2.}, photon::TargetCorner{3., 4.},
    photon::TargetCorner{5., 6.}, photon::TargetCorner{7., 8.}};
static std::vector<photon::TargetCorner> detectedCorners{
    photon::TargetCorner{1., 2.}, photon::TargetCorner{3., 4.},
    photon::TargetCorner{5., 6.}, photon::TargetCorner{7., 8.}};

TEST(PhotonPoseEstimatorTest, LowestAmbiguityStrategy) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(1_rad, 2_rad, 3_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(4_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(4_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(1_rad, 2_rad, 3_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(11));

  photon::PhotonPoseEstimator estimator(aprilTags, wpi::math::Transform3d{});

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateLowestAmbiguityPose(result);
  }
  ASSERT_TRUE(estimatedPose);
  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(
      11, wpi::units::unit_cast<double>(estimatedPose.value().timestamp), .02);
  EXPECT_NEAR(1, wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(3, wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2, wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, ClosestToCameraHeightStrategy) {
  std::vector<wpi::apriltag::AprilTag> tags = {
      {0, wpi::math::Pose3d(wpi::units::meter_t(3), wpi::units::meter_t(3),
                            wpi::units::meter_t(3), wpi::math::Rotation3d())},
      {1, wpi::math::Pose3d(wpi::units::meter_t(5), wpi::units::meter_t(5),
                            wpi::units::meter_t(5), wpi::math::Rotation3d())},
  };
  auto aprilTags = wpi::apriltag::AprilTagFieldLayout(tags, 54_ft, 27_ft);

  std::vector<std::pair<photon::PhotonCamera, wpi::math::Transform3d>> cameras;

  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");

  // ID 0 at 3,3,3
  // ID 1 at 5,5,5

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(0_m, 0_m, 0_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 1_m, 1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 2_m, 2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(3_m, 3_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(4_m, 4_m, 4_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(5_m, 5_m, 5_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(17_s);

  photon::PhotonPoseEstimator estimator(aprilTags, {{0_m, 0_m, 4_m}, {}});

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateClosestToCameraHeightPose(result);
  }
  ASSERT_TRUE(estimatedPose);

  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(
      17, wpi::units::unit_cast<double>(estimatedPose.value().timestamp), .02);
  EXPECT_NEAR(4, wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(4, wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(0, wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, ClosestToReferencePoseStrategy) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(0_m, 0_m, 0_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 1_m, 1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 2_m, 2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(3_m, 3_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2.2_m, 2.2_m, 2.2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 1.9_m, 2.1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(17));

  photon::PhotonPoseEstimator estimator(aprilTags, {});

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateClosestToReferencePose(
        result, wpi::math::Pose3d(1_m, 1_m, 1_m,
                                  wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)));
  }

  ASSERT_TRUE(estimatedPose);
  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(
      17, wpi::units::unit_cast<double>(estimatedPose.value().timestamp), .01);
  EXPECT_NEAR(1, wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(1.1, wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(.9, wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, ClosestToLastPose) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(0_m, 0_m, 0_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 1_m, 1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 2_m, 2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(3_m, 3_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2.2_m, 2.2_m, 2.2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 1.9_m, 2.1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(17));

  photon::PhotonPoseEstimator estimator(aprilTags, {});

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateClosestToReferencePose(
        result, wpi::math::Pose3d(1_m, 1_m, 1_m,
                                  wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)));
  }

  ASSERT_TRUE(estimatedPose);
  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  std::vector<photon::PhotonTrackedTarget> targetsThree{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(0_m, 0_m, 0_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 1_m, 1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2.1_m, 1.9_m, 2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(3_m, 3_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2.4_m, 2.4_m, 2.2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 1_m, 2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targetsThree,
      std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(21));

  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateClosestToReferencePose(result, pose);
  }

  ASSERT_TRUE(estimatedPose);
  pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(21.0,
              wpi::units::unit_cast<double>(estimatedPose.value().timestamp),
              .01);
  EXPECT_NEAR(.9, wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(1.1, wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(1, wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, PnpDistanceTrigSolve) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");
  cameraOne.test = true;

  std::vector<photon::VisionTargetSim> targets;
  targets.reserve(tags.size());
  for (const auto& tag : tags) {
    targets.push_back(
        photon::VisionTargetSim(tag.pose, photon::kAprilTag36h11, tag.ID));
  }
  photon::PhotonCameraSim cameraOneSim = photon::PhotonCameraSim(
      &cameraOne, photon::SimCameraProperties::PERFECT_90DEG());

  /* Compound Rolled + Pitched + Yaw */
  wpi::math::Transform3d compoundTestTransform = wpi::math::Transform3d(
      -12_in, -11_in, 3_m, wpi::math::Rotation3d(37_deg, 6_deg, 60_deg));

  photon::PhotonPoseEstimator estimator(aprilTags, compoundTestTransform);

  /* real pose of the robot base to test against */
  wpi::math::Pose3d realPose = wpi::math::Pose3d(
      7.3_m, 4.42_m, 0_m, wpi::math::Rotation3d(0_rad, 0_rad, 2.197_rad));

  photon::PhotonPipelineResult result = cameraOneSim.Process(
      1_ms, realPose.TransformBy(estimator.GetRobotToCameraTransform()),
      targets);
  cameraOne.testResult = {result};
  cameraOne.testResult[0].SetReceiveTimestamp(17_s);

  estimator.AddHeadingData(result.GetTimestamp(), realPose.Rotation());

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimatePnpDistanceTrigSolvePose(result);
  }

  ASSERT_TRUE(estimatedPose);
  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(wpi::units::unit_cast<double>(realPose.X()),
              wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(wpi::units::unit_cast<double>(realPose.Y()),
              wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(wpi::units::unit_cast<double>(realPose.Z()),
              wpi::units::unit_cast<double>(pose.Z()), .01);

  /* Straight on */
  wpi::math::Transform3d straightOnTestTransform = wpi::math::Transform3d(
      0_m, 0_m, 3_m, wpi::math::Rotation3d(0_rad, 0_rad, 0_rad));

  estimator.SetRobotToCameraTransform(straightOnTestTransform);
  realPose = wpi::math::Pose3d(4.81_m, 2.38_m, 0_m,
                               wpi::math::Rotation3d(0_rad, 0_rad, 2.818_rad));
  result = cameraOneSim.Process(
      1_ms, realPose.TransformBy(estimator.GetRobotToCameraTransform()),
      targets);
  cameraOne.testResult = {result};
  cameraOne.testResult[0].SetReceiveTimestamp(18_s);

  estimator.AddHeadingData(result.GetTimestamp(), realPose.Rotation());

  estimatedPose = std::nullopt;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimatePnpDistanceTrigSolvePose(result);
  }

  ASSERT_TRUE(estimatedPose);
  pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(wpi::units::unit_cast<double>(realPose.X()),
              wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(wpi::units::unit_cast<double>(realPose.Y()),
              wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(wpi::units::unit_cast<double>(realPose.Z()),
              wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, AverageBestPoses) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 2_m, 2_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 1_m, 1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(3_m, 3_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(3_m, 3_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(0_m, 0_m, 0_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(2_m, 1.9_m, 2.1_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(15));

  photon::PhotonPoseEstimator estimator(aprilTags, {});

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateAverageBestTargetsPose(result);
  }

  ASSERT_TRUE(estimatedPose);
  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(15.0,
              wpi::units::unit_cast<double>(estimatedPose.value().timestamp),
              .01);
  EXPECT_NEAR(2.15, wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(2.15, wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2.15, wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, MultiTagOnCoprocFallback) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 0, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(1_rad, 2_rad, 3_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(1_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.7, corners, detectedCorners},
      photon::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1, -1, -1.f,
          wpi::math::Transform3d(wpi::math::Translation3d(4_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          wpi::math::Transform3d(wpi::math::Translation3d(4_m, 2_m, 3_m),
                                 wpi::math::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt}};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(11));

  photon::PhotonPoseEstimator estimator(aprilTags, wpi::math::Transform3d{});

  std::optional<photon::EstimatedRobotPose> estimatedPose;
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateCoprocMultiTagPose(result);
  }
  ASSERT_FALSE(estimatedPose);
  for (const auto& result : cameraOne.GetAllUnreadResults()) {
    estimatedPose = estimator.EstimateLowestAmbiguityPose(result);
  }
  ASSERT_TRUE(estimatedPose);
  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  // Make sure values match what we'd expect for the LOWEST_AMBIGUITY strategy
  EXPECT_NEAR(
      11, wpi::units::unit_cast<double>(estimatedPose.value().timestamp), .02);
  EXPECT_NEAR(1, wpi::units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(3, wpi::units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2, wpi::units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, CopyResult) {
  std::vector<photon::PhotonTrackedTarget> targets{};

  auto testResult = photon::PhotonPipelineResult{
      photon::PhotonPipelineMetadata{0, 0, 2000, 1000}, targets, std::nullopt};
  testResult.SetReceiveTimestamp(wpi::units::second_t(11));

  auto test2 = testResult;

  EXPECT_NEAR(testResult.GetTimestamp().to<double>(),
              test2.GetTimestamp().to<double>(), 0.001);
}

TEST(PhotonPoseEstimatorTest, ConstrainedPnpEmptyCase) {
  photon::PhotonPoseEstimator estimator(
      wpi::apriltag::AprilTagFieldLayout::LoadField(
          wpi::apriltag::AprilTagField::k2024Crescendo),
      wpi::math::Transform3d());

  photon::PhotonPipelineResult result;
  auto distortion = Eigen::VectorXd::Zero(8);
  auto cameraMat = Eigen::Matrix3d{{399.37500000000006, 0, 319.5},
                                   {0, 399.16666666666674, 239.5},
                                   {0, 0, 1}};
  auto estimate = estimator.EstimateConstrainedSolvepnpPose(
      result, cameraMat, distortion, wpi::math::Pose3d(), true, 0.0);
  EXPECT_FALSE(estimate.has_value());
}

TEST(PhotonPoseEstimatorTest, ConstrainedPnpOneTag) {
  photon::PhotonCamera cameraOne = photon::PhotonCamera("test");
  auto distortion = Eigen::VectorXd::Zero(8);
  auto cameraMat = Eigen::Matrix3d{{399.37500000000006, 0, 319.5},
                                   {0, 399.16666666666674, 239.5},
                                   {0, 0, 1}};

  // Create corners data matching the Java test
  std::vector<photon::TargetCorner> corners8{
      photon::TargetCorner{98.09875447066685, 331.0093220119495},
      photon::TargetCorner{122.20226758624413, 335.50083894738486},
      photon::TargetCorner{127.17118732489361, 313.81406314178633},
      photon::TargetCorner{104.28543773760417, 309.6516557438994}};

  wpi::math::Transform3d poseTransform(
      wpi::math::Translation3d(3.1665557336121353_m, 4.430673446050584_m,
                               0.48678786477534686_m),
      wpi::math::Rotation3d(
          wpi::math::Quaternion(0.3132532247418243, 0.24722671090692333,
                                -0.08413452932300695, 0.9130568172784148)));

  std::vector<photon::PhotonTrackedTarget> targets{
      photon::PhotonTrackedTarget{0.0, 0.0, 0.0, 0.0, 8, 0, 0.0f, poseTransform,
                                  poseTransform, 0.0, corners8, corners8}};

  auto multiTagResult = std::make_optional<photon::MultiTargetPNPResult>(
      photon::PnpResult{poseTransform, poseTransform, 0.1, 0.1, 0.0},
      std::vector<int16_t>{8});

  photon::PhotonPipelineResult result{
      photon::PhotonPipelineMetadata{1, 10000, 2000, 100}, targets,
      multiTagResult};

  cameraOne.test = true;
  cameraOne.testResult = {result};
  cameraOne.testResult[0].SetReceiveTimestamp(wpi::units::second_t(15));

  const wpi::units::radian_t camPitch = 30_deg;
  const wpi::math::Transform3d kRobotToCam{
      wpi::math::Translation3d(0.5_m, 0.0_m, 0.5_m),
      wpi::math::Rotation3d(0_rad, -camPitch, 0_rad)};

  photon::PhotonPoseEstimator estimator(
      wpi::apriltag::AprilTagFieldLayout::LoadField(
          wpi::apriltag::AprilTagField::k2024Crescendo),
      kRobotToCam);

  auto estimatedMultiTagPose =
      estimator.EstimateCoprocMultiTagPose(cameraOne.testResult[0]);

  estimator.AddHeadingData(cameraOne.testResult[0].GetTimestamp(),
                           wpi::math::Rotation2d());

  auto estimatedPose = estimator.EstimateConstrainedSolvepnpPose(
      cameraOne.testResult[0], cameraMat, distortion,
      estimatedMultiTagPose->estimatedPose, true, 0);

  ASSERT_TRUE(estimatedPose.has_value());

  wpi::math::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(3.58, wpi::units::unit_cast<double>(pose.X()), 0.01);
  EXPECT_NEAR(4.13, wpi::units::unit_cast<double>(pose.Y()), 0.01);
  EXPECT_NEAR(0.0, wpi::units::unit_cast<double>(pose.Z()), 0.01);

  EXPECT_EQ(photon::CONSTRAINED_SOLVEPNP, estimatedPose.value().strategy);
}
