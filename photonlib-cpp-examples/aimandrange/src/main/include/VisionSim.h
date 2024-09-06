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

class VisionSim {
 public:
  explicit VisionSim(photon::PhotonCamera* camera) {
    if (frc::RobotBase::IsSimulation()) {
      visionSim = std::make_unique<photon::VisionSystemSim>("main");

      visionSim->AddAprilTags(constants::Vision::kTagLayout);

      cameraProp = std::make_unique<photon::SimCameraProperties>();

      cameraProp->SetCalibration(320, 240, frc::Rotation2d{90_deg});
      cameraProp->SetCalibError(.35, .10);
      cameraProp->SetFPS(70_Hz);
      cameraProp->SetAvgLatency(30_ms);
      cameraProp->SetLatencyStdDev(15_ms);

      cameraSim =
          std::make_shared<photon::PhotonCameraSim>(camera, *cameraProp.get());

      visionSim->AddCamera(cameraSim.get(), constants::Vision::kRobotToCam);
      cameraSim->EnableDrawWireframe(true);
    }
  }

  photon::PhotonPipelineResult GetLatestResult() { return m_latestResult; }

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
  std::unique_ptr<photon::VisionSystemSim> visionSim;
  std::unique_ptr<photon::SimCameraProperties> cameraProp;
  std::shared_ptr<photon::PhotonCameraSim> cameraSim;

  // The most recent result, cached for calculating std devs
  photon::PhotonPipelineResult m_latestResult;
};
