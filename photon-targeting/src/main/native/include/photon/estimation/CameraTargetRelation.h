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
