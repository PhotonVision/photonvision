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

#include <frc/smartdashboard/Field2d.h>
#include <frc/smartdashboard/SmartDashboard.h>
#include <units/angle.h>
#include <units/area.h>

#include "SimPhotonCamera.h"
#include "SimVisionTarget.h"

namespace photonlib {
class SimVisionSystem {
 public:
  SimPhotonCamera cam;
  units::radian_t camHorizFOV{0};
  units::radian_t camVertFOV{0};
  units::meter_t maxLEDRange{0};
  int cameraResWidth{0};
  int cameraResHeight{0};
  double minTargetArea{0.0};
  frc::Transform3d cameraToRobot;

  frc::Field2d dbgField;
  frc::FieldObject2d* dbgRobot;
  frc::FieldObject2d* dbgCamera;

  std::vector<SimVisionTarget> targetList;

  /**
   * Create a simulated vision system involving a camera and coprocessor mounted
   * on a mobile robot running PhotonVision, detecting one or more targets
   * scattered around the field. This assumes a fairly simple and
   * distortion-less pinhole camera model.
   *
   * @param camName Name of the PhotonVision camera to create. Align it with the
   * settings you use in the PhotonVision GUI.
   * @param camDiagFOV Diagonal Field of View of the camera used. Align it with
   * the manufacturer specifications, and/or whatever is configured in the
   * PhotonVision Setting page.
   * @param cameraToRobot Transform to move from the camera's mount position to
   * the robot's position
   * @param maxLEDRange Maximum distance at which your camera can illuminate the
   * target and make it visible. Set to 9000 or more if your vision system does
   * not rely on LED's.
   * @param cameraResWidth Width of your camera's image sensor in pixels
   * @param cameraResHeight Height of your camera's image sensor in pixels
   * @param minTargetArea Minimum area that that the target should be before
   * it's recognized as a target by the camera. Match this with your contour
   * filtering settings in the PhotonVision GUI.
   */
  SimVisionSystem(std::string camName, units::degree_t camDiagFOV,
                  frc::Transform3d cameraToRobot, units::meter_t maxLEDRange,
                  int cameraResWidth, int cameraResHeight, double minTargetArea)
      : cam(camName),
        camHorizFOV((camDiagFOV * cameraResWidth) /
                    std::hypot(cameraResWidth, cameraResHeight)),
        camVertFOV((camDiagFOV * cameraResHeight) /
                   std::hypot(cameraResWidth, cameraResHeight)),
        maxLEDRange(maxLEDRange),
        cameraResWidth(cameraResWidth),
        cameraResHeight(cameraResHeight),
        minTargetArea(minTargetArea),
        cameraToRobot(cameraToRobot),
        dbgField(),
        dbgRobot(dbgField.GetRobotObject()),
        dbgCamera(dbgField.GetObject(camName + " Camera")) {
    frc::SmartDashboard::PutData(camName + " Sim Field", &dbgField);
  }

  /**
   * Add a target on the field which your vision system is designed to detect.
   * The PhotonCamera from this system will report the location of the robot
   * relative to the subset of these targets which are visible from the given
   * robot position.
   *
   * @param target Target to add to the simulated field
   */
  void AddSimVisionTarget(SimVisionTarget target) {
    targetList.push_back(target);
    dbgField.GetObject("Target " + std::to_string(target.targetId))
        ->SetPose(target.targetPose.ToPose2d());
  }

  /**
   * Clears all sim vision targets.
   * This is useful for switching alliances and needing to repopulate the sim
   * targets. NOTE: Old targets will still show on the Field2d unless
   * overwritten by new targets with the same ID
   */
  void ClearVisionTargets() { targetList.clear(); }

  /**
   * Adjust the camera position relative to the robot. Use this if your camera
   * is on a gimbal or turret or some other mobile platform.
   *
   * @param newCameraToRobot New Transform from the robot to the camera
   */
  void MoveCamera(frc::Transform3d newCameraToRobot) {
    cameraToRobot = newCameraToRobot;
  }

