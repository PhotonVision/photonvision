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

#pragma once

#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Transform3d.h>
#include <frc/interpolation/TimeInterpolatableBuffer.h>
#include <opencv2/core/mat.hpp>

#include "photon/PhotonCamera.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

namespace photon {
enum PoseStrategy {
  LOWEST_AMBIGUITY = 0,
  CLOSEST_TO_CAMERA_HEIGHT,
  CLOSEST_TO_REFERENCE_POSE,
  CLOSEST_TO_LAST_POSE,
  AVERAGE_BEST_TARGETS,
  MULTI_TAG_PNP_ON_COPROCESSOR,
  MULTI_TAG_PNP_ON_RIO,
  CONSTRAINED_SOLVEPNP,
  PNP_DISTANCE_TRIG_SOLVE
};

struct ConstrainedSolvepnpParams {
  bool headingFree{false};
  double headingScalingFactor{0.0};
};

struct EstimatedRobotPose {
  /** The estimated pose */
  frc::Pose3d estimatedPose;
  /** The estimated time the frame used to derive the robot pose was taken, in
   * the same timebase as the RoboRIO FPGA Timestamp */
  units::second_t timestamp;

  /** A list of the targets used to compute this pose */
  wpi::SmallVector<PhotonTrackedTarget, 10> targetsUsed;

  /** The strategy actually used to produce this pose */
  PoseStrategy strategy;

  EstimatedRobotPose(frc::Pose3d pose_, units::second_t time_,
                     std::span<const PhotonTrackedTarget> targets,
                     PoseStrategy strategy_)
      : estimatedPose(pose_),
        timestamp(time_),
        targetsUsed(targets.data(), targets.data() + targets.size()),
        strategy(strategy_) {}
};

/**
 * The PhotonPoseEstimator class filters or combines readings from all the
 * fiducials visible at a given timestamp on the field to produce a single robot
 * in field pose, using the strategy set below. Example usage can be found in
 * our apriltagExample example project.
 */
class PhotonPoseEstimator {
 public:
  /**
   * Create a new PhotonPoseEstimator.
   *
   * @param aprilTags A AprilTagFieldLayout linking AprilTag IDs to Pose3ds with
   * respect to the FIRST field.
   * @param robotToCamera Transform3d from the center of the robot to the camera
   * mount positions (ie, robot ➔ camera).
   */
  explicit PhotonPoseEstimator(frc::AprilTagFieldLayout aprilTags,
                               frc::Transform3d robotToCamera);

  /**
   * Create a new PhotonPoseEstimator.
   *
   * @param aprilTags A AprilTagFieldLayout linking AprilTag IDs to Pose3ds with
   * respect to the FIRST field.
   * @param strategy The strategy it should use to determine the best pose.
   * @param robotToCamera Transform3d from the center of the robot to the camera
   * mount positions (ie, robot ➔ camera).
   * @deprecated Use individual estimation methods with the 2 argument
   * constructor instead.
   */
  [[deprecated(
      "Use individual estimation methods with the 2 argument constructor "
      "instead.")]]
  explicit PhotonPoseEstimator(frc::AprilTagFieldLayout aprilTags,
                               PoseStrategy strategy,
                               frc::Transform3d robotToCamera);

  /**
   * Get the AprilTagFieldLayout being used by the PositionEstimator.
   *
   * @return the AprilTagFieldLayout
   */
  frc::AprilTagFieldLayout GetFieldLayout() const { return aprilTags; }

  /**
   * Get the Position Estimation Strategy being used by the Position Estimator.
   *
   * @return the strategy
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  PoseStrategy GetPoseStrategy() const {
    return strategy;
  }

  /**
   * Set the Position Estimation Strategy used by the Position Estimator.
   *
   * @param strategy the strategy to set
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  inline void SetPoseStrategy(PoseStrategy strat) {
    if (strategy != strat) {
      InvalidatePoseCache();
    }
    strategy = strat;
  }

  /**
   * Set the Position Estimation Strategy used in multi-tag mode when
   * only one tag can be seen. Must NOT be MULTI_TAG_PNP
   *
   * @param strategy the strategy to set
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  void SetMultiTagFallbackStrategy(PoseStrategy strategy);

  /**
   * Return the reference position that is being used by the estimator.
   *
   * @return the referencePose
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  frc::Pose3d GetReferencePose() const {
    return referencePose;
  }

  /**
   * Update the stored reference pose for use when using the
   * CLOSEST_TO_REFERENCE_POSE strategy.
   *
   * @param referencePose the referencePose to set
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  inline void SetReferencePose(frc::Pose3d referencePose) {
    if (this->referencePose != referencePose) {
      InvalidatePoseCache();
    }
    this->referencePose = referencePose;
  }

  /**
   * @return The current transform from the center of the robot to the camera
   *         mount position.
   */
  inline frc::Transform3d GetRobotToCameraTransform() {
    return m_robotToCamera;
  }

