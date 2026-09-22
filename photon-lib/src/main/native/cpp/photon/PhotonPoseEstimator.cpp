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

#include <array>
#include <limits>
#include <optional>
#include <utility>
#include <vector>

#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Rotation3d.hpp>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/system/Errors.hpp>
#include <wpi/units/angle.hpp>
#include <wpi/units/math.hpp>
#include <wpi/units/time.hpp>
#include <wpi/util/UsageReporting.hpp>

#include "photon/PhotonCamera.h"
#include "photon/estimation/TargetModel.h"
#include "photon/estimation/VisionEstimation.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

namespace photon {

PhotonPoseEstimator::PhotonPoseEstimator(wpi::fields::Field tags,
                                         wpi::math::Transform3d robotToCamera)
    : aprilTags(tags),
      m_robotToCamera(robotToCamera),
      headingBuffer(
          wpi::math::TimeInterpolatableBuffer<wpi::math::Rotation2d>(1_s)) {
  wpi::util::ReportUsage("PhotonVision/PhotonPoseEstimator",
                         std::to_string(InstanceCount));
  InstanceCount++;
}

bool ShouldEstimate(const PhotonPipelineResult& result) {
  // Time in the past -- give up, since the following if expects times > 0
  if (result.GetTimestamp() < 0_s) {
    WPILIB_ReportError(wpi::warn::Warning,
                       "Result timestamp was reported in the past!");
    return false;
  }

  // If no targets seen, trivial case -- can't do estimation
  return result.HasTargets();
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateLowestAmbiguityPose(
    PhotonPipelineResult cameraResult) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  double lowestAmbiguityScore = std::numeric_limits<double>::infinity();
  auto targets = cameraResult.GetTargets();
  auto foundIt = targets.end();
  for (auto it = targets.begin(); it != targets.end(); ++it) {
    double targetPoseAmbiguity = it->GetPoseAmbiguity();
    // Skip non-fiducial targets (ambiguity == -1), otherwise they win over
    // every fiducial target and the whole estimate is thrown away when
    // GetTagPose(-1) returns nullopt.
    if (targetPoseAmbiguity != -1 &&
        targetPoseAmbiguity < lowestAmbiguityScore) {
      foundIt = it;
      lowestAmbiguityScore = targetPoseAmbiguity;
    }
  }

  if (foundIt == targets.end()) {
    return std::nullopt;
  }

  auto& bestTarget = *foundIt;

  std::optional<wpi::math::Pose3d> fiducialPose =
      aprilTags.GetTagPose(bestTarget.GetFiducialId());
  if (!fiducialPose) {
    WPILIB_ReportError(wpi::warn::Warning,
                       "Tried to get pose of unknown April Tag: {}",
                       bestTarget.GetFiducialId());
    return std::nullopt;
  }

  std::array<PhotonTrackedTarget, 1> usedTargets{bestTarget};
  return EstimatedRobotPose{
      fiducialPose->TransformBy(bestTarget.GetBestCameraToTarget().Inverse())
          .TransformBy(m_robotToCamera.Inverse()),
      cameraResult.GetTimestamp(), usedTargets, LOWEST_AMBIGUITY};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateClosestToCameraHeightPose(
    PhotonPipelineResult cameraResult) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  wpi::units::meter_t smallestHeightDifference =
      wpi::units::meter_t(std::numeric_limits<double>::infinity());

  std::optional<wpi::math::Pose3d> bestPose = std::nullopt;
  std::optional<PhotonTrackedTarget> bestTarget = std::nullopt;

  for (auto& target : cameraResult.GetTargets()) {
    std::optional<wpi::math::Pose3d> fiducialPose =
        aprilTags.GetTagPose(target.GetFiducialId());
    if (!fiducialPose) {
      WPILIB_ReportError(wpi::warn::Warning,
                         "Tried to get pose of unknown April Tag: {}",
                         target.GetFiducialId());
      continue;
    }
    wpi::math::Pose3d const targetPose = *fiducialPose;

    wpi::units::meter_t const alternativeDifference = wpi::units::math::abs(
        m_robotToCamera.Z() -
        targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
            .Z());

    wpi::units::meter_t const bestDifference = wpi::units::math::abs(
        m_robotToCamera.Z() -
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()).Z());

    if (alternativeDifference < smallestHeightDifference) {
      smallestHeightDifference = alternativeDifference;
      bestPose =
          targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse());
      bestTarget = target;
    }
    if (bestDifference < smallestHeightDifference) {
      smallestHeightDifference = bestDifference;
      bestPose =
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse());
      bestTarget = target;
    }
  }

  if (!bestTarget) {
    return std::nullopt;
  }
  std::array<PhotonTrackedTarget, 1> usedTargets{*bestTarget};
  return EstimatedRobotPose{*bestPose, cameraResult.GetTimestamp(), usedTargets,
                            CLOSEST_TO_CAMERA_HEIGHT};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateClosestToReferencePose(
    PhotonPipelineResult cameraResult, wpi::math::Pose3d referencePose) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  wpi::units::meter_t smallestDifference =
      wpi::units::meter_t(std::numeric_limits<double>::infinity());
  wpi::units::second_t stateTimestamp = wpi::units::second_t(0);
  wpi::math::Pose3d pose;
  std::optional<PhotonTrackedTarget> bestTarget = std::nullopt;

  auto targets = cameraResult.GetTargets();
  for (auto& target : targets) {
    std::optional<wpi::math::Pose3d> fiducialPose =
        aprilTags.GetTagPose(target.GetFiducialId());
    if (!fiducialPose) {
      WPILIB_ReportError(wpi::warn::Warning,
                         "Tried to get pose of unknown April Tag: {}",
                         target.GetFiducialId());
      continue;
    }
    wpi::math::Pose3d targetPose = fiducialPose.value();

    const auto altPose =
        targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
            .TransformBy(m_robotToCamera.Inverse());
    const auto bestPose =
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
            .TransformBy(m_robotToCamera.Inverse());

    wpi::units::meter_t const alternativeDifference = wpi::units::math::abs(
        referencePose.Translation().Distance(altPose.Translation()));
    wpi::units::meter_t const bestDifference = wpi::units::math::abs(
        referencePose.Translation().Distance(bestPose.Translation()));
    if (alternativeDifference < smallestDifference) {
      smallestDifference = alternativeDifference;
      pose = altPose;
      stateTimestamp = cameraResult.GetTimestamp();
      bestTarget = target;
    }

    if (bestDifference < smallestDifference) {
      smallestDifference = bestDifference;
      pose = bestPose;
      stateTimestamp = cameraResult.GetTimestamp();
      bestTarget = target;
    }
  }

  if (!bestTarget) {
    return std::nullopt;
  }
  std::array<PhotonTrackedTarget, 1> usedTargets{*bestTarget};
  return EstimatedRobotPose{pose, stateTimestamp, usedTargets,
                            CLOSEST_TO_REFERENCE_POSE};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateCoprocMultiTagPose(
    PhotonPipelineResult cameraResult) {
  if (!cameraResult.MultiTagResult() || !ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }

  const auto field2camera = cameraResult.MultiTagResult()->estimatedPose.best;

  const auto fieldToRobot =
      wpi::math::Pose3d() + field2camera + m_robotToCamera.Inverse();
  return photon::EstimatedRobotPose(fieldToRobot, cameraResult.GetTimestamp(),
                                    cameraResult.GetTargets(),
                                    MULTI_TAG_PNP_ON_COPROCESSOR);
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::EstimateRioMultiTagPose(
    PhotonPipelineResult cameraResult, PhotonCamera::CameraMatrix cameraMatrix,
    PhotonCamera::DistortionMatrix distCoeffs) {
  // Need at least 2 targets
  if (cameraResult.GetTargets().size() < 2 || !ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }

  std::vector<PhotonTrackedTarget> targets{cameraResult.GetTargets().begin(),
                                           cameraResult.GetTargets().end()};
  const auto pnpResult = VisionEstimation::EstimateCamPosePNP(
      cameraMatrix, distCoeffs, targets, aprilTags, photon::kAprilTag36h11);
  if (!pnpResult) {
    return std::nullopt;
  }

  const wpi::math::Pose3d fieldToRobot =
      wpi::math::Pose3d() + pnpResult->best + m_robotToCamera.Inverse();

  return photon::EstimatedRobotPose(fieldToRobot, cameraResult.GetTimestamp(),
                                    cameraResult.GetTargets(),
                                    MULTI_TAG_PNP_ON_RIO);
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimatePnpDistanceTrigSolvePose(
    PhotonPipelineResult cameraResult) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  PhotonTrackedTarget bestTarget = cameraResult.GetBestTarget();
  std::optional<wpi::math::Rotation2d> headingSampleOpt =
      headingBuffer.Sample(cameraResult.GetTimestamp());
  if (!headingSampleOpt) {
    WPILIB_ReportError(
        wpi::warn::Warning,
        "There was no heading data! Use AddHeadingData to add it!");
    return std::nullopt;
  }

  wpi::math::Rotation2d headingSample = headingSampleOpt.value();

  wpi::math::Translation2d camToTagTranslation =
      wpi::math::Translation3d(
          bestTarget.GetBestCameraToTarget().Translation().Norm(),
          wpi::math::Rotation3d(0_rad,
                                -wpi::units::degree_t(bestTarget.GetPitch()),
                                -wpi::units::degree_t(bestTarget.GetYaw())))
          .RotateBy(m_robotToCamera.Rotation())
          .ToTranslation2d()
          .RotateBy(headingSample);

  std::optional<wpi::math::Pose3d> fiducialPose =
      aprilTags.GetTagPose(bestTarget.GetFiducialId());
  if (!fiducialPose) {
    WPILIB_ReportError(wpi::warn::Warning,
                       "Tried to get pose of unknown April Tag: {}",
                       bestTarget.GetFiducialId());
    return std::nullopt;
  }

  wpi::math::Pose2d tagPose = fiducialPose.value().ToPose2d();

  wpi::math::Translation2d fieldToCameraTranslation =
      tagPose.Translation() - camToTagTranslation;

  wpi::math::Translation2d camToRobotTranslation =
      (-m_robotToCamera.Translation().ToTranslation2d())
          .RotateBy(headingSample);

  wpi::math::Pose2d robotPose = wpi::math::Pose2d(
      fieldToCameraTranslation + camToRobotTranslation, headingSample);

  std::array<PhotonTrackedTarget, 1> usedTargets{bestTarget};
  return EstimatedRobotPose{wpi::math::Pose3d(robotPose),
                            cameraResult.GetTimestamp(), usedTargets,
                            PNP_DISTANCE_TRIG_SOLVE};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateAverageBestTargetsPose(
    PhotonPipelineResult cameraResult) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  std::vector<
      std::pair<wpi::math::Pose3d, std::pair<double, PhotonTrackedTarget>>>
      tempPoses;
  double totalAmbiguity = 0;

  auto targets = cameraResult.GetTargets();
  for (auto& target : targets) {
    std::optional<wpi::math::Pose3d> fiducialPose =
        aprilTags.GetTagPose(target.GetFiducialId());
    if (!fiducialPose) {
      WPILIB_ReportError(wpi::warn::Warning,
                         "Tried to get pose of unknown April Tag: {}",
                         target.GetFiducialId());
      continue;
    }

    wpi::math::Pose3d targetPose = fiducialPose.value();
    // Ambiguity = 0, use that pose
    if (target.GetPoseAmbiguity() == 0) {
      std::array<PhotonTrackedTarget, 1> usedTargets{target};
      return EstimatedRobotPose{
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          cameraResult.GetTimestamp(), usedTargets, AVERAGE_BEST_TARGETS};
    }
    totalAmbiguity += 1. / target.GetPoseAmbiguity();

    tempPoses.push_back(std::make_pair(
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()),
        std::make_pair(target.GetPoseAmbiguity(), target)));
  }

  wpi::math::Translation3d transform = wpi::math::Translation3d();
  wpi::math::Rotation3d rotation = wpi::math::Rotation3d();

  std::vector<PhotonTrackedTarget> usedTargets;
  usedTargets.reserve(tempPoses.size());
  for (std::pair<wpi::math::Pose3d, std::pair<double, PhotonTrackedTarget>>&
           pair : tempPoses) {
    double const weight = (1. / pair.second.first) / totalAmbiguity;
    transform = transform + pair.first.Translation() * weight;
    rotation = rotation.RotateBy(pair.first.Rotation() * weight);
    usedTargets.push_back(pair.second.second);
  }

  return EstimatedRobotPose{wpi::math::Pose3d(transform, rotation),
                            cameraResult.GetTimestamp(), usedTargets,
                            AVERAGE_BEST_TARGETS};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateConstrainedSolvepnpPose(
    photon::PhotonPipelineResult cameraResult,
    photon::PhotonCamera::CameraMatrix cameraMatrix,
    photon::PhotonCamera::DistortionMatrix distCoeffs,
    wpi::math::Pose3d seedPose, bool headingFree, double headingScaleFactor) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  // Need heading if heading fixed
  if (!headingFree) {
    if (!headingBuffer.Sample(cameraResult.GetTimestamp())) {
      return std::nullopt;
    } else {
      // If heading fixed, force rotation component
      seedPose = wpi::math::Pose3d{
          seedPose.Translation(),
          wpi::math::Rotation3d{
              headingBuffer.Sample(cameraResult.GetTimestamp()).value()}};
    }
  }
  std::vector<photon::PhotonTrackedTarget> targets{
      cameraResult.GetTargets().begin(), cameraResult.GetTargets().end()};

  std::optional<photon::PnpResult> pnpResult =
      VisionEstimation::EstimateRobotPoseConstrainedSolvePNP(
          cameraMatrix, distCoeffs, targets, m_robotToCamera, seedPose,
          aprilTags, photon::kAprilTag36h11, headingFree,
          wpi::math::Rotation2d{
              headingBuffer.Sample(cameraResult.GetTimestamp()).value()},
          headingScaleFactor);

  if (!pnpResult) {
    return std::nullopt;
  }

  wpi::math::Pose3d best = wpi::math::Pose3d{} + pnpResult->best;

  return EstimatedRobotPose{best, cameraResult.GetTimestamp(),
                            cameraResult.GetTargets(),
                            PoseStrategy::CONSTRAINED_SOLVEPNP};
}
}  // namespace photon
