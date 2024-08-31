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

#include "Robot.h"

#include <photon/PhotonUtils.h>

#include <frc/Timer.h>
#include <frc/smartdashboard/SmartDashboard.h>
#include <units/time.h>

void Robot::RobotPeriodic() {
  photon::PhotonCamera::SetVersionCheckEnabled(false);

  auto start = frc::Timer::GetFPGATimestamp();
  photon::PhotonPipelineResult result = camera.GetLatestResult();
  auto end = frc::Timer::GetFPGATimestamp();

  std::printf("DT is %.2f uS for %i targets\n",
              units::microsecond_t(end - start).to<double>(),
              result.GetTargets().size());
}

void Robot::TeleopPeriodic() {
  double forwardSpeed = -xboxController.GetRightY();
  double rotationSpeed;

  if (xboxController.GetAButton()) {
    // Vision-alignment mode
    // Query the latest result from PhotonVision
    auto start = frc::Timer::GetFPGATimestamp();
    photon::PhotonPipelineResult result = camera.GetLatestResult();
    auto end = frc::Timer::GetFPGATimestamp();
    frc::SmartDashboard::PutNumber("decode_dt", (end - start).to<double>());

    if (result.HasTargets()) {
      // Rotation speed is the output of the PID controller
      rotationSpeed = -controller.Calculate(result.GetBestTarget().GetYaw(), 0);
    } else {
      // If we have no targets, stay still.
      rotationSpeed = 0;
    }
  } else {
    // Manual Driver Mode
    rotationSpeed = xboxController.GetLeftX();
  }

  // Use our forward/turn speeds to control the drivetrain
  drive.ArcadeDrive(forwardSpeed, rotationSpeed);
}

#ifndef RUNNING_FRC_TESTS
int main() { return frc::StartRobot<Robot>(); }
#endif