  /**
   * Useful for pan and tilt mechanisms, or cameras on turrets
   *
   * @param robotToCamera The current transform from the center of the robot to
   * the camera mount position.
   */
  inline void SetRobotToCameraTransform(frc::Transform3d robotToCamera) {
    m_robotToCamera = robotToCamera;
  }

  /**
   * Update the stored last pose. Useful for setting the initial estimate when
   * using the CLOSEST_TO_LAST_POSE strategy.
   *
   * @param lastPose the lastPose to set
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  inline void SetLastPose(frc::Pose3d lastPose) {
    this->lastPose = lastPose;
  }

  /**
   * Add robot heading data to the buffer. Must be called periodically for the
   * PNP_DISTANCE_TRIG_SOLVE strategy.
   *
   * @param timestamp Timestamp of the robot heading data.
   * @param heading Field-relative heading at the given timestamp. Standard
   * WPILIB field coordinates.
   */
  inline void AddHeadingData(units::second_t timestamp,
                             frc::Rotation2d heading) {
    this->headingBuffer.AddSample(timestamp, heading);
  }

  /**
   * Add robot heading data to the buffer. Must be called periodically for the
   * PNP_DISTANCE_TRIG_SOLVE strategy.
   *
   * @param timestamp Timestamp of the robot heading data.
   * @param heading Field-relative heading at the given timestamp. Standard
   * WPILIB coordinates.
   */
  inline void AddHeadingData(units::second_t timestamp,
                             frc::Rotation3d heading) {
    AddHeadingData(timestamp, heading.ToRotation2d());
  }

  /**
   * Clears all heading data in the buffer, and adds a new seed. Useful for
   * preventing estimates from utilizing heading data provided prior to a pose
   * or rotation reset.
   *
   * @param timestamp Timestamp of the robot heading data.
   * @param heading Field-relative robot heading at given timestamp. Standard
   * WPILIB field coordinates.
   */
  inline void ResetHeadingData(units::second_t timestamp,
                               frc::Rotation2d heading) {
    headingBuffer.Clear();
    AddHeadingData(timestamp, heading);
  }

  /**
   * Clears all heading data in the buffer, and adds a new seed. Useful for
   * preventing estimates from utilizing heading data provided prior to a pose
   * or rotation reset.
   *
   * @param timestamp Timestamp of the robot heading data.
   * @param heading Field-relative robot heading at given timestamp. Standard
   * WPILIB field coordinates.
   */
  inline void ResetHeadingData(units::second_t timestamp,
                               frc::Rotation3d heading) {
    ResetHeadingData(timestamp, heading.ToRotation2d());
  }

  /**
   * Update the pose estimator. If updating multiple times per loop, you should
   * call this exactly once per new result, in order of increasing result
   * timestamp.
   *
   * @param result The vision targeting result to process
   * @param cameraIntrinsics The camera calibration pinhole coefficients matrix.
   * Only required if doing multitag-on-rio, and may be nullopt otherwise.
   * @param distCoeffsData The camera calibration distortion coefficients. Only
   * required if doing multitag-on-rio, and may be nullopt otherwise.
   * @param constrainedPnpParams Constrained SolvePNP params, if needed.
   * @deprecated Use individual estimation methods instead.
   */
  [[deprecated("Use individual estimation methods instead.")]]
  std::optional<EstimatedRobotPose> Update(
      const photon::PhotonPipelineResult& result,
      std::optional<photon::PhotonCamera::CameraMatrix> cameraMatrixData =
          std::nullopt,
      std::optional<photon::PhotonCamera::DistortionMatrix> coeffsData =
          std::nullopt,
      std::optional<ConstrainedSolvepnpParams> constrainedPnpParams =
          std::nullopt);

