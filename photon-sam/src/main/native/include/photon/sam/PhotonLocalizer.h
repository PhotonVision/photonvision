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

#pragma once

#include <vector>
#include <memory>

#include <gtsam/geometry/Cal3_S2.h>
#include <gtsam/slam/expressions.h>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/math/linalg/EigenCore.hpp>
#include <wpi/system/Notifier.hpp>

#include "photon/sam/ConcurrentLocalizer.h"
#include "photon/targeting/PhotonPipelineResult.h"

namespace photon {

struct GTSAMPoseEstimate {
  wpi::math::Pose3d pose;
  uint64_t timestamp;
};

struct GTSAMCamConfig {
  gtsam::Pose3 robotToCamera;
  gtsam::Cal3_S2_ cameraCal;
  gtsam::SharedNoiseModel pixelNoise;  // Isotropic or robust noise model. TODO:
                                       // Make robust? ts needs to be tested

  explicit GTSAMCamConfig(gtsam::Pose3 robotToCamera_gtsam,
                          gtsam::Cal3_S2_ cameraCal_gtsam,
                          gtsam::SharedNoiseModel pixelNoise_gtsam)
      : robotToCamera(robotToCamera_gtsam),
        cameraCal(cameraCal_gtsam),
        pixelNoise(pixelNoise_gtsam) {}

  // For convenience, allow construction from WPILIB types
  explicit GTSAMCamConfig(wpi::math::Transform3d robotToCamera_wpi,
                          wpi::math::Vectord<5> cameraCal_wpi, double sigma)
      : GTSAMCamConfig(sam::Transform3dToGtsamPose3(robotToCamera_wpi),
                       gtsam::Cal3_S2(cameraCal_wpi),
                       gtsam::noiseModel::Isotropic::Sigma(2, sigma)) {}
};

class PhotonLocalizer {
 public:
  PhotonLocalizer(const wpi::fields::Field& layout,
                  const TargetModel& tagModel);

  void Start();
  void Stop();

  void SetOdomNoise(wpi::math::Vectord<6> sigmas);  // Diagonal noise model

  void SubmitReset(wpi::math::Pose3d pose, uint64_t timeUs);
  void SubmitOdometry(wpi::math::Transform3d poseDelta, uint64_t timeUs);
  void SubmitPipelineResult(PhotonPipelineResult result,
                            const GTSAMCamConfig& camConfig);
  GTSAMPoseEstimate GetLatestPoseEstimate() const;
  wpi::math::Vectord<6> GetPoseStdDevs() const;

 private:
  std::unique_ptr<photon::sam::ConcurrentLocalizer> core;
  wpi::Notifier notifier;
  gtsam::SharedNoiseModel odomNoise;  // Should be diagonal. TODO: Make robust?
                                      // ts needs to be tested
};
}  // namespace photon