  /**
   * Periodic update. Call this once per frame of image data you wish to process
   * and send to NetworkTables
   *
   * @param robotPose current pose of the robot on the field. Will be used to
   * calculate which targets are actually in view, where they are at relative to
   * the robot, and relevant PhotonVision parameters.
   */
  void ProcessFrame(frc::Pose2d robotPose) {
    ProcessFrame(frc::Pose3d{
        robotPose.X(), robotPose.Y(), 0.0_m,
        frc::Rotation3d{0_rad, 0_rad, robotPose.Rotation().Radians()}});
  }

  /**
   * Periodic update. Call this once per frame of image data you wish to process
   * and send to NetworkTables
   *
   * @param robotPose current pose of the robot in space. Will be used to
   * calculate which targets are actually in view, where they are at relative to
   * the robot, and relevant PhotonVision parameters.
   */
  void ProcessFrame(frc::Pose3d robotPose) {
    frc::Pose3d cameraPose = robotPose.TransformBy(cameraToRobot.Inverse());
    dbgRobot->SetPose(robotPose.ToPose2d());
    dbgCamera->SetPose(cameraPose.ToPose2d());

    std::vector<PhotonTrackedTarget> visibleTargetList{};

    for (const auto& target : targetList) {
      frc::Transform3d camToTargetTransform{cameraPose, target.targetPose};
      frc::Translation3d camToTargetTranslation{
          camToTargetTransform.Translation()};

      frc::Translation3d altTranslation{camToTargetTranslation.X(),
                                        -1.0 * camToTargetTranslation.Y(),
                                        camToTargetTranslation.Z()};
      frc::Rotation3d altRotation{camToTargetTransform.Rotation() * -1.0};
      frc::Transform3d camToTargetAltTransform{altTranslation, altRotation};
      units::meter_t dist{camToTargetTranslation.Norm()};
      double areaPixels{target.targetArea / GetM2PerPx(dist)};
      units::radian_t yaw{units::math::atan2(camToTargetTranslation.Y(),
                                             camToTargetTranslation.X())};
      units::meter_t cameraHeightOffGround{cameraPose.Z()};
      units::meter_t targetHeightAboveGround(target.targetPose.Z());
      units::radian_t camPitch{cameraPose.Rotation().Y()};
      frc::Transform2d transformAlongGround{cameraPose.ToPose2d(),
                                            target.targetPose.ToPose2d()};
      units::meter_t distanceAlongGround{
          transformAlongGround.Translation().Norm()};
      units::radian_t pitch =
          units::math::atan2(targetHeightAboveGround - cameraHeightOffGround,
                             distanceAlongGround) -
          camPitch;

      if (CamCamSeeTarget(dist, yaw, pitch, areaPixels)) {
        visibleTargetList.push_back(
            PhotonTrackedTarget{yaw.convert<units::degree>().to<double>(),
                                pitch.convert<units::degree>().to<double>(),
                                areaPixels,
                                0.0,
                                target.targetId,
                                camToTargetTransform,
                                // TODO sim alternate pose
                                camToTargetTransform,
                                // TODO ambiguity
                                0.0,
                                {{0, 0}, {0, 0}, {0, 0}, {0, 0}},
                                {{0, 0}, {0, 0}, {0, 0}, {0, 0}}});
      }

      cam.SubmitProcessedFrame(0_s, visibleTargetList);
    }
  }

  units::square_meter_t GetM2PerPx(units::meter_t dist) {
    units::meter_t widthMPerPx =
        2 * dist * units::math::tan(camHorizFOV / 2) / cameraResWidth;
    units::meter_t heightMPerPx =
        2 * dist * units::math::tan(camVertFOV / 2) / cameraResHeight;
    return widthMPerPx * heightMPerPx;
  }

  bool CamCamSeeTarget(units::meter_t dist, units::radian_t yaw,
                       units::radian_t pitch, double area) {
    bool inRange = dist < maxLEDRange;
    bool inHorizAngle = units::math::abs(yaw) < camHorizFOV / 2;
    bool inVertAngle = units::math::abs(pitch) < camVertFOV / 2;
    bool targetBigEnough = area > minTargetArea;
    return (inRange && inHorizAngle && inVertAngle && targetBigEnough);
  }
};
}  // namespace photonlib
