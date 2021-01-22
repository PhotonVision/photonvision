/**
 * Copyright (C) 2018-2020 Photon Vision.
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

#include "Robot.h"

#include <photonlib/PhotonUtils.h>

void Robot::TeleopPeriodic() {
  double forwardSpeed;
  double rotationSpeed;

  if (xboxController.GetAButton()) {
    // Vision-alignment mode
    // Query the latest result from PhotonVision
    const auto &result = camera.GetLatestResult();

    if (result.HasTargets()) {
      // First calculate range
      units::meter_t range =
          photonlib::PhotonUtils::CalculateDistanceToTarget(
              CAMERA_HEIGHT, TARGET_HEIGHT, CAMERA_PITCH,
              units::degree_t{result.GetBestTarget().GetPitch()});

      // Use this range as the measurement we give to the PID controller.
      // -1.0 required to ensure positive PID controller effort _increases_ range
      forwardSpeed = -1.0* forwardController.Calculate(
          range.to<double>(), GOAL_RANGE_METERS.to<double>());

      // Also calculate angular power
      // -1.0 required to ensure positive PID controller effort _increases_ yaw
      rotationSpeed = -1.0 * turnController.Calculate(
          result.GetBestTarget().GetYaw(), 0);
    } else {
      // If we have no targets, stay still.
      forwardSpeed = 0;
      rotationSpeed = 0;
    }
  } else {
    // Manual Driver Mode
    forwardSpeed =
        -1.0 * xboxController.GetY(frc::GenericHID::JoystickHand::kRightHand);
    rotationSpeed =
        xboxController.GetX(frc::GenericHID::JoystickHand::kLeftHand);
  }

  // Use our forward/turn speeds to control the drivetrain
  drive.ArcadeDrive(forwardSpeed, rotationSpeed);
}

#ifndef RUNNING_FRC_TESTS
int main() { return frc::StartRobot<Robot>(); }
#endif
