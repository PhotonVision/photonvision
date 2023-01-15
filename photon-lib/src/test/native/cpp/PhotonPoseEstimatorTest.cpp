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

#include <map>
#include <utility>
#include <vector>

#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Transform3d.h>
#include <units/angle.h>
#include <units/length.h>
#include <wpi/SmallVector.h>

#include "gtest/gtest.h"
#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonPoseEstimator.h"
#include "photonlib/PhotonTrackedTarget.h"

static std::vector<frc::AprilTag> tags = {
    {0, frc::Pose3d(units::meter_t(3), units::meter_t(3), units::meter_t(3),
                    frc::Rotation3d())},
    {1, frc::Pose3d(units::meter_t(5), units::meter_t(5), units::meter_t(5),
                    frc::Rotation3d())}};

static frc::AprilTagFieldLayout aprilTags{tags, 54_ft, 27_ft};

static wpi::SmallVector<std::pair<double, double>, 4> corners{
    std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}};
static std::vector<std::pair<double, double>> detectedCorners{
    std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}};

TEST(PhotonPoseEstimatorTest, LowestAmbiguityStrategy) {
  photonlib::PhotonCamera cameraOne = photonlib::PhotonCamera("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 3> targets{
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 0,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.7, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1,
          frc::Transform3d(frc::Translation3d(4_m, 2_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(4_m, 2_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {2_ms, targets};
  cameraOne.testResult.SetTimestamp(units::second_t(11));

  photonlib::PhotonPoseEstimator estimator(
      aprilTags, photonlib::LOWEST_AMBIGUITY, std::move(cameraOne), {});
  auto estimatedPose = estimator.Update();
  frc::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(11, units::unit_cast<double>(estimatedPose.value().timestamp),
              .02);
  EXPECT_NEAR(1, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(3, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2, units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, ClosestToCameraHeightStrategy) {
  std::vector<frc::AprilTag> tags = {
      {0, frc::Pose3d(units::meter_t(3), units::meter_t(3), units::meter_t(3),
                      frc::Rotation3d())},
      {1, frc::Pose3d(units::meter_t(5), units::meter_t(5), units::meter_t(5),
                      frc::Rotation3d())},
  };
  auto aprilTags = frc::AprilTagFieldLayout(tags, 54_ft, 27_ft);

  std::vector<std::pair<photonlib::PhotonCamera, frc::Transform3d>> cameras;

  photonlib::PhotonCamera cameraOne = photonlib::PhotonCamera("test");

  // ID 0 at 3,3,3
  // ID 1 at 5,5,5

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 3> targets{
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0,
          frc::Transform3d(frc::Translation3d(4_m, 4_m, 4_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(5_m, 5_m, 5_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {2_ms, targets};
  cameraOne.testResult.SetTimestamp(17_s);

  photonlib::PhotonPoseEstimator estimator(
      aprilTags, photonlib::CLOSEST_TO_CAMERA_HEIGHT, std::move(cameraOne),
      {{0_m, 0_m, 4_m}, {}});
  auto estimatedPose = estimator.Update();
  frc::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(17, units::unit_cast<double>(estimatedPose.value().timestamp),
              .02);
  EXPECT_NEAR(4, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(4, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(0, units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, ClosestToReferencePoseStrategy) {
  photonlib::PhotonCamera cameraOne = photonlib::PhotonCamera("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 3> targets{
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0,
          frc::Transform3d(frc::Translation3d(2.2_m, 2.2_m, 2.2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1.9_m, 2.1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {2_ms, targets};
  cameraOne.testResult.SetTimestamp(units::second_t(17));

  photonlib::PhotonPoseEstimator estimator(aprilTags,
                                           photonlib::CLOSEST_TO_REFERENCE_POSE,
                                           std::move(cameraOne), {});
  estimator.SetReferencePose(
      frc::Pose3d(1_m, 1_m, 1_m, frc::Rotation3d(0_rad, 0_rad, 0_rad)));
  auto estimatedPose = estimator.Update();
  frc::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(17, units::unit_cast<double>(estimatedPose.value().timestamp),
              .01);
  EXPECT_NEAR(1, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(1.1, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(.9, units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, ClosestToLastPose) {
  photonlib::PhotonCamera cameraOne = photonlib::PhotonCamera("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 3> targets{
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0,
          frc::Transform3d(frc::Translation3d(2.2_m, 2.2_m, 2.2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1.9_m, 2.1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {2_ms, targets};
  cameraOne.testResult.SetTimestamp(units::second_t(17));

  photonlib::PhotonPoseEstimator estimator(
      aprilTags, photonlib::CLOSEST_TO_LAST_POSE, std::move(cameraOne), {});
  estimator.SetLastPose(
      frc::Pose3d(1_m, 1_m, 1_m, frc::Rotation3d(0_rad, 0_rad, 0_rad)));
  auto estimatedPose = estimator.Update();
  frc::Pose3d pose = estimatedPose.value().estimatedPose;

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 3> targetsThree{
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 0,
          frc::Transform3d(frc::Translation3d(2.1_m, 1.9_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0,
          frc::Transform3d(frc::Translation3d(2.4_m, 2.4_m, 2.2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  estimator.GetCamera().testResult = {2_ms, targetsThree};
  estimator.GetCamera().testResult.SetTimestamp(units::second_t(7));

  estimatedPose = estimator.Update();
  pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(7.0, units::unit_cast<double>(estimatedPose.value().timestamp),
              .01);
  EXPECT_NEAR(.9, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(1.1, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(1, units::unit_cast<double>(pose.Z()), .01);
}

TEST(PhotonPoseEstimatorTest, AverageBestPoses) {
  photonlib::PhotonCamera cameraOne = photonlib::PhotonCamera("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 3> targets{
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 0,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, 1,
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3, corners, detectedCorners},
      photonlib::PhotonTrackedTarget{
          9.0, -2.0, 19.0, 3.0, 0,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1.9_m, 2.1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4, corners, detectedCorners}};

  cameraOne.test = true;
  cameraOne.testResult = {2_ms, targets};
  cameraOne.testResult.SetTimestamp(units::second_t(15));

  photonlib::PhotonPoseEstimator estimator(
      aprilTags, photonlib::AVERAGE_BEST_TARGETS, std::move(cameraOne), {});
  auto estimatedPose = estimator.Update();
  frc::Pose3d pose = estimatedPose.value().estimatedPose;

  EXPECT_NEAR(15.0, units::unit_cast<double>(estimatedPose.value().timestamp),
              .01);
  EXPECT_NEAR(2.15, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(2.15, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2.15, units::unit_cast<double>(pose.Z()), .01);
}
