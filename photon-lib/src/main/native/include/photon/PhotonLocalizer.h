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

#include <vector>

#include <gtsam/geometry/Cal3_S2.h>
#include <gtsam/slam/expressions.h>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/math/linalg/EigenCore.hpp>
#include <wpi/system/Notifier.hpp>

#include "photon/gtsam/ConcurrentLocalizer.h"
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
      : GTSAMCamConfig(pvgtsam::Transform3dToGtsamPose3(robotToCamera_wpi),
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
  pvgtsam::ConcurrentLocalizer core;
  wpi::Notifier notifier;
  gtsam::SharedNoiseModel odomNoise;  // Should be diagonal. TODO: Make robust?
                                      // ts needs to be tested
};
}  // namespace photon
