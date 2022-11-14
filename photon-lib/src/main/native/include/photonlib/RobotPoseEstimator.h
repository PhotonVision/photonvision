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

#pragma once

#include <map>
#include <utility>
#include <vector>

#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Transform3d.h>

#include "photonlib/PhotonCamera.h"

namespace photonlib {
enum PoseStrategy : int {
  LOWEST_AMBIGUITY,
  CLOSEST_TO_CAMERA_HEIGHT,
  CLOSEST_TO_REFERENCE_POSE,
  CLOSEST_TO_LAST_POSE,
  AVERAGE_BEST_TARGETS
};
/**
 * A managing class to determine how an estimated pose should be chosen.
 */
class RobotPoseEstimator {
 public:
  explicit RobotPoseEstimator(
      std::map<int, frc::Pose3d> aprilTags, PoseStrategy strategy,
      std::vector<std::pair<PhotonCamera, frc::Transform3d>>);

  std::pair<frc::Pose3d, units::millisecond_t> Update();

  void SetPoseStrategy(PoseStrategy strategy);

  PoseStrategy GetPoseStrategy() const { return strategy; }

  frc::Pose3d GetLastPose() const { return lastPose; }

  frc::Pose3d GetReferencePose() const { return referencePose; }

 private:
  std::map<int, frc::Pose3d> aprilTags;
  PoseStrategy strategy;
  std::vector<std::pair<PhotonCamera, frc::Transform3d>> cameras;
  frc::Pose3d lastPose;
  frc::Pose3d referencePose;

  std::pair<frc::Pose3d, units::millisecond_t> LowestAmbiguityStrategy();

  std::pair<frc::Pose3d, units::millisecond_t> ClosestToCameraHeightStrategy();

  std::pair<frc::Pose3d, units::millisecond_t> ClosestToReferencePoseStrategy();

  std::pair<frc::Pose3d, units::millisecond_t> AverageBestTargetsStrategy();
};

}  // namespace photonlib
