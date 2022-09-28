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

#include "photonlib/SimVisionSystem.h"

#include <cmath>

#include <units/angle.h>
#include <units/length.h>
#include <wpi/span.h>

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

    double area = tgt.tgtArea.value() / GetM2PerPx(distAlongGround);

    // 2D yaw mode considers the target as a point, and should ignore target
    // rotation.
    // Photon reports it in the correct robot reference frame.
    // IE: targets to the left of the image should report negative yaw.
    units::degree_t yawAngle = -units::math::atan2(
        camToTargetTrans.Translation().Y(), camToTargetTrans.Translation().X());
    units::degree_t pitchAngle =
        units::math::atan2(distVertical, distAlongGround) - camPitch;

    if (CamCanSeeTarget(distHypot, yawAngle, pitchAngle, area)) {
      PhotonTrackedTarget newTgt = PhotonTrackedTarget(
          yawAngle.value(), pitchAngle.value(), area, 0.0, -1,
          frc::Transform3d(),  // TODO fiducial 3d pose
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}});
      visibleTgtList.push_back(newTgt);
    }
  }

  units::second_t procDelay(0.0);  // Future - tie this to something meaningful
  cam.SubmitProcessedFrame(procDelay,
                           wpi::span<PhotonTrackedTarget>(visibleTgtList));
}

double SimVisionSystem::GetM2PerPx(units::meter_t dist) {
  double heightMPerPx =
      2 * dist.value() * units::math::tan(camVertFOV / 2) / cameraResHeight;
  double widthMPerPx =
      2 * dist.value() * units::math::tan(camHorizFOV / 2) / cameraResWidth;
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
