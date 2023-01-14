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

#include "photonlib/PhotonPoseEstimator.h"

#include <iostream>
#include <limits>
#include <map>
#include <span>
#include <string>
#include <utility>
#include <vector>

#include <frc/Errors.h>
#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Transform3d.h>
#include <units/time.h>

#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonTrackedTarget.h"

namespace photonlib {
PhotonPoseEstimator::PhotonPoseEstimator(frc::AprilTagFieldLayout tags,
                                         PoseStrategy strat, PhotonCamera&& cam,
                                         frc::Transform3d robotToCamera)
    : aprilTags(tags),
      strategy(strat),
      camera(std::move(cam)),
      m_robotToCamera(robotToCamera),
      lastPose(frc::Pose3d()),
      referencePose(frc::Pose3d()) {}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update() {
  auto result = camera.GetLatestResult();

  if (!result.HasTargets()) {
    return std::nullopt;
  }

  std::optional<EstimatedRobotPose> ret = std::nullopt;

  switch (strategy) {
    case LOWEST_AMBIGUITY:
      ret = LowestAmbiguityStrategy(result);
      break;
    case CLOSEST_TO_CAMERA_HEIGHT:
      ret = ClosestToCameraHeightStrategy(result);
      break;
    case CLOSEST_TO_REFERENCE_POSE:
      ret = ClosestToReferencePoseStrategy(result);
      break;
    case CLOSEST_TO_LAST_POSE:
      SetReferencePose(lastPose);
      ret = ClosestToReferencePoseStrategy(result);
      break;
    case AVERAGE_BEST_TARGETS:
      ret = AverageBestTargetsStrategy(result);
      break;
    default:
      FRC_ReportError(frc::warn::Warning, "Invalid Pose Strategy selected!",
                      "");
      return std::nullopt;
  }

  if (!ret) {
    // TODO
  }

  return ret;
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::LowestAmbiguityStrategy(
    PhotonPipelineResult result) {
  int lowestAJ = -1;
  double lowestAmbiguityScore = std::numeric_limits<double>::infinity();
  auto targets = result.GetTargets();
  for (PhotonPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
    if (targets[j].GetPoseAmbiguity() < lowestAmbiguityScore) {
      lowestAJ = j;
      lowestAmbiguityScore = targets[j].GetPoseAmbiguity();
    }
  }

  if (lowestAJ == -1) {
    return std::nullopt;
  }

  PhotonTrackedTarget bestTarget = targets[lowestAJ];

  std::optional<frc::Pose3d> fiducialPose =
      aprilTags.GetTagPose(bestTarget.GetFiducialId());
  if (!fiducialPose) {
    FRC_ReportError(frc::warn::Warning,
                    "Tried to get pose of unknown April Tag: {}",
                    bestTarget.GetFiducialId());
    return std::nullopt;
  }

  return EstimatedRobotPose{
      fiducialPose.value()
          .TransformBy(bestTarget.GetBestCameraToTarget().Inverse())
          .TransformBy(m_robotToCamera.Inverse()),
      result.GetTimestamp()};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::ClosestToCameraHeightStrategy(
    PhotonPipelineResult result) {
  units::meter_t smallestHeightDifference =
      units::meter_t(std::numeric_limits<double>::infinity());

  std::optional<EstimatedRobotPose> pose = std::nullopt;

  for (auto& target : result.GetTargets()) {
    std::optional<frc::Pose3d> fiducialPose =
        aprilTags.GetTagPose(target.GetFiducialId());
    if (!fiducialPose) {
      FRC_ReportError(frc::warn::Warning,
                      "Tried to get pose of unknown April Tag: {}",
                      target.GetFiducialId());
      continue;
    }
    frc::Pose3d targetPose = fiducialPose.value();

    units::meter_t alternativeDifference = units::math::abs(
        m_robotToCamera.Z() -
        targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
            .Z());

    units::meter_t bestDifference = units::math::abs(
        m_robotToCamera.Z() -
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()).Z());

    if (alternativeDifference < smallestHeightDifference) {
      smallestHeightDifference = alternativeDifference;
      pose = EstimatedRobotPose{
          targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          result.GetTimestamp()};
    }
    if (bestDifference < smallestHeightDifference) {
      smallestHeightDifference = bestDifference;
      pose = EstimatedRobotPose{
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          result.GetTimestamp()};
    }
  }

  return pose;
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::ClosestToReferencePoseStrategy(
    PhotonPipelineResult result) {
  units::meter_t smallestDifference =
      units::meter_t(std::numeric_limits<double>::infinity());
  units::second_t stateTimestamp = units::second_t(0);
  frc::Pose3d pose = lastPose;

  auto targets = result.GetTargets();
  for (PhotonPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
    PhotonTrackedTarget target = targets[j];
    std::optional<frc::Pose3d> fiducialPose =
        aprilTags.GetTagPose(target.GetFiducialId());
    if (!fiducialPose) {
      FRC_ReportError(frc::warn::Warning,
                      "Tried to get pose of unknown April Tag: {}",
                      target.GetFiducialId());
      continue;
    }
    frc::Pose3d targetPose = fiducialPose.value();

    const auto altPose =
        targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
            .TransformBy(m_robotToCamera.Inverse());
    const auto bestPose =
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
            .TransformBy(m_robotToCamera.Inverse());

    units::meter_t alternativeDifference = units::math::abs(
        referencePose.Translation().Distance(altPose.Translation()));
    units::meter_t bestDifference = units::math::abs(
        referencePose.Translation().Distance(bestPose.Translation()));
    if (alternativeDifference < smallestDifference) {
      smallestDifference = alternativeDifference;
      pose = altPose;
      stateTimestamp = result.GetTimestamp();
    }

    if (bestDifference < smallestDifference) {
      smallestDifference = bestDifference;
      pose = bestPose;
      stateTimestamp = result.GetTimestamp();
    }
  }

  return EstimatedRobotPose{pose, stateTimestamp};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::AverageBestTargetsStrategy(PhotonPipelineResult result) {
  std::vector<std::pair<frc::Pose3d, std::pair<double, units::second_t>>>
      tempPoses;
  double totalAmbiguity = 0;

  auto targets = result.GetTargets();
  for (PhotonPoseEstimator::size_type j = 0; j < targets.size(); ++j) {
    PhotonTrackedTarget target = targets[j];
    std::optional<frc::Pose3d> fiducialPose =
        aprilTags.GetTagPose(target.GetFiducialId());
    if (!fiducialPose) {
      FRC_ReportError(frc::warn::Warning,
                      "Tried to get pose of unknown April Tag: {}",
                      target.GetFiducialId());
      continue;
    }

    frc::Pose3d targetPose = fiducialPose.value();
    // Ambiguity = 0, use that pose
    if (target.GetPoseAmbiguity() == 0) {
      return EstimatedRobotPose{
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          result.GetLatency()};
    }
    totalAmbiguity += 1. / target.GetPoseAmbiguity();

    tempPoses.push_back(std::make_pair(
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()),
        std::make_pair(target.GetPoseAmbiguity(), result.GetTimestamp())));
  }

  frc::Translation3d transform = frc::Translation3d();
  frc::Rotation3d rotation = frc::Rotation3d();

  for (std::pair<frc::Pose3d, std::pair<double, units::second_t>>& pair :
       tempPoses) {
    double weight = (1. / pair.second.first) / totalAmbiguity;
    transform = transform + pair.first.Translation() * weight;
    rotation = rotation + pair.first.Rotation() * weight;
  }

  return EstimatedRobotPose{frc::Pose3d(transform, rotation),
                            result.GetTimestamp()};
}
}  // namespace photonlib
