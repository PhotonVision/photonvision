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

#include <wpi/math/geometry/Pose3d.hpp>

namespace photon {
class CameraTargetRelation {
 public:
  const wpi::math::Pose3d camPose;
  const wpi::math::Transform3d camToTarg;
  const wpi::units::meter_t camToTargDist;
  const wpi::units::meter_t camToTargDistXY;
  const wpi::math::Rotation2d camToTargYaw;
  const wpi::math::Rotation2d camToTargPitch;

  const wpi::math::Rotation2d camToTargAngle;

  const wpi::math::Transform3d targToCam;
  const wpi::math::Rotation2d targToCamYaw;
  const wpi::math::Rotation2d targToCamPitch;

  const wpi::math::Rotation2d targToCamAngle;

  CameraTargetRelation(const wpi::math::Pose3d& cameraPose,
                       const wpi::math::Pose3d& targetPose)
      : camPose(cameraPose),
        camToTarg(wpi::math::Transform3d{cameraPose, targetPose}),
        camToTargDist(camToTarg.Translation().Norm()),
        camToTargDistXY(wpi::units::math::hypot(camToTarg.Translation().X(),
                                           camToTarg.Translation().Y())),
        camToTargYaw(wpi::math::Rotation2d{camToTarg.X().to<double>(),
                                           camToTarg.Y().to<double>()}),
        camToTargPitch(wpi::math::Rotation2d{camToTargDistXY.to<double>(),
                                             -camToTarg.Z().to<double>()}),
        camToTargAngle(wpi::math::Rotation2d{wpi::units::math::hypot(
            camToTargYaw.Radians(), camToTargPitch.Radians())}),
        targToCam(wpi::math::Transform3d{targetPose, cameraPose}),
        targToCamYaw(wpi::math::Rotation2d{targToCam.X().to<double>(),
                                           targToCam.Y().to<double>()}),
        targToCamPitch(wpi::math::Rotation2d{camToTargDistXY.to<double>(),
                                             -targToCam.Z().to<double>()}),
        targToCamAngle(wpi::math::Rotation2d{wpi::units::math::hypot(
            targToCamYaw.Radians(), targToCamPitch.Radians())}) {}
};
}  // namespace photon
