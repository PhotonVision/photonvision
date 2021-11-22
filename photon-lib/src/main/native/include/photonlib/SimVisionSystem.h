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
