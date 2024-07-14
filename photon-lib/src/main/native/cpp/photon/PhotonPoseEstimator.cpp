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

#include <hal/FRCUsageReporting.h>

#include <cmath>
#include <iostream>
#include <limits>
#include <map>
#include <span>
#include <string>
#include <utility>
#include <vector>

#include <Eigen/Core>
#include <frc/Errors.h>
#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Transform3d.h>
#include <opencv2/calib3d.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/core/types.hpp>
#include <units/math.h>
#include <units/time.h>

#include "photon/PhotonCamera.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

#define OPENCV_DISABLE_EIGEN_TENSOR_SUPPORT
#include <opencv2/core/eigen.hpp>

namespace photon {

namespace detail {
cv::Point3d ToPoint3d(const frc::Translation3d& translation);
std::optional<std::array<cv::Point3d, 4>> CalcTagCorners(
    int tagID, const frc::AprilTagFieldLayout& aprilTags);
frc::Pose3d ToPose3d(const cv::Mat& tvec, const cv::Mat& rvec);
cv::Point3d TagCornerToObjectPoint(units::meter_t cornerX,
                                   units::meter_t cornerY, frc::Pose3d tagPose);
}  // namespace detail

PhotonPoseEstimator::PhotonPoseEstimator(frc::AprilTagFieldLayout tags,
                                         PoseStrategy strat,
                                         frc::Transform3d robotToCamera)
    : aprilTags(tags),
      strategy(strat),
      camera(nullptr),
      m_robotToCamera(robotToCamera),
      lastPose(frc::Pose3d()),
      referencePose(frc::Pose3d()),
      poseCacheTimestamp(-1_s) {
  HAL_Report(HALUsageReporting::kResourceType_PhotonPoseEstimator,
             InstanceCount);
  InstanceCount++;
}

PhotonPoseEstimator::PhotonPoseEstimator(frc::AprilTagFieldLayout tags,
                                         PoseStrategy strat, PhotonCamera&& cam,
                                         frc::Transform3d robotToCamera)
    : aprilTags(tags),
      strategy(strat),
      camera(std::make_shared<PhotonCamera>(std::move(cam))),
      m_robotToCamera(robotToCamera),
      lastPose(frc::Pose3d()),
      referencePose(frc::Pose3d()),
      poseCacheTimestamp(-1_s) {
  HAL_Report(HALUsageReporting::kResourceType_PhotonPoseEstimator,
             InstanceCount);
  InstanceCount++;
}

void PhotonPoseEstimator::SetMultiTagFallbackStrategy(PoseStrategy strategy) {
  if (strategy == MULTI_TAG_PNP_ON_COPROCESSOR ||
      strategy == MULTI_TAG_PNP_ON_RIO) {
    FRC_ReportError(
        frc::warn::Warning,
        "Fallback cannot be set to MULTI_TAG_PNP! Setting to lowest ambiguity",
        "");
    strategy = LOWEST_AMBIGUITY;
  }
  if (this->multiTagFallbackStrategy != strategy) {
    InvalidatePoseCache();
  }
  multiTagFallbackStrategy = strategy;
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update() {
  if (!camera) {
    FRC_ReportError(frc::warn::Warning, "[PhotonPoseEstimator] Missing camera!",
                    "");
    return std::nullopt;
  }
  auto result = camera->GetLatestResult();
  return Update(result, camera->GetCameraMatrix(), camera->GetDistCoeffs());
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update(
    const PhotonPipelineResult& result) {
  // If camera is null, best we can do is pass null calibration data
  if (!camera) {
    return Update(result, std::nullopt, std::nullopt, this->strategy);
  }
  return Update(result, camera->GetCameraMatrix(), camera->GetDistCoeffs());
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update(
    const PhotonPipelineResult& result,
    std::optional<PhotonCamera::CameraMatrix> cameraMatrixData,
    std::optional<PhotonCamera::DistortionMatrix> cameraDistCoeffs) {
  // Time in the past -- give up, since the following if expects times > 0
  if (result.GetTimestamp() < 0_s) {
    return std::nullopt;
  }

  // If the pose cache timestamp was set, and the result is from the same
  // timestamp, return an empty result
  if (poseCacheTimestamp > 0_s &&
      units::math::abs(poseCacheTimestamp - result.GetTimestamp()) < 0.001_ms) {
    return std::nullopt;
  }

  // Remember the timestamp of the current result used
  poseCacheTimestamp = result.GetTimestamp();

  // If no targets seen, trivial case -- return empty result
  if (!result.HasTargets()) {
    return std::nullopt;
  }

  return Update(result, cameraMatrixData, cameraDistCoeffs, this->strategy);
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update(
    const PhotonPipelineResult& result,
    std::optional<PhotonCamera::CameraMatrix> cameraMatrixData,
    std::optional<PhotonCamera::DistortionMatrix> cameraDistCoeffs,
    PoseStrategy strategy) {
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
    case MULTI_TAG_PNP_ON_COPROCESSOR:
      ret =
          MultiTagOnCoprocStrategy(result, cameraMatrixData, cameraDistCoeffs);
      break;
    case MULTI_TAG_PNP_ON_RIO:
      ret = MultiTagOnRioStrategy(result, cameraMatrixData, cameraDistCoeffs);
      break;
    default:
      FRC_ReportError(frc::warn::Warning, "Invalid Pose Strategy selected!",
                      "");
      ret = std::nullopt;
  }

  if (ret) {
    lastPose = ret.value().estimatedPose;
  }
  return ret;
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::LowestAmbiguityStrategy(
    PhotonPipelineResult result) {
  double lowestAmbiguityScore = std::numeric_limits<double>::infinity();
  auto targets = result.GetTargets();
  auto foundIt = targets.end();
  for (auto it = targets.begin(); it != targets.end(); ++it) {
    if (it->GetPoseAmbiguity() < lowestAmbiguityScore) {
      foundIt = it;
      lowestAmbiguityScore = it->GetPoseAmbiguity();
    }
  }

  if (foundIt == targets.end()) {
    return std::nullopt;
  }

  auto& bestTarget = *foundIt;

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
      result.GetTimestamp(), result.GetTargets(), LOWEST_AMBIGUITY};
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
    frc::Pose3d const targetPose = fiducialPose.value();

    units::meter_t const alternativeDifference = units::math::abs(
        m_robotToCamera.Z() -
        targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
            .Z());

    units::meter_t const bestDifference = units::math::abs(
        m_robotToCamera.Z() -
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()).Z());

    if (alternativeDifference < smallestHeightDifference) {
      smallestHeightDifference = alternativeDifference;
      pose = EstimatedRobotPose{
          targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          result.GetTimestamp(), result.GetTargets(), CLOSEST_TO_CAMERA_HEIGHT};
    }
    if (bestDifference < smallestHeightDifference) {
      smallestHeightDifference = bestDifference;
      pose = EstimatedRobotPose{
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          result.GetTimestamp(), result.GetTargets(), CLOSEST_TO_CAMERA_HEIGHT};
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
  for (auto& target : targets) {
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

    units::meter_t const alternativeDifference = units::math::abs(
        referencePose.Translation().Distance(altPose.Translation()));
    units::meter_t const bestDifference = units::math::abs(
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

  return EstimatedRobotPose{pose, stateTimestamp, result.GetTargets(),
                            CLOSEST_TO_REFERENCE_POSE};
}

std::optional<std::array<cv::Point3d, 4>> detail::CalcTagCorners(
    int tagID, const frc::AprilTagFieldLayout& aprilTags) {
  if (auto tagPose = aprilTags.GetTagPose(tagID); tagPose.has_value()) {
    return std::array{TagCornerToObjectPoint(-3_in, -3_in, *tagPose),
                      TagCornerToObjectPoint(+3_in, -3_in, *tagPose),
                      TagCornerToObjectPoint(+3_in, +3_in, *tagPose),
                      TagCornerToObjectPoint(-3_in, +3_in, *tagPose)};
  } else {
    return std::nullopt;
  }
}

cv::Point3d detail::ToPoint3d(const frc::Translation3d& translation) {
  return cv::Point3d(-translation.Y().value(), -translation.Z().value(),
                     +translation.X().value());
}

cv::Point3d detail::TagCornerToObjectPoint(units::meter_t cornerX,
                                           units::meter_t cornerY,
                                           frc::Pose3d tagPose) {
  frc::Translation3d cornerTrans =
      tagPose.Translation() +
      frc::Translation3d(0.0_m, cornerX, cornerY).RotateBy(tagPose.Rotation());
  return ToPoint3d(cornerTrans);
}

frc::Pose3d detail::ToPose3d(const cv::Mat& tvec, const cv::Mat& rvec) {
  using namespace frc;
  using namespace units;

  cv::Mat R;
  cv::Rodrigues(rvec, R);  // R is 3x3

  R = R.t();                  // rotation of inverse
  cv::Mat tvecI = -R * tvec;  // translation of inverse

  Eigen::Matrix<double, 3, 1> tv;
  tv[0] = +tvecI.at<double>(2, 0);
  tv[1] = -tvecI.at<double>(0, 0);
  tv[2] = -tvecI.at<double>(1, 0);
  Eigen::Matrix<double, 3, 1> rv;
  rv[0] = +rvec.at<double>(2, 0);
  rv[1] = -rvec.at<double>(0, 0);
  rv[2] = +rvec.at<double>(1, 0);

  return Pose3d(Translation3d(meter_t{tv[0]}, meter_t{tv[1]}, meter_t{tv[2]}),
                Rotation3d(rv));
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::MultiTagOnCoprocStrategy(
    PhotonPipelineResult result,
    std::optional<PhotonCamera::CameraMatrix> camMat,
    std::optional<PhotonCamera::DistortionMatrix> distCoeffs) {
  if (result.MultiTagResult()) {
    const auto field2camera = result.MultiTagResult()->estimatedPose.best;

    const auto fieldToRobot =
        frc::Pose3d() + field2camera + m_robotToCamera.Inverse();
    return photon::EstimatedRobotPose(fieldToRobot, result.GetTimestamp(),
                                      result.GetTargets(),
                                      MULTI_TAG_PNP_ON_COPROCESSOR);
  }

  return Update(result, std::nullopt, std::nullopt,
                this->multiTagFallbackStrategy);
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::MultiTagOnRioStrategy(
    PhotonPipelineResult result,
    std::optional<PhotonCamera::CameraMatrix> camMat,
    std::optional<PhotonCamera::DistortionMatrix> distCoeffs) {
  using namespace frc;

  // Need at least 2 targets
  if (!result.HasTargets() || result.GetTargets().size() < 2) {
    return Update(result, std::nullopt, std::nullopt,
                  this->multiTagFallbackStrategy);
  }

  if (!camMat || !distCoeffs) {
    return Update(result, std::nullopt, std::nullopt,
                  this->multiTagFallbackStrategy);
  }

  auto const targets = result.GetTargets();

  // List of corners mapped from 3d space (meters) to the 2d camera screen
  // (pixels).
  std::vector<cv::Point3f> objectPoints;
  std::vector<cv::Point2f> imagePoints;

  // Add all target corners to main list of corners
  for (auto target : targets) {
    int id = target.GetFiducialId();
    if (auto const tagCorners = detail::CalcTagCorners(id, aprilTags);
        tagCorners.has_value()) {
      auto const targetCorners = target.GetDetectedCorners();
      for (size_t cornerIdx = 0; cornerIdx < 4; ++cornerIdx) {
        imagePoints.emplace_back(targetCorners[cornerIdx].x,
                                 targetCorners[cornerIdx].y);
        objectPoints.emplace_back((*tagCorners)[cornerIdx]);
      }
    }
  }

  // We should only do multi-tag if at least 2 tags (* 4 corners/tag)
  if (imagePoints.size() < 8) {
    return Update(result, camMat, distCoeffs, this->multiTagFallbackStrategy);
  }

  // Output mats for results
  cv::Mat const rvec(3, 1, cv::DataType<double>::type);
  cv::Mat const tvec(3, 1, cv::DataType<double>::type);

  {
    cv::Mat cameraMatCV(camMat->rows(), camMat->cols(), CV_64F);
    cv::eigen2cv(*camMat, cameraMatCV);
    cv::Mat distCoeffsMatCV(distCoeffs->rows(), distCoeffs->cols(), CV_64F);
    cv::eigen2cv(*distCoeffs, distCoeffsMatCV);

    cv::solvePnP(objectPoints, imagePoints, cameraMatCV, distCoeffsMatCV, rvec,
                 tvec, false, cv::SOLVEPNP_SQPNP);
  }

  const Pose3d pose = detail::ToPose3d(tvec, rvec);

  return photon::EstimatedRobotPose(pose.TransformBy(m_robotToCamera.Inverse()),
                                    result.GetTimestamp(), result.GetTargets(),
                                    MULTI_TAG_PNP_ON_RIO);
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::AverageBestTargetsStrategy(PhotonPipelineResult result) {
  std::vector<std::pair<frc::Pose3d, std::pair<double, units::second_t>>>
      tempPoses;
  double totalAmbiguity = 0;

  auto targets = result.GetTargets();
  for (auto& target : targets) {
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
          result.GetTimestamp(), result.GetTargets(), AVERAGE_BEST_TARGETS};
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
    double const weight = (1. / pair.second.first) / totalAmbiguity;
    transform = transform + pair.first.Translation() * weight;
    rotation = rotation + pair.first.Rotation() * weight;
  }

  return EstimatedRobotPose{frc::Pose3d(transform, rotation),
                            result.GetTimestamp(), result.GetTargets(),
                            AVERAGE_BEST_TARGETS};
}
}  // namespace photon
