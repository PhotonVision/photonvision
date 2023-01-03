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

#include <iostream>
#include <limits>
#include <map>
#include <span>
#include <string>
#include <utility>
#include <vector>

#include <frc/Errors.h>
#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Transform3d.h>
#include <units/time.h>

#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonTrackedTarget.h"

namespace photonlib {
RobotPoseEstimator::RobotPoseEstimator(
    std::shared_ptr<frc::AprilTagFieldLayout> tags, PoseStrategy strat,
    std::vector<std::pair<std::shared_ptr<PhotonCamera>, frc::Transform3d>>
        cams)
    : aprilTags(tags),
      strategy(strat),
      cameras(std::move(cams)),
      lastPose(frc::Pose3d()),
      referencePose(frc::Pose3d()) {}

std::pair<frc::Pose3d, units::second_t> RobotPoseEstimator::Update() {
  if (cameras.empty()) {
    return std::make_pair(lastPose, units::second_t(0));
  }

  std::pair<frc::Pose3d, units::second_t> pair;
  switch (strategy) {
    case LOWEST_AMBIGUITY:
      pair = LowestAmbiguityStrategy();
      lastPose = pair.first;
      return pair;
    case CLOSEST_TO_CAMERA_HEIGHT:
      pair = ClosestToCameraHeightStrategy();
      lastPose = pair.first;
      return pair;
    case CLOSEST_TO_REFERENCE_POSE:
      pair = ClosestToReferencePoseStrategy();
      lastPose = pair.first;
      return pair;
    case CLOSEST_TO_LAST_POSE:
      SetReferencePose(lastPose);
      pair = ClosestToReferencePoseStrategy();
      lastPose = pair.first;
      return pair;
    case AVERAGE_BEST_TARGETS:
      pair = AverageBestTargetsStrategy();
      lastPose = pair.first;
      return pair;
    default:
      FRC_ReportError(frc::warn::Warning, "Invalid Pose Strategy selected!",
                      "");
  }

  return std::make_pair(lastPose, units::second_t(0));
}

std::pair<frc::Pose3d, units::second_t>
RobotPoseEstimator::LowestAmbiguityStrategy() {
  int lowestAI = -1;
  int lowestAJ = -1;
  double lowestAmbiguityScore = std::numeric_limits<double>::infinity();
  for (RobotPoseEstimator::size_type i = 0; i < cameras.size(); ++i) {
    std::pair<std::shared_ptr<PhotonCamera>, frc::Transform3d> p = cameras[i];
    std::span<const PhotonTrackedTarget> targets =
        p.first->GetLatestResult().GetTargets();
    for (RobotPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
      if (targets[j].GetPoseAmbiguity() < lowestAmbiguityScore) {
        lowestAI = i;
        lowestAJ = j;
        lowestAmbiguityScore = targets[j].GetPoseAmbiguity();
      }
    }
  }

  if (lowestAI == -1 || lowestAJ == -1) {
    return std::make_pair(lastPose, units::second_t(0));
  }

  PhotonTrackedTarget bestTarget =
      cameras[lowestAI].first->GetLatestResult().GetTargets()[lowestAJ];

  std::optional<frc::Pose3d> fiducialPose =
      aprilTags->GetTagPose(bestTarget.GetFiducialId());
  if (!fiducialPose) {
    FRC_ReportError(frc::warn::Warning,
                    "Tried to get pose of unknown April Tag: {}",
                    bestTarget.GetFiducialId());
    return std::make_pair(lastPose, units::second_t(0));
  }

  return std::make_pair(
      fiducialPose.value()
          .TransformBy(bestTarget.GetBestCameraToTarget().Inverse())
          .TransformBy(cameras[lowestAI].second.Inverse()),
      cameras[lowestAI].first->GetLatestResult().GetTimestamp());
}

std::pair<frc::Pose3d, units::second_t>
RobotPoseEstimator::ClosestToCameraHeightStrategy() {
  units::meter_t smallestHeightDifference =
      units::meter_t(std::numeric_limits<double>::infinity());
  units::second_t stateTimestamp = units::second_t(0);
  frc::Pose3d pose = lastPose;

  for (RobotPoseEstimator::size_type i = 0; i < cameras.size(); ++i) {
    std::pair<std::shared_ptr<PhotonCamera>, frc::Transform3d> p = cameras[i];
    std::span<const PhotonTrackedTarget> targets =
        p.first->GetLatestResult().GetTargets();
    for (RobotPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
      PhotonTrackedTarget target = targets[j];
      std::optional<frc::Pose3d> fiducialPose =
          aprilTags->GetTagPose(target.GetFiducialId());
      if (!fiducialPose) {
        FRC_ReportError(frc::warn::Warning,
                        "Tried to get pose of unknown April Tag: {}",
                        target.GetFiducialId());
        continue;
      }
      frc::Pose3d targetPose = fiducialPose.value();
      units::meter_t alternativeDifference = units::math::abs(
          p.second.Z() -
          targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
              .Z());
      units::meter_t bestDifference = units::math::abs(
          p.second.Z() -
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()).Z());
      if (alternativeDifference < smallestHeightDifference) {
        smallestHeightDifference = alternativeDifference;
        pose = targetPose.TransformBy(
            target.GetAlternateCameraToTarget().Inverse());
        stateTimestamp = p.first->GetLatestResult().GetTimestamp();
      }
      if (bestDifference < smallestHeightDifference) {
        smallestHeightDifference = bestDifference;
        pose = targetPose.TransformBy(target.GetBestCameraToTarget().Inverse());
        stateTimestamp = p.first->GetLatestResult().GetTimestamp();
      }
    }
  }
  return std::make_pair(pose, stateTimestamp);
}

