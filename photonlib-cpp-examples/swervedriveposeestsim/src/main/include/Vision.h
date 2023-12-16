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

#include <photon/PhotonCamera.h>
#include <photon/PhotonPoseEstimator.h>
#include <photon/estimation/VisionEstimation.h>
#include <photon/simulation/VisionSystemSim.h>
#include <photon/simulation/VisionTargetSim.h>
#include <photon/targeting/PhotonPipelineResult.h>

#include <limits>
#include <memory>

#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/apriltag/AprilTagFields.h>

#include "Constants.h"

class Vision {
 public:
  Vision() {
    photonEstimator.SetMultiTagFallbackStrategy(
        photon::PoseStrategy::LOWEST_AMBIGUITY);

    if (frc::RobotBase::IsSimulation()) {
      visionSim = std::make_unique<photon::VisionSystemSim>("main");

      visionSim->AddAprilTags(constants::Vision::kTagLayout);

      cameraProp = std::make_unique<photon::SimCameraProperties>();

      cameraProp->SetCalibration(960, 720, frc::Rotation2d{90_deg});
      cameraProp->SetCalibError(.35, .10);
      cameraProp->SetFPS(15_Hz);
      cameraProp->SetAvgLatency(50_ms);
      cameraProp->SetLatencyStdDev(15_ms);

      cameraSim = std::make_shared<photon::PhotonCameraSim>(camera.get(),
                                                            *cameraProp.get());

      visionSim->AddCamera(cameraSim.get(), robotToCam);
      cameraSim->EnableDrawWireframe(true);
    }
  }

  photon::PhotonPipelineResult GetLatestResult() {
    return camera->GetLatestResult();
  }

  std::optional<photon::EstimatedRobotPose> GetEstimatedGlobalPose() {
    auto visionEst = photonEstimator.Update();
    units::second_t latestTimestamp = camera->GetLatestResult().GetTimestamp();
    bool newResult =
        units::math::abs(latestTimestamp - lastEstTimestamp) > 1e-5_s;
    if (frc::RobotBase::IsSimulation()) {
      if (visionEst.has_value()) {
        GetSimDebugField()
            .GetObject("VisionEstimation")
            ->SetPose(visionEst.value().estimatedPose.ToPose2d());
      } else {
        if (newResult) {
          GetSimDebugField().GetObject("VisionEstimation")->SetPoses({});
        }
      }
    }
    if (newResult) {
      lastEstTimestamp = latestTimestamp;
    }
    return visionEst;
  }

  Eigen::Matrix<double, 3, 1> GetEstimationStdDevs(frc::Pose2d estimatedPose) {
    Eigen::Matrix<double, 3, 1> estStdDevs =
        constants::Vision::kSingleTagStdDevs;
    auto targets = GetLatestResult().GetTargets();
    int numTags = 0;
    units::meter_t avgDist = 0_m;
    for (const auto& tgt : targets) {
      auto tagPose =
          photonEstimator.GetFieldLayout().GetTagPose(tgt.GetFiducialId());
      if (tagPose.has_value()) {
        numTags++;
        avgDist += tagPose.value().ToPose2d().Translation().Distance(
            estimatedPose.Translation());
      }
    }
    if (numTags == 0) {
      return estStdDevs;
    }
    avgDist /= numTags;
    if (numTags > 1) {
      estStdDevs = constants::Vision::kMultiTagStdDevs;
    }
    if (numTags == 1 && avgDist > 4_m) {
      estStdDevs = (Eigen::MatrixXd(3, 1) << std::numeric_limits<double>::max(),
                    std::numeric_limits<double>::max(),
                    std::numeric_limits<double>::max())
                       .finished();
    } else {
      estStdDevs = estStdDevs * (1 + (avgDist.value() * avgDist.value() / 30));
    }
    return estStdDevs;
  }

  void SimPeriodic(frc::Pose2d robotSimPose) {
    visionSim->Update(robotSimPose);
  }

  void ResetSimPose(frc::Pose2d pose) {
    if (frc::RobotBase::IsSimulation()) {
      visionSim->ResetRobotPose(pose);
    }
  }

  frc::Field2d& GetSimDebugField() { return visionSim->GetDebugField(); }

 private:
  frc::Transform3d robotToCam{frc::Translation3d{0.5_m, 0.5_m, 0.5_m},
                              frc::Rotation3d{}};
  photon::PhotonPoseEstimator photonEstimator{
      LoadAprilTagLayoutField(frc::AprilTagField::k2023ChargedUp),
      photon::PoseStrategy::MULTI_TAG_PNP_ON_COPROCESSOR,
      photon::PhotonCamera{"photonvision"}, robotToCam};
  std::shared_ptr<photon::PhotonCamera> camera{photonEstimator.GetCamera()};
  std::unique_ptr<photon::VisionSystemSim> visionSim;
  std::unique_ptr<photon::SimCameraProperties> cameraProp;
  std::shared_ptr<photon::PhotonCameraSim> cameraSim;
  units::second_t lastEstTimestamp{0_s};
};
