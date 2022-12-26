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

#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Transform3d.h>
#include <units/angle.h>
#include <units/length.h>
#include <wpi/SmallVector.h>

#include "gtest/gtest.h"
#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonTrackedTarget.h"
#include "photonlib/RobotPoseEstimator.h"

TEST(RobotPoseEstimatorTest, LowestAmbiguityStrategy) {
  std::map<int, frc::Pose3d> aprilTags;
  aprilTags.insert({0, frc::Pose3d(units::meter_t(3), units::meter_t(3),
                                   units::meter_t(3), frc::Rotation3d())});
  aprilTags.insert({1, frc::Pose3d(units::meter_t(5), units::meter_t(5),
                                   units::meter_t(5), frc::Rotation3d())});

  std::vector<
      std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
      cameras;

  std::shared_ptr<photonlib::PhotonCamera> cameraOne =
      std::make_shared<photonlib::PhotonCamera>("test");
  std::shared_ptr<photonlib::PhotonCamera> cameraTwo =
      std::make_shared<photonlib::PhotonCamera>("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targets{
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          0,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.7,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          1,
          frc::Transform3d(frc::Translation3d(4_m, 2_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(4_m, 2_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraOne->test = true;
  cameraOne->testResult = {2_s, targets};

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 1> targetsTwo{
      photonlib::PhotonTrackedTarget{
          9.0,
          -2.0,
          19.0,
          3.0,
          0,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          0.4,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraTwo->test = true;
  cameraTwo->testResult = {4_s, targetsTwo};

  cameras.push_back(std::make_pair(
      cameraOne, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  cameras.push_back(std::make_pair(
      cameraTwo, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  photonlib::RobotPoseEstimator estimator(aprilTags,
                                          photonlib::LOWEST_AMBIGUITY, cameras);
  std::pair<frc::Pose3d, units::millisecond_t> estimatedPose =
      estimator.Update();
  frc::Pose3d pose = estimatedPose.first;

  EXPECT_NEAR(2, units::unit_cast<double>(estimatedPose.second), .01);
  EXPECT_NEAR(1, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(3, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2, units::unit_cast<double>(pose.Z()), .01);
}

TEST(RobotPoseEstimatorTest, ClosestToCameraHeightStrategy) {
  std::map<int, frc::Pose3d> aprilTags;
  aprilTags.insert({0, frc::Pose3d(units::meter_t(3), units::meter_t(3),
                                   units::meter_t(3), frc::Rotation3d())});
  aprilTags.insert({1, frc::Pose3d(units::meter_t(5), units::meter_t(5),
                                   units::meter_t(5), frc::Rotation3d())});

  std::vector<
      std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
      cameras;

  std::shared_ptr<photonlib::PhotonCamera> cameraOne =
      std::make_shared<photonlib::PhotonCamera>("test");
  std::shared_ptr<photonlib::PhotonCamera> cameraTwo =
      std::make_shared<photonlib::PhotonCamera>("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targets{
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          1,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraOne->test = true;
  cameraOne->testResult = {2_s, targets};

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 1> targetsTwo{
      photonlib::PhotonTrackedTarget{
          9.0,
          -2.0,
          19.0,
          3.0,
          0,
          frc::Transform3d(frc::Translation3d(4_m, 4_m, 4_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(5_m, 5_m, 5_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraTwo->test = true;
  cameraTwo->testResult = {4_s, targetsTwo};

  cameras.push_back(std::make_pair(
      cameraOne, frc::Transform3d(frc::Translation3d(0_m, 0_m, 4_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  cameras.push_back(std::make_pair(
      cameraTwo, frc::Transform3d(frc::Translation3d(0_m, 0_m, 2_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  photonlib::RobotPoseEstimator estimator(
      aprilTags, photonlib::CLOSEST_TO_CAMERA_HEIGHT, cameras);
  std::pair<frc::Pose3d, units::millisecond_t> estimatedPose =
      estimator.Update();
  frc::Pose3d pose = estimatedPose.first;

  EXPECT_NEAR(2, units::unit_cast<double>(estimatedPose.second), .01);
  EXPECT_NEAR(4, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(4, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(4, units::unit_cast<double>(pose.Z()), .01);
}

TEST(RobotPoseEstimatorTest, ClosestToReferencePoseStrategy) {
  std::map<int, frc::Pose3d> aprilTags;
  aprilTags.insert({0, frc::Pose3d(units::meter_t(3), units::meter_t(3),
                                   units::meter_t(3), frc::Rotation3d())});
  aprilTags.insert({1, frc::Pose3d(units::meter_t(5), units::meter_t(5),
                                   units::meter_t(5), frc::Rotation3d())});

  std::vector<
      std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
      cameras;

  std::shared_ptr<photonlib::PhotonCamera> cameraOne =
      std::make_shared<photonlib::PhotonCamera>("test");
  std::shared_ptr<photonlib::PhotonCamera> cameraTwo =
      std::make_shared<photonlib::PhotonCamera>("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targets{
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          1,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraOne->test = true;
  cameraOne->testResult = {2_s, targets};

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 1> targetsTwo{
      photonlib::PhotonTrackedTarget{
          9.0,
          -2.0,
          19.0,
          3.0,
          0,
          frc::Transform3d(frc::Translation3d(2.2_m, 2.2_m, 2.2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1.9_m, 2.1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraTwo->test = true;
  cameraTwo->testResult = {4_s, targetsTwo};

  cameras.push_back(std::make_pair(
      cameraOne, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  cameras.push_back(std::make_pair(
      cameraTwo, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  photonlib::RobotPoseEstimator estimator(
      aprilTags, photonlib::CLOSEST_TO_REFERENCE_POSE, cameras);
  estimator.SetReferencePose(
      frc::Pose3d(1_m, 1_m, 1_m, frc::Rotation3d(0_rad, 0_rad, 0_rad)));
  std::pair<frc::Pose3d, units::millisecond_t> estimatedPose =
      estimator.Update();
  frc::Pose3d pose = estimatedPose.first;

  EXPECT_NEAR(4, units::unit_cast<double>(estimatedPose.second), .01);
  EXPECT_NEAR(1, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(1.1, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(.9, units::unit_cast<double>(pose.Z()), .01);
}

TEST(RobotPoseEstimatorTest, ClosestToLastPose) {
  std::map<int, frc::Pose3d> aprilTags;
  aprilTags.insert({0, frc::Pose3d(units::meter_t(3), units::meter_t(3),
                                   units::meter_t(3), frc::Rotation3d())});
  aprilTags.insert({1, frc::Pose3d(units::meter_t(5), units::meter_t(5),
                                   units::meter_t(5), frc::Rotation3d())});

  std::vector<
      std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
      cameras;

  std::shared_ptr<photonlib::PhotonCamera> cameraOne =
      std::make_shared<photonlib::PhotonCamera>("test");
  std::shared_ptr<photonlib::PhotonCamera> cameraTwo =
      std::make_shared<photonlib::PhotonCamera>("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targets{
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          1,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraOne->test = true;
  cameraOne->testResult = {2_s, targets};

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 1> targetsTwo{
      photonlib::PhotonTrackedTarget{
          9.0,
          -2.0,
          19.0,
          3.0,
          0,
          frc::Transform3d(frc::Translation3d(2.2_m, 2.2_m, 2.2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1.9_m, 2.1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraTwo->test = true;
  cameraTwo->testResult = {4_s, targetsTwo};

  cameras.push_back(std::make_pair(
      cameraOne, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  cameras.push_back(std::make_pair(
      cameraTwo, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  photonlib::RobotPoseEstimator estimator(
      aprilTags, photonlib::CLOSEST_TO_LAST_POSE, cameras);
  estimator.SetLastPose(
      frc::Pose3d(1_m, 1_m, 1_m, frc::Rotation3d(0_rad, 0_rad, 0_rad)));
  std::pair<frc::Pose3d, units::millisecond_t> estimatedPose =
      estimator.Update();
  frc::Pose3d pose = estimatedPose.first;

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targetsThree{
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          1,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          0,
          frc::Transform3d(frc::Translation3d(2.1_m, 1.9_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraOne->testResult = {2_s, targetsThree};

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 1> targetsFour{
      photonlib::PhotonTrackedTarget{
          9.0,
          -2.0,
          19.0,
          3.0,
          0,
          frc::Transform3d(frc::Translation3d(2.4_m, 2.4_m, 2.2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraTwo->testResult = {4_s, targetsFour};

  std::vector<
      std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
      camerasUpdated;
  camerasUpdated.push_back(std::make_pair(
      cameraOne, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  camerasUpdated.push_back(std::make_pair(
      cameraTwo, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  estimator.SetCameras(camerasUpdated);
  estimatedPose = estimator.Update();
  pose = estimatedPose.first;

  EXPECT_NEAR(2, units::unit_cast<double>(estimatedPose.second), .01);
  EXPECT_NEAR(.9, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(1.1, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(1, units::unit_cast<double>(pose.Z()), .01);
}

TEST(RobotPoseEstimatorTest, AverageBestPoses) {
  std::map<int, frc::Pose3d> aprilTags;
  aprilTags.insert({0, frc::Pose3d(units::meter_t(3), units::meter_t(3),
                                   units::meter_t(3), frc::Rotation3d())});
  aprilTags.insert({1, frc::Pose3d(units::meter_t(5), units::meter_t(5),
                                   units::meter_t(5), frc::Rotation3d())});

  std::vector<
      std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
      cameras;

  std::shared_ptr<photonlib::PhotonCamera> cameraOne =
      std::make_shared<photonlib::PhotonCamera>("test");
  std::shared_ptr<photonlib::PhotonCamera> cameraTwo =
      std::make_shared<photonlib::PhotonCamera>("test");

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targets{
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          0,
          frc::Transform3d(frc::Translation3d(2_m, 2_m, 2_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 1_m, 1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.7,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photonlib::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          1,
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(3_m, 3_m, 3_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.3,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraOne->test = true;
  cameraOne->testResult = {2_s, targets};

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 1> targetsTwo{
      photonlib::PhotonTrackedTarget{
          9.0,
          -2.0,
          19.0,
          3.0,
          0,
          frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          frc::Transform3d(frc::Translation3d(2_m, 1.9_m, 2.1_m),
                           frc::Rotation3d(0_rad, 0_rad, 0_rad)),
          0.4,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  cameraTwo->test = true;
  cameraTwo->testResult = {4_s, targetsTwo};

  cameras.push_back(std::make_pair(
      cameraOne, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  cameras.push_back(std::make_pair(
      cameraTwo, frc::Transform3d(frc::Translation3d(0_m, 0_m, 0_m),
                                  frc::Rotation3d(0_rad, 0_rad, 0_rad))));
  photonlib::RobotPoseEstimator estimator(
      aprilTags, photonlib::AVERAGE_BEST_TARGETS, cameras);
  std::pair<frc::Pose3d, units::millisecond_t> estimatedPose =
      estimator.Update();
  frc::Pose3d pose = estimatedPose.first;

  EXPECT_NEAR(2.6885245901639347,
              units::unit_cast<double>(estimatedPose.second), .01);
  EXPECT_NEAR(2.15, units::unit_cast<double>(pose.X()), .01);
  EXPECT_NEAR(2.15, units::unit_cast<double>(pose.Y()), .01);
  EXPECT_NEAR(2.15, units::unit_cast<double>(pose.Z()), .01);
}
