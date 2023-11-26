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

void Robot::AutonomousInit() {
  autoTimer.Restart();
  frc::Pose2d pose{1_m, 1_m, frc::Rotation2d{}};
  drivetrain.ResetPose(pose, true);
}

void Robot::AutonomousPeriodic() {
  if (autoTimer.Get() < 10_s) {
    drivetrain.Drive(0.5_mps, 0.5_mps, 0.5_rad_per_s, false);
  } else {
    autoTimer.Stop();
    drivetrain.Stop();
  }
}

void Robot::AutonomousExit() {}

void Robot::TeleopInit() {}

void Robot::TeleopPeriodic() {
  double forward = -controller.GetLeftY() * kDriveSpeed;
  if (std::abs(forward) < 0.1) {
    forward = 0;
  }
  forward = forwardLimiter.Calculate(forward);

  double strafe = -controller.GetLeftX() * kDriveSpeed;
  if (std::abs(strafe) < 0.1) {
    strafe = 0;
  }
  strafe = strafeLimiter.Calculate(strafe);

  double turn = -controller.GetRightX() * kDriveSpeed;
  if (std::abs(turn) < 0.1) {
    turn = 0;
  }
  turn = turnLimiter.Calculate(turn);

  drivetrain.Drive(forward * constants::Swerve::kMaxLinearSpeed,
                   strafe * constants::Swerve::kMaxLinearSpeed,
                   turn * constants::Swerve::kMaxAngularSpeed, true);
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
  frc::sim::RoboRioSim::SetVInVoltage(loadedBattVolts);
}

#ifndef RUNNING_FRC_TESTS
int main() { return frc::StartRobot<Robot>(); }
#endif
