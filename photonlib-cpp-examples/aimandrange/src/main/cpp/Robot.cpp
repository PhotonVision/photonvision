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

#include "Robot.h"

#include <photonlib/PhotonUtils.h>

void Robot::TeleopPeriodic() {
  double forwardSpeed;
  double rotationSpeed;

  if (xboxController.GetAButton()) {
    // Vision-alignment mode
    // Query the latest result from PhotonVision
    const auto& result = camera.GetLatestResult();

    if (result.HasTargets()) {
      // First calculate range
      units::meter_t range = photonlib::PhotonUtils::CalculateDistanceToTarget(
          CAMERA_HEIGHT, TARGET_HEIGHT, CAMERA_PITCH,
          units::degree_t{result.GetBestTarget().GetPitch()});

      // Use this range as the measurement we give to the PID controller.
      // -1.0 required to ensure positive PID controller effort _increases_
      // range
      forwardSpeed = -forwardController.Calculate(range.value(),
                                                  GOAL_RANGE_METERS.value());

      // Also calculate angular power
      // -1.0 required to ensure positive PID controller effort _increases_ yaw
      rotationSpeed =
          -turnController.Calculate(result.GetBestTarget().GetYaw(), 0);
    } else {
      // If we have no targets, stay still.
      forwardSpeed = 0;
      rotationSpeed = 0;
    }
  } else {
    // Manual Driver Mode
    forwardSpeed = -xboxController.GetRightY();
    rotationSpeed = xboxController.GetLeftX();
  }

  // Use our forward/turn speeds to control the drivetrain
  drive.ArcadeDrive(forwardSpeed, rotationSpeed);
}

#ifndef RUNNING_FRC_TESTS
int main() { return frc::StartRobot<Robot>(); }
#endif
