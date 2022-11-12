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

#include "photonlib/RobotPoseEstimator.h"

#include <map>
#include <string>
#include <utility>
#include <vector>

#include <frc/Errors.h>
#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Transform3d.h>
#include <units/time.h>
#include <wpi/span.h>

#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonTrackedTarget.h"

namespace photonlib {
RobotPoseEstimator::RobotPoseEstimator(
    std::map<int, frc::Pose3d> aprilTags, PoseStrategy strategy,
    std::vector<std::pair<PhotonCamera, frc::Transform3d>> cameras) {
  this->aprilTags = aprilTags;
  this->strategy = strategy;
  this->cameras = cameras;
  this->lastPose = frc::Pose3d();
}

std::pair<frc::Pose3d, units::millisecond_t> RobotPoseEstimator::update() {
  if (cameras.empty()) {
    return std::make_pair(lastPose, units::millisecond_t(0));
  }
  std::pair<frc::Pose3d, units::millisecond_t> pair;
  switch (strategy) {
    case LOWEST_AMBIGUITY:
      pair = LowestAmbiguityStrategy();
      lastPose = pair.first;
      return pair;
    default:
      FRC_ReportError(frc::warn::Warning, "Invalid Pose Strategy selected!",
                      "");
  }
  return std::make_pair(lastPose, units::millisecond_t(0));
}

std::pair<frc::Pose3d, units::millisecond_t>
RobotPoseEstimator::LowestAmbiguityStrategy() {
  int lowestAI = -1;
  int lowestAJ = -1;
  double lowestAmbiguityScore = 10;
  for (std::string::size_type i = 0; i < cameras.size(); ++i) {
    std::pair<PhotonCamera, frc::Transform3d> p = cameras[i];
    wpi::span<const PhotonTrackedTarget> targets =
        p.first.GetLatestResult().GetTargets();
    for (std::string::size_type j = 0; j < targets.size(); ++j) {
      if (targets[j].GetPoseAmbiguity() < lowestAmbiguityScore) {
        lowestAI = i;
        lowestAJ = j;
        lowestAmbiguityScore = targets[j].GetPoseAmbiguity();
      }
    }
  }

  if (lowestAI == -1 || lowestAJ == -1) {
    return std::make_pair(lastPose, units::millisecond_t(0));
  }

  PhotonTrackedTarget bestTarget =
      cameras[lowestAI].first.GetLatestResult().GetTargets()[lowestAJ];

  if (aprilTags.count(bestTarget.GetFiducialId()) == 0) {
    FRC_ReportError(frc::warn::Warning,
                    "Tried to get pose of unknown April Tag: {}",
                    bestTarget.GetFiducialId());
    return std::make_pair(lastPose, units::millisecond_t(0));
  }

  return std::make_pair(
      aprilTags[bestTarget.GetFiducialId()]
          .TransformBy(bestTarget.GetBestCameraToTarget().Inverse())
          .TransformBy(cameras[lowestAI].second.Inverse()),
      cameras[lowestAI].first.GetLatestResult().GetLatency() / 1000.);
}
}  // namespace photonlib
