/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "photon/sam/PhotonLocalizer.h"

#include <algorithm>
#include <ranges>
#include <vector>

#include <gtsam/geometry/Cal3_S2.h>

using namespace photon;

PhotonLocalizer::PhotonLocalizer(const wpi::fields::Field& layout,
                                 const TargetModel& tagModel)
    : core(
          std::make_unique<photon::sam::ConcurrentLocalizer>(layout, tagModel)),
      notifier([this] { this->core->Step(); }) {
  gtsam::Vector6 odomSigma;
  odomSigma << 0.005, 0.005,
      0.002,              // roll, pitch, yaw (rad): gyro yaw is very good
      0.01, 0.01, 0.002;  // x, y, z (m) per step
  odomNoise = gtsam::noiseModel::Diagonal::Sigmas(odomSigma);
}

void PhotonLocalizer::SubmitReset(wpi::math::Pose3d pose, uint64_t timeUs) {
  core->SubmitReset(
      sam::ResetData{sam::Pose3dToGtsamPose3(pose), odomNoise, timeUs});
}

void PhotonLocalizer::SubmitOdometry(wpi::math::Transform3d delta,
                                     uint64_t timeUs) {
  core->SubmitOdometry(sam::OdometryObservation{
      timeUs, sam::Transform3dToGtsamPose3(delta), odomNoise});
}

void PhotonLocalizer::SubmitPipelineResult(PhotonPipelineResult result,
                                           const GTSAMCamConfig& camConfig) {
  for (const PhotonTrackedTarget& target : result.targets) {
    auto v = target.GetDetectedCorners() |
             std::views::transform([](const TargetCorner& corner) {
               return gtsam::Point2{corner.x, corner.y};
             });
    std::vector<gtsam::Point2> gtsam_corners(v.begin(), v.end());
    core->SubmitTagObservation(sam::CameraVisionObservation{
        static_cast<uint64_t>(result.metadata.captureTimestampNanos),
        target.GetFiducialId(), gtsam_corners, camConfig.cameraCal,
        camConfig.robotToCamera, camConfig.pixelNoise});
  }
}

GTSAMPoseEstimate PhotonLocalizer::GetLatestPoseEstimate() const {
  return GTSAMPoseEstimate{sam::GtsamToFrcPose3d(core->GetLatestWorldToBody()),
                           gtsam::symbolIndex(core->GetCurrStateIdx())};
}

wpi::math::Vectord<6> PhotonLocalizer::GetPoseStdDevs() const {
  return core->GetPoseComponentStdDevs();
}

void PhotonLocalizer::SetOdomNoise(wpi::math::Vectord<6> sigmas) {
  odomNoise = gtsam::noiseModel::Diagonal::Sigmas(sigmas);
}

void PhotonLocalizer::Start() {
  notifier.StartPeriodic(wpi::units::second_t(0.02));
}

void PhotonLocalizer::Stop() { notifier.Stop(); }
