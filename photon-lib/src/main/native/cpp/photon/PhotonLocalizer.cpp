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

#include "photon/PhotonLocalizer.h"

#include <algorithm>
#include <ranges>
#include <vector>

#include <gtsam/geometry/Cal3_S2.h>

using namespace photon;

PhotonLocalizer::PhotonLocalizer(const wpi::fields::Field& layout,
                                 const TargetModel& tagModel)
    : core(layout, tagModel), notifier([this] { this->core.Step(); }) {
  gtsam::Vector6 odomSigma;
  odomSigma << 0.005, 0.005,
      0.002,              // roll, pitch, yaw (rad): gyro yaw is very good
      0.01, 0.01, 0.002;  // x, y, z (m) per step
  odomNoise = gtsam::noiseModel::Diagonal::Sigmas(odomSigma);
}

void PhotonLocalizer::SubmitReset(wpi::math::Pose3d pose, uint64_t timeUs) {
  core.SubmitReset(
      pvgtsam::ResetData{pvgtsam::Pose3dToGtsamPose3(pose), odomNoise, timeUs});
}

void PhotonLocalizer::SubmitOdometry(wpi::math::Transform3d delta,
                                     uint64_t timeUs) {
  core.SubmitOdometry(pvgtsam::OdometryObservation{
      timeUs, pvgtsam::Transform3dToGtsamPose3(delta), odomNoise});
}

void PhotonLocalizer::SubmitPipelineResult(PhotonPipelineResult result,
                                           const GTSAMCamConfig& camConfig) {
  for (const PhotonTrackedTarget& target : result.targets) {
    auto v = target.GetDetectedCorners() |
             std::views::transform([](const TargetCorner& corner) {
               return gtsam::Point2{corner.x, corner.y};
             });
    std::vector<gtsam::Point2> gtsam_corners(v.begin(), v.end());
    core.SubmitTagObservation(pvgtsam::CameraVisionObservation{
        static_cast<uint64_t>(result.metadata.captureTimestampNanos),
        target.GetFiducialId(), gtsam_corners, camConfig.cameraCal,
        camConfig.robotToCamera, camConfig.pixelNoise});
  }
}

GTSAMPoseEstimate PhotonLocalizer::GetLatestPoseEstimate() const {
  return GTSAMPoseEstimate{
      pvgtsam::GtsamToFrcPose3d(core.GetLatestWorldToBody()),
      gtsam::symbolIndex(core.GetCurrStateIdx())};
}

wpi::math::Vectord<6> PhotonLocalizer::GetPoseStdDevs() const {
  return core.GetPoseComponentStdDevs();
}

void PhotonLocalizer::SetOdomNoise(wpi::math::Vectord<6> sigmas) {
  odomNoise = gtsam::noiseModel::Diagonal::Sigmas(sigmas);
}

void PhotonLocalizer::Start() {
  notifier.StartPeriodic(wpi::units::second_t(0.02));
}

void PhotonLocalizer::Stop() { notifier.Stop(); }
