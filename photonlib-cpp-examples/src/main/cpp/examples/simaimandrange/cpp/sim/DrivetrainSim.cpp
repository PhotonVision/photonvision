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

#include "DrivetrainSim.h"

/**
 * Perform all periodic drivetrain simulation related tasks to advance our
 * simulation of robot physics forward by a single 20ms step.
 */
void DrivetrainSim::update() {
  double leftMotorCmd = 0;
  double rightMotorCmd = 0;

  if (frc::DriverStation::IsEnabled() &&
      !frc::RobotController::IsBrownedOut()) {
    leftMotorCmd = leftLeader.GetSpeed();
    rightMotorCmd = rightLeader.GetSpeed();
  }

  m_drivetrainSimulator.SetInputs(
      units::volt_t(leftMotorCmd * frc::RobotController::GetInputVoltage()),
      units::volt_t(-rightMotorCmd * frc::RobotController::GetInputVoltage()));
  m_drivetrainSimulator.Update(20_ms);

  // Update PhotonVision based on our new robot position.
  simVision.ProcessFrame(m_drivetrainSimulator.GetPose());

  field.SetRobotPose(m_drivetrainSimulator.GetPose());
}
