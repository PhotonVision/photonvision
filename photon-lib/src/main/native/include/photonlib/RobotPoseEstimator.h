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
#include <memory>
#include <utility>
#include <vector>

#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Transform3d.h>

#include "photonlib/PhotonCamera.h"

namespace frc {
class AprilTagFieldLayout;
}  // namespace frc

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
  using map_value_type =
      std::pair<std::shared_ptr<PhotonCamera>, frc::Transform3d>;
  using size_type = std::vector<map_value_type>::size_type;

  explicit RobotPoseEstimator(
      std::shared_ptr<frc::AprilTagFieldLayout> aprilTags,
      PoseStrategy strategy, std::vector<map_value_type> cameras);

  std::pair<frc::Pose3d, units::millisecond_t> Update();

  inline void SetPoseStrategy(PoseStrategy strat) { strategy = strat; }

  inline void SetReferencePose(frc::Pose3d referencePose) {
    this->referencePose = referencePose;
  }

  inline void SetLastPose(frc::Pose3d lastPose) { this->lastPose = lastPose; }

  inline void SetCameras(
      const std::vector<std::pair<std::shared_ptr<PhotonCamera>,
                                  frc::Transform3d>>& cameras) {
    this->cameras = cameras;
  }

  PoseStrategy GetPoseStrategy() const { return strategy; }

  frc::Pose3d GetLastPose() const { return lastPose; }

  frc::Pose3d GetReferencePose() const { return referencePose; }

 private:
  std::shared_ptr<frc::AprilTagFieldLayout> aprilTags;
  PoseStrategy strategy;
  std::vector<map_value_type> cameras;
  frc::Pose3d lastPose;
  frc::Pose3d referencePose;

  std::pair<frc::Pose3d, units::millisecond_t> LowestAmbiguityStrategy();

  std::pair<frc::Pose3d, units::millisecond_t> ClosestToCameraHeightStrategy();

  std::pair<frc::Pose3d, units::millisecond_t> ClosestToReferencePoseStrategy();

  std::pair<frc::Pose3d, units::millisecond_t> AverageBestTargetsStrategy();
};

}  // namespace photonlib
