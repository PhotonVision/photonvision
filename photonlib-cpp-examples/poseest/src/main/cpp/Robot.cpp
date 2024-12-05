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
  launcher.periodic();
  drivetrain.Periodic();

  auto visionEst = vision.GetEstimatedGlobalPose();
  if (visionEst.has_value()) {
    auto est = visionEst.value();
    auto estPose = est.estimatedPose.ToPose2d();
    auto estStdDevs = vision.GetEstimationStdDevs(estPose);
    drivetrain.AddVisionMeasurement(est.estimatedPose.ToPose2d(), est.timestamp,
                                    estStdDevs);
  }

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

  // Command drivetrain motors based on target speeds
  drivetrain.Drive(forward, strafe, turn);

  // Calculate whether the gamepiece launcher runs based on our global pose
  // estimate.
  frc::Pose2d curPose = drivetrain.GetPose();
  bool shouldRun = (curPose.Y() > 2.0_m &&
                    curPose.X() < 4.0_m);  // Close enough to blue speaker
  launcher.setRunning(shouldRun);
}

void Robot::TeleopExit() {}

void Robot::TestInit() {}

void Robot::TestPeriodic() {}

void Robot::TestExit() {}

void Robot::SimulationPeriodic() {
  launcher.simulationPeriodic();
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
