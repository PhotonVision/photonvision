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
