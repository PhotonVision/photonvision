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

#include "photonlib/SimVisionSystem.h"

#include <cmath>

#include <units/angle.h>
#include <units/length.h>

namespace photonlib {

SimVisionSystem::SimVisionSystem(const std::string& name,
                                 units::degree_t camDiagFOV,
                                 units::degree_t camPitch,
                                 frc::Transform2d cameraToRobot,
                                 units::meter_t cameraHeightOffGround,
                                 units::meter_t maxLEDRange, int cameraResWidth,
                                 int cameraResHeight, double minTargetArea)
    : camPitch(camPitch),
      cameraToRobot(cameraToRobot),
      cameraHeightOffGround(cameraHeightOffGround),
      maxLEDRange(maxLEDRange),
      cameraResWidth(cameraResWidth),
      cameraResHeight(cameraResHeight),
      minTargetArea(minTargetArea) {
  double hypotPixels = std::hypot(cameraResWidth, cameraResHeight);
  camHorizFOV = camDiagFOV * cameraResWidth / hypotPixels;
  camVertFOV = camDiagFOV * cameraResHeight / hypotPixels;

  cam = SimPhotonCamera(name);
  tgtList.clear();
}

void SimVisionSystem::AddSimVisionTarget(SimVisionTarget tgt) {
  tgtList.push_back(tgt);
}

void SimVisionSystem::MoveCamera(frc::Transform2d newCameraToRobot,
                                 units::meter_t newCamHeight,
                                 units::degree_t newCamPitch) {
  cameraToRobot = newCameraToRobot;
  cameraHeightOffGround = newCamHeight;
  camPitch = newCamPitch;
}

void SimVisionSystem::ProcessFrame(frc::Pose2d robotPose) {
  frc::Pose2d cameraPos = robotPose.TransformBy(cameraToRobot.Inverse());
  std::vector<PhotonTrackedTarget> visibleTgtList = {};

  for (auto&& tgt : tgtList) {
    frc::Transform2d camToTargetTrans =
        frc::Transform2d(cameraPos, tgt.targetPos);

    units::meter_t distAlongGround = camToTargetTrans.Translation().Norm();
    units::meter_t distVertical =
        tgt.targetHeightAboveGround - cameraHeightOffGround;
    units::meter_t distHypot =
        units::math::hypot(distAlongGround, distVertical);

    double area = tgt.tgtArea.to<double>() / GetM2PerPx(distAlongGround);

    // 2D yaw mode considers the target as a point, and should ignore target
    // rotation.
    // Photon reports it in the correct robot reference frame.
    // IE: targets to the left of the image should report negative yaw.
    units::degree_t yawAngle =
        -1.0 * units::math::atan2(camToTargetTrans.Translation().Y(),
                                  camToTargetTrans.Translation().X());
    units::degree_t pitchAngle =
        units::math::atan2(distVertical, distAlongGround) - camPitch;

    if (CamCanSeeTarget(distHypot, yawAngle, pitchAngle, area)) {
      PhotonTrackedTarget newTgt =
          PhotonTrackedTarget(yawAngle.to<double>(), pitchAngle.to<double>(),
                              area, 0.0, camToTargetTrans);
      visibleTgtList.push_back(newTgt);
    }
  }

  units::second_t procDelay(0.0);  // Future - tie this to something meaningful
  cam.SubmitProcessedFrame(
      procDelay, wpi::MutableArrayRef<PhotonTrackedTarget>(visibleTgtList));
}

double SimVisionSystem::GetM2PerPx(units::meter_t dist) {
  double heightMPerPx = 2 * dist.to<double>() *
                        units::math::tan(camVertFOV / 2) / cameraResHeight;
  double widthMPerPx = 2 * dist.to<double>() *
                       units::math::tan(camHorizFOV / 2) / cameraResWidth;
  return widthMPerPx * heightMPerPx;
}

bool SimVisionSystem::CamCanSeeTarget(units::meter_t distHypot,
                                      units::degree_t yaw,
                                      units::degree_t pitch, double area) {
  bool inRange = (distHypot < maxLEDRange);
  bool inHorizAngle = units::math::abs(yaw) < (camHorizFOV / 2);
  bool inVertAngle = units::math::abs(pitch) < (camVertFOV / 2);
  bool targetBigEnough = area > minTargetArea;
  return (inRange && inHorizAngle && inVertAngle && targetBigEnough);
}

}  // namespace photonlib
