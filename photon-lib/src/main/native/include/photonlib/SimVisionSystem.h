/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

#include <string>
#include <vector>

#include <frc/geometry/Translation2d.h>
#include <units/angle.h>
#include <units/area.h>
#include <units/length.h>
#include <units/time.h>
#include <wpi/SmallVector.h>

#include "photonlib/SimPhotonCamera.h"
#include "photonlib/SimVisionTarget.h"

namespace photonlib {

/**
 * Represents a camera that is connected to PhotonVision.
 */
class SimVisionSystem {
 public:
  explicit SimVisionSystem(const std::string& name, units::degree_t camDiagFOV,
                           units::degree_t camPitch,
                           frc::Transform2d cameraToRobot,
                           units::meter_t cameraHeightOffGround,
                           units::meter_t maxLEDRange, int cameraResWidth,
                           int cameraResHeight, double minTargetArea);

  void AddSimVisionTarget(SimVisionTarget tgt);
  void MoveCamera(frc::Transform2d newcameraToRobot,
                  units::meter_t newCamHeight, units::degree_t newCamPitch);
  void ProcessFrame(frc::Pose2d robotPose);

 private:
  units::degree_t camPitch;
  frc::Transform2d cameraToRobot;
  units::meter_t cameraHeightOffGround;
  units::meter_t maxLEDRange;
  int cameraResWidth;
  int cameraResHeight;
  double minTargetArea;
  units::degree_t camHorizFOV;
  units::degree_t camVertFOV;
  std::vector<SimVisionTarget> tgtList = {};

  double GetM2PerPx(units::meter_t dist);
  bool CamCanSeeTarget(units::meter_t distHypot, units::degree_t yaw,
                       units::degree_t pitch, double area);

 public:
  SimPhotonCamera cam = photonlib::SimPhotonCamera("Default");
};

}  // namespace photonlib
