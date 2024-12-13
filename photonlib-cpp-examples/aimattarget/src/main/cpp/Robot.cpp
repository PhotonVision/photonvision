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

#include <iostream>

#include <frc/simulation/BatterySim.h>
#include <frc/simulation/RoboRioSim.h>

void Robot::RobotInit() {}

void Robot::RobotPeriodic() {
  drivetrain.Periodic();
  drivetrain.Log();
}

void Robot::DisabledInit() {}

void Robot::DisabledPeriodic() { drivetrain.Stop(); }

void Robot::DisabledExit() {}

void Robot::AutonomousInit() {}

void Robot::AutonomousPeriodic() {}

void Robot::AutonomousExit() {}

void Robot::TeleopInit() {
  frc::Pose2d pose{1_m, 1_m, frc::Rotation2d{}};
  drivetrain.ResetPose(pose, true);
}

void Robot::TeleopPeriodic() {
  // Calculate drivetrain commands from Joystick values
  auto forward =
      -1.0 * controller.GetLeftY() * constants::Swerve::kMaxLinearSpeed;
  auto strafe =
      -1.0 * controller.GetLeftX() * constants::Swerve::kMaxLinearSpeed;
  auto turn =
      -1.0 * controller.GetRightX() * constants::Swerve::kMaxAngularSpeed;

  bool targetVisible = false;
  double targetYaw = 0.0;
  auto results = camera.GetAllUnreadResults();
  if (results.size() > 0) {
    // Camera processed a new frame since last
    // Get the last one in the list.
    auto result = results[results.size() - 1];
    if (result.HasTargets()) {
      // At least one AprilTag was seen by the camera
      for (auto& target : result.GetTargets()) {
        if (target.GetFiducialId() == 7) {
          // Found Tag 7, record its information
          targetYaw = target.GetYaw();
          targetVisible = true;
        }
      }
    }
  }

  // Auto-align when requested
  if (controller.GetAButton() && targetVisible) {
    // Driver wants auto-alignment to tag 7
    // And, tag 7 is in sight, so we can turn toward it.
    // Override the driver's turn command with an automatic one that turns
    // toward the tag.
    turn =
        -1.0 * targetYaw * VISION_TURN_kP * constants::Swerve::kMaxAngularSpeed;
  }

  // Command drivetrain motors based on target speeds
  drivetrain.Drive(forward, strafe, turn);
}

void Robot::TeleopExit() {}

void Robot::TestInit() {}

void Robot::TestPeriodic() {}

void Robot::TestExit() {}

void Robot::SimulationPeriodic() {
  drivetrain.SimulationPeriodic();
  vision.SimPeriodic(drivetrain.GetSimPose());

  frc::Field2d& debugField = vision.GetSimDebugField();
  debugField.GetObject("EstimatedRobot")->SetPose(drivetrain.GetPose());
  debugField.GetObject("EstimatedRobotModules")
      ->SetPoses(drivetrain.GetModulePoses());

  units::ampere_t totalCurrent = drivetrain.GetCurrentDraw();
  units::volt_t loadedBattVolts =
      frc::sim::BatterySim::Calculate({totalCurrent});
  // Using max(0.1, voltage) here isn't a *physically correct* solution,
  // but it avoids problems with battery voltage measuring 0.
  frc::sim::RoboRioSim::SetVInVoltage(units::math::max(0.1_V, loadedBattVolts));
}

#ifndef RUNNING_FRC_TESTS
int main() { return frc::StartRobot<Robot>(); }
#endif
