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

#include <frc/geometry/Pose3d.h>

namespace photon {
class CameraTargetRelation {
 public:
  const frc::Pose3d camPose;
  const frc::Transform3d camToTarg;
  const units::meter_t camToTargDist;
  const units::meter_t camToTargDistXY;
  const frc::Rotation2d camToTargYaw;
  const frc::Rotation2d camToTargPitch;

  const frc::Rotation2d camToTargAngle;

  const frc::Transform3d targToCam;
  const frc::Rotation2d targToCamYaw;
  const frc::Rotation2d targToCamPitch;

  const frc::Rotation2d targToCamAngle;

  CameraTargetRelation(const frc::Pose3d& cameraPose,
                       const frc::Pose3d& targetPose)
      : camPose(cameraPose),
        camToTarg(frc::Transform3d{cameraPose, targetPose}),
        camToTargDist(camToTarg.Translation().Norm()),
        camToTargDistXY(units::math::hypot(camToTarg.Translation().X(),
                                           camToTarg.Translation().Y())),
        camToTargYaw(frc::Rotation2d{camToTarg.X().to<double>(),
                                     camToTarg.Y().to<double>()}),
        camToTargPitch(frc::Rotation2d{camToTargDistXY.to<double>(),
                                       -camToTarg.Z().to<double>()}),
        camToTargAngle(frc::Rotation2d{units::math::hypot(
            camToTargYaw.Radians(), camToTargPitch.Radians())}),
        targToCam(frc::Transform3d{targetPose, cameraPose}),
        targToCamYaw(frc::Rotation2d{targToCam.X().to<double>(),
                                     targToCam.Y().to<double>()}),
        targToCamPitch(frc::Rotation2d{camToTargDistXY.to<double>(),
                                       -targToCam.Z().to<double>()}),
        targToCamAngle(frc::Rotation2d{units::math::hypot(
            targToCamYaw.Radians(), targToCamPitch.Radians())}) {}
};
}  // namespace photon