std::pair<frc::Pose3d, units::second_t>
RobotPoseEstimator::ClosestToReferencePoseStrategy() {
  units::meter_t smallestDifference =
      units::meter_t(std::numeric_limits<double>::infinity());
  units::second_t stateTimestamp = units::second_t(0);
  frc::Pose3d pose = lastPose;

  for (RobotPoseEstimator::size_type i = 0; i < cameras.size(); ++i) {
    std::pair<std::shared_ptr<PhotonCamera>, frc::Transform3d> p = cameras[i];
    std::span<const PhotonTrackedTarget> targets =
        p.first->GetLatestResult().GetTargets();
    for (RobotPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
      PhotonTrackedTarget target = targets[j];
      std::optional<frc::Pose3d> fiducialPose =
          aprilTags->GetTagPose(target.GetFiducialId());
      if (!fiducialPose) {
        FRC_ReportError(frc::warn::Warning,
                        "Tried to get pose of unknown April Tag: {}",
                        target.GetFiducialId());
        continue;
      }
      frc::Pose3d targetPose = fiducialPose.value();
      units::meter_t alternativeDifference =
          units::math::abs(referencePose.Translation().Distance(
              targetPose
                  .TransformBy(target.GetAlternateCameraToTarget().Inverse())
                  .Translation()));
      units::meter_t bestDifference =
          units::math::abs(referencePose.Translation().Distance(
              targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
                  .Translation()));
      if (alternativeDifference < smallestDifference) {
        smallestDifference = alternativeDifference;
        pose = targetPose.TransformBy(
            target.GetAlternateCameraToTarget().Inverse());
        stateTimestamp = p.first->GetLatestResult().GetTimestamp();
      }

      if (bestDifference < smallestDifference) {
        smallestDifference = bestDifference;
        pose = targetPose.TransformBy(target.GetBestCameraToTarget().Inverse());
        stateTimestamp = p.first->GetLatestResult().GetTimestamp();
      }
    }
  }

  return std::make_pair(pose, stateTimestamp);
}

std::pair<frc::Pose3d, units::second_t>
RobotPoseEstimator::AverageBestTargetsStrategy() {
  std::vector<std::pair<frc::Pose3d, std::pair<double, units::second_t>>>
      tempPoses;
  double totalAmbiguity = 0;
  units::second_t timstampSum = units::second_t(0);

  for (RobotPoseEstimator::size_type i = 0; i < cameras.size(); ++i) {
    std::pair<std::shared_ptr<PhotonCamera>, frc::Transform3d> p = cameras[i];
    std::span<const PhotonTrackedTarget> targets =
        p.first->GetLatestResult().GetTargets();
    timstampSum += p.first->GetLatestResult().GetTimestamp();
    for (RobotPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
      PhotonTrackedTarget target = targets[j];
      std::optional<frc::Pose3d> fiducialPose =
          aprilTags->GetTagPose(target.GetFiducialId());
      if (!fiducialPose) {
        FRC_ReportError(frc::warn::Warning,
                        "Tried to get pose of unknown April Tag: {}",
                        target.GetFiducialId());
        continue;
      }

      frc::Pose3d targetPose = fiducialPose.value();
      if (target.GetPoseAmbiguity() == 0) {
        FRC_ReportError(frc::warn::Warning,
                        "Pose ambiguity of zero exists, using that instead!",
                        "");
        return std::make_pair(
            targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()),
            p.first->GetLatestResult().GetLatency() / 1000.);
      }
      totalAmbiguity += 1. / target.GetPoseAmbiguity();

      tempPoses.push_back(std::make_pair(
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()),
          std::make_pair(target.GetPoseAmbiguity(),
                         p.first->GetLatestResult().GetTimestamp())));
    }
  }

  frc::Translation3d transform = frc::Translation3d();
  frc::Rotation3d rotation = frc::Rotation3d();

  for (std::pair<frc::Pose3d, std::pair<double, units::second_t>>& pair :
       tempPoses) {
    double weight = (1. / pair.second.first) / totalAmbiguity;
    transform = transform + pair.first.Translation() * weight;
    rotation = rotation + pair.first.Rotation() * weight;
  }

  return std::make_pair(frc::Pose3d(transform, rotation),
                        timstampSum / cameras.size());
}
}  // namespace photonlib
