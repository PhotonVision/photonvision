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

#include <limits>
#include <optional>
#include <utility>
#include <vector>

#include <Eigen/Core>
#include <opencv2/calib3d.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/core/types.hpp>
#include <wpi/deprecated.h>
#include <wpi/hal/UsageReporting.h>
#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Rotation3d.hpp>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/system/Errors.hpp>
#include <wpi/units/angle.hpp>
#include <wpi/units/math.hpp>
#include <wpi/units/time.hpp>

#include "photon/PhotonCamera.h"
#include "photon/estimation/TargetModel.h"
#include "photon/estimation/VisionEstimation.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

#define OPENCV_DISABLE_EIGEN_TENSOR_SUPPORT
#include <opencv2/core/eigen.hpp>
WPI_IGNORE_DEPRECATED
namespace photon {

namespace detail {
cv::Point3d ToPoint3d(const wpi::math::Translation3d& translation);
std::optional<std::array<cv::Point3d, 4>> CalcTagCorners(
    int tagID, const wpi::apriltag::AprilTagFieldLayout& aprilTags);
wpi::math::Pose3d ToPose3d(const cv::Mat& tvec, const cv::Mat& rvec);
cv::Point3d TagCornerToObjectPoint(wpi::units::meter_t cornerX,
                                   wpi::units::meter_t cornerY,
                                   wpi::math::Pose3d tagPose);
}  // namespace detail

PhotonPoseEstimator::PhotonPoseEstimator(
    wpi::apriltag::AprilTagFieldLayout tags,
    wpi::math::Transform3d robotToCamera)
    : aprilTags(tags),
      m_robotToCamera(robotToCamera),
      lastPose(wpi::math::Pose3d()),
      referencePose(wpi::math::Pose3d()),
      poseCacheTimestamp(-1_s),
      headingBuffer(
          wpi::math::TimeInterpolatableBuffer<wpi::math::Rotation2d>(1_s)) {
  HAL_Report(HALUsageReporting::kResourceType_PhotonPoseEstimator,
             InstanceCount);
  InstanceCount++;
}

PhotonPoseEstimator::PhotonPoseEstimator(
    wpi::apriltag::AprilTagFieldLayout tags, PoseStrategy strat,
    wpi::math::Transform3d robotToCamera)
    : aprilTags(tags),
      strategy(strat),
      m_robotToCamera(robotToCamera),
      lastPose(wpi::math::Pose3d()),
      referencePose(wpi::math::Pose3d()),
      poseCacheTimestamp(-1_s),
      headingBuffer(
          wpi::math::TimeInterpolatableBuffer<wpi::math::Rotation2d>(1_s)) {
  InstanceCount++;
  HAL_ReportUsage("PhotonVision/PhotonPoseEstimator", InstanceCount, "");
}

void PhotonPoseEstimator::SetMultiTagFallbackStrategy(PoseStrategy strategy) {
  if (strategy == MULTI_TAG_PNP_ON_COPROCESSOR ||
      strategy == MULTI_TAG_PNP_ON_RIO) {
    WPILIB_ReportError(
        wpi::warn::Warning,
        "Fallback cannot be set to MULTI_TAG_PNP! Setting to lowest ambiguity",
        "");
    strategy = LOWEST_AMBIGUITY;
  }
  if (this->multiTagFallbackStrategy != strategy) {
    InvalidatePoseCache();
  }
  multiTagFallbackStrategy = strategy;
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update(
    const PhotonPipelineResult& result,
    std::optional<PhotonCamera::CameraMatrix> cameraMatrixData,
    std::optional<PhotonCamera::DistortionMatrix> cameraDistCoeffs,
    std::optional<ConstrainedSolvepnpParams> constrainedPnpParams) {
  // Time in the past -- give up, since the following if expects times > 0
  if (result.GetTimestamp() < 0_s) {
    WPILIB_ReportError(wpi::warn::Warning,
                       "Result timestamp was reported in the past!");
    return std::nullopt;
  }

  // If the pose cache timestamp was set, and the result is from the same
  // timestamp, return an empty result
  if (poseCacheTimestamp > 0_s &&
      wpi::units::math::abs(poseCacheTimestamp - result.GetTimestamp()) <
          0.001_ms) {
    return std::nullopt;
  }

  // Remember the timestamp of the current result used
  poseCacheTimestamp = result.GetTimestamp();

  // If no targets seen, trivial case -- return empty result
  if (!result.HasTargets()) {
    return std::nullopt;
  }

  return Update(result, cameraMatrixData, cameraDistCoeffs,
                constrainedPnpParams, this->strategy);
}

std::optional<EstimatedRobotPose> PhotonPoseEstimator::Update(
    const PhotonPipelineResult& result,
    std::optional<PhotonCamera::CameraMatrix> cameraMatrixData,
    std::optional<PhotonCamera::DistortionMatrix> cameraDistCoeffs,
    std::optional<ConstrainedSolvepnpParams> constrainedPnpParams,
    PoseStrategy strategy) {
  std::optional<EstimatedRobotPose> ret = std::nullopt;

  switch (strategy) {
    case LOWEST_AMBIGUITY:
      ret = EstimateLowestAmbiguityPose(result);
      break;
    case CLOSEST_TO_CAMERA_HEIGHT:
      ret = EstimateClosestToCameraHeightPose(result);
      break;
    case CLOSEST_TO_REFERENCE_POSE:
      ret = EstimateClosestToReferencePose(result, this->referencePose);
      break;
    case CLOSEST_TO_LAST_POSE:
      SetReferencePose(lastPose);
      ret = EstimateClosestToReferencePose(result, this->referencePose);
      break;
    case AVERAGE_BEST_TARGETS:
      ret = EstimateAverageBestTargetsPose(result);
      break;
    case MULTI_TAG_PNP_ON_COPROCESSOR:
      if (!result.MultiTagResult()) {
        ret = Update(result, this->multiTagFallbackStrategy);
      } else {
        ret = EstimateCoprocMultiTagPose(result);
      }
      break;
    case MULTI_TAG_PNP_ON_RIO:
      if (!cameraMatrixData && !cameraDistCoeffs) {
        WPILIB_ReportError(
            wpi::warn::Warning,
            "No camera calibration provided to multi-tag-on-rio!", "");
        ret = Update(result, this->multiTagFallbackStrategy);
      }
      ret =
          EstimateRioMultiTagPose(result, *cameraMatrixData, *cameraDistCoeffs);
      if (!ret) {
        ret = Update(result, this->multiTagFallbackStrategy);
      }
      break;
    case CONSTRAINED_SOLVEPNP: {
      using namespace frc;

      if (!cameraMatrixData || !cameraDistCoeffs) {
        WPILib_ReportError(
            wpi::warn::Warning,
            "No camera calibration data provided for Constrained SolvePnP!");
        ret = Update(result, this->multiTagFallbackStrategy);
        break;
      }

      if (!constrainedPnpParams) {
        return {};
      }

      if (!constrainedPnpParams->headingFree &&
          !headingBuffer.Sample(result.GetTimestamp()).has_value()) {
        ret = Update(result, cameraMatrixData, cameraDistCoeffs, {},
                     this->multiTagFallbackStrategy);
        break;
      }

      wpi::math::Pose3d fieldToRobotSeed;

      if (result.MultiTagResult().has_value()) {
        fieldToRobotSeed =
            wpi::math::Pose3d{} + (result.MultiTagResult()->estimatedPose.best +
                                   m_robotToCamera.Inverse());
      } else {
        std::optional<EstimatedRobotPose> nestedUpdate =
            Update(result, cameraMatrixData, cameraDistCoeffs, {},
                   this->multiTagFallbackStrategy);

        if (!nestedUpdate.has_value()) {
          return {};
        }

        fieldToRobotSeed = nestedUpdate->estimatedPose;
      }

      ret = EstimateConstrainedSolvepnpPose(
          result, *cameraMatrixData, *cameraDistCoeffs, fieldToRobotSeed,
          constrainedPnpParams->headingFree,
          constrainedPnpParams->headingScalingFactor);

      if (!ret) {
        ret = Update(result, cameraMatrixData, cameraDistCoeffs, {},
                     this->multiTagFallbackStrategy);
      }
      break;
    }
    case PNP_DISTANCE_TRIG_SOLVE:
      ret = EstimatePnpDistanceTrigSolvePose(result);
      break;
    default:
      WPILIB_ReportError(wpi::warn::Warning, "Invalid Pose Strategy selected!",
                         "");
      ret = std::nullopt;
  }

  if (ret) {
    lastPose = ret->estimatedPose;
  }
  return ret;
}

bool ShouldEstimate(const PhotonPipelineResult& result) {
  // Time in the past -- give up, since the following if expects times > 0
  if (result.GetTimestamp() < 0_s) {
    WPILib_ReportError(wpi::warn::Warning,
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
    if (it->GetPoseAmbiguity() < lowestAmbiguityScore) {
      foundIt = it;
      lowestAmbiguityScore = it->GetPoseAmbiguity();
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

  return EstimatedRobotPose{
      fiducialPose->TransformBy(bestTarget.GetBestCameraToTarget().Inverse())
          .TransformBy(m_robotToCamera.Inverse()),
      cameraResult.GetTimestamp(), cameraResult.GetTargets(), LOWEST_AMBIGUITY};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateClosestToCameraHeightPose(
    PhotonPipelineResult cameraResult) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  wpi::units::meter_t smallestHeightDifference =
      wpi::units::meter_t(std::numeric_limits<double>::infinity());

  std::optional<EstimatedRobotPose> pose = std::nullopt;

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
      pose = EstimatedRobotPose{
          targetPose.TransformBy(target.GetAlternateCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          cameraResult.GetTimestamp(), cameraResult.GetTargets(),
          CLOSEST_TO_CAMERA_HEIGHT};
    }
    if (bestDifference < smallestHeightDifference) {
      smallestHeightDifference = bestDifference;
      pose = EstimatedRobotPose{
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          cameraResult.GetTimestamp(), cameraResult.GetTargets(),
          CLOSEST_TO_CAMERA_HEIGHT};
    }
  }

  return pose;
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
  wpi::math::Pose3d pose = lastPose;

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
    }

    if (bestDifference < smallestDifference) {
      smallestDifference = bestDifference;
      pose = bestPose;
      stateTimestamp = cameraResult.GetTimestamp();
    }
  }

  return EstimatedRobotPose{pose, stateTimestamp, cameraResult.GetTargets(),
                            CLOSEST_TO_REFERENCE_POSE};
}

std::optional<std::array<cv::Point3d, 4>> detail::CalcTagCorners(
    int tagID, const wpi::apriltag::AprilTagFieldLayout& aprilTags) {
  if (auto tagPose = aprilTags.GetTagPose(tagID); tagPose.has_value()) {
    return std::array{TagCornerToObjectPoint(-3_in, -3_in, *tagPose),
                      TagCornerToObjectPoint(+3_in, -3_in, *tagPose),
                      TagCornerToObjectPoint(+3_in, +3_in, *tagPose),
                      TagCornerToObjectPoint(-3_in, +3_in, *tagPose)};
  } else {
    return std::nullopt;
  }
}

cv::Point3d detail::ToPoint3d(const wpi::math::Translation3d& translation) {
  return cv::Point3d(-translation.Y().value(), -translation.Z().value(),
                     +translation.X().value());
}

cv::Point3d detail::TagCornerToObjectPoint(wpi::units::meter_t cornerX,
                                           wpi::units::meter_t cornerY,
                                           wpi::math::Pose3d tagPose) {
  wpi::math::Translation3d cornerTrans =
      tagPose.Translation() + wpi::math::Translation3d(0.0_m, cornerX, cornerY)
                                  .RotateBy(tagPose.Rotation());
  return ToPoint3d(cornerTrans);
}

wpi::math::Pose3d detail::ToPose3d(const cv::Mat& tvec, const cv::Mat& rvec) {
  using namespace wpi::math;
  using namespace wpi::units;

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

  const auto targets = cameraResult.GetTargets();

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
    return std::nullopt;
  }

  // Output mats for results
  cv::Mat const rvec(3, 1, cv::DataType<double>::type);
  cv::Mat const tvec(3, 1, cv::DataType<double>::type);

  {
    cv::Mat cameraMatCV(cameraMatrix.rows(), cameraMatrix.cols(), CV_64F);
    cv::eigen2cv(cameraMatrix, cameraMatCV);
    cv::Mat distCoeffsMatCV(distCoeffs.rows(), distCoeffs.cols(), CV_64F);
    cv::eigen2cv(distCoeffs, distCoeffsMatCV);

    cv::solvePnP(objectPoints, imagePoints, cameraMatCV, distCoeffsMatCV, rvec,
                 tvec, false, cv::SOLVEPNP_SQPNP);
  }

  const wpi::math::Pose3d pose = detail::ToPose3d(tvec, rvec);

  return photon::EstimatedRobotPose(
      pose.TransformBy(m_robotToCamera.Inverse()), cameraResult.GetTimestamp(),
      cameraResult.GetTargets(), MULTI_TAG_PNP_ON_RIO);
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

  return EstimatedRobotPose{wpi::math::Pose3d(robotPose),
                            cameraResult.GetTimestamp(),
                            cameraResult.GetTargets(), PNP_DISTANCE_TRIG_SOLVE};
}

std::optional<EstimatedRobotPose>
PhotonPoseEstimator::EstimateAverageBestTargetsPose(
    PhotonPipelineResult cameraResult) {
  if (!ShouldEstimate(cameraResult)) {
    return std::nullopt;
  }
  std::vector<
      std::pair<wpi::math::Pose3d, std::pair<double, wpi::units::second_t>>>
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
      return EstimatedRobotPose{
          targetPose.TransformBy(target.GetBestCameraToTarget().Inverse())
              .TransformBy(m_robotToCamera.Inverse()),
          cameraResult.GetTimestamp(), cameraResult.GetTargets(),
          AVERAGE_BEST_TARGETS};
    }
    totalAmbiguity += 1. / target.GetPoseAmbiguity();

    tempPoses.push_back(std::make_pair(
        targetPose.TransformBy(target.GetBestCameraToTarget().Inverse()),
        std::make_pair(target.GetPoseAmbiguity(),
                       cameraResult.GetTimestamp())));
  }

  wpi::math::Translation3d transform = wpi::math::Translation3d();
  wpi::math::Rotation3d rotation = wpi::math::Rotation3d();

  for (std::pair<wpi::math::Pose3d, std::pair<double, wpi::units::second_t>>&
           pair : tempPoses) {
    double const weight = (1. / pair.second.first) / totalAmbiguity;
    transform = transform + pair.first.Translation() * weight;
    rotation = rotation + pair.first.Rotation() * weight;
  }

  return EstimatedRobotPose{wpi::math::Pose3d(transform, rotation),
                            cameraResult.GetTimestamp(),
                            cameraResult.GetTargets(), AVERAGE_BEST_TARGETS};
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