  /**
   * Return the estimated position of the robot with the lowest position
   * ambiguity from a List of pipeline results.
   *
   * @param cameraResult A pipeline result from the camera.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateLowestAmbiguityPose(
      PhotonPipelineResult cameraResult);

  /**
   * Return the estimated position of the robot using the target with the lowest
   * delta height difference between the estimated and actual height of the
   * camera.
   *
   * @param cameraResult A pipeline result from the camera.
   * @return An EstimatedRobotPose with an estimated pose, timestamp and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateClosestToCameraHeightPose(
      PhotonPipelineResult cameraResult);

  /**
   * Return the estimated position of the robot using the target with the lowest
   * delta in the vector magnitude between it and the reference pose.
   *
   * @param cameraResult A pipeline result from the camera.
   * @param referencePose reference pose to check vector magnitude difference
   * against.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateClosestToReferencePose(
      PhotonPipelineResult cameraResult, frc::Pose3d referencePose);

  /**
   * Return the estimated position of the robot by using all visible tags to
   * compute a single pose estimate on coprocessor. This option needs to be
   * enabled on the PhotonVision web UI as well.
   *
   * @param cameraResult A pipeline result from the camera.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateCoprocMultiTagPose(
      PhotonPipelineResult cameraResult);

  /**
   * Return the estimated position of the robot by using all visible tags to
   * compute a single pose estimate on the RoboRIO. This can take a lot of time
   * due to the RIO's weak computing power.
   *
   * @param cameraResult A pipeline result from the camera.
   * @param cameraMatrix Camera intrinsics from camera calibration data.
   * @param distCoeffs Distortion coefficients from camera calibration data.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateRioMultiTagPose(
      PhotonPipelineResult cameraResult, PhotonCamera::CameraMatrix camMat,
      PhotonCamera::DistortionMatrix distCoeffs);

  /**
   * Return the estimated position of the robot by using distance data from best
   * visible tag to compute a Pose. This runs on the RoboRIO in order to access
   * the robot's yaw heading, and MUST have AddHeadingData called every frame so
   * heading data is up-to-date.
   *
   * <p>Yields a Pose2d in estimatedRobotPose (0 for z, roll, pitch)
   *
   * <p>https://www.chiefdelphi.com/t/frc-6328-mechanical-advantage-2025-build-thread/477314/98
   *
   * @param cameraResult A pipeline result from the camera.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimatePnpDistanceTrigSolvePose(
      PhotonPipelineResult cameraResult);

  /**
   * Return the average of the best target poses using ambiguity as weight.
   * @param cameraResult A pipeline result from the camera.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateAverageBestTargetsPose(
      PhotonPipelineResult cameraResult);

  /**
   * Return the estimated position of the robot by solving a constrained version
   * of the Perspective-n-Point problem with the robot's drivebase flat on the
   * floor. This computation takes place on the RoboRIO, and typically takes not
   * more than 2ms. See
   * photon::VisionEstimation::EstimateRobotPoseConstrainedSolvePNP for tuning
   * handles this strategy exposes. Internally, the cost function is a
   * sum-squared of pixel reprojection error + (optionally) heading error *
   * heading scale factor. This strategy needs addHeadingData called every frame
   * so heading data is up-to-date.
   *
   * @param cameraResult A pipeline result from the camera.
   * @param cameraMatrix Camera intrinsics from camera calibration data.
   * @param distCoeffs Distortion coefficients from camera calibration data.
   * @param seedPose An initial guess at robot pose, refined via optimization.
   * Better guesses will converge faster. Can come from any pose source, but
   * some battle-tested sources are estimateCoprocMultiTagPose, or
   * estimateLowestAmbiguityPose if MultiTag results aren't available.
   * @param headingFree If true, heading is completely free to vary. If false,
   * heading excursions from the provided heading measurement will be penalized
   * @param headingScaleFactor If headingFree is false, this weights the cost of
   * changing our robot heading estimate against the tag corner reprojection
   * error cost.
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> EstimateConstrainedSolvepnpPose(
      photon::PhotonPipelineResult cameraResult,
      photon::PhotonCamera::CameraMatrix cameraMatrix,
      photon::PhotonCamera::DistortionMatrix distCoeffs, frc::Pose3d seedPose,
      bool headingFree, double headingScaleFactor);

 private:
  frc::AprilTagFieldLayout aprilTags;
  PoseStrategy strategy;
  PoseStrategy multiTagFallbackStrategy = LOWEST_AMBIGUITY;

  frc::Transform3d m_robotToCamera;

  frc::Pose3d lastPose;
  frc::Pose3d referencePose;

  units::second_t poseCacheTimestamp;

  frc::TimeInterpolatableBuffer<frc::Rotation2d> headingBuffer;

  inline static int InstanceCount = 1;

  inline void InvalidatePoseCache() { poseCacheTimestamp = -1_s; }

  /**
   * Internal convenience method for using a fallback strategy for update().
   * This should only be called after timestamp checks have been done by another
   * update() overload.
   *
   * @param cameraResult A pipeline result from the camera.
   * @param strategy The pose strategy to use
   * @return An EstimatedRobotPose with an estimated pose, timestamp, and
   * targets used to create the estimate.
   */
  std::optional<EstimatedRobotPose> Update(const PhotonPipelineResult& result,
                                           PoseStrategy strategy) {
    return Update(result, std::nullopt, std::nullopt, std::nullopt, strategy);
  }

  std::optional<EstimatedRobotPose> Update(
      const PhotonPipelineResult& result,
      std::optional<PhotonCamera::CameraMatrix> cameraMatrixData,
      std::optional<PhotonCamera::DistortionMatrix> coeffsData,
      std::optional<ConstrainedSolvepnpParams> constrainedPnpParams,
      PoseStrategy strategy);
};

}  // namespace photon
