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

#include "subsystems/GamepieceLauncher.h"  // Include the header file

GamepieceLauncher::GamepieceLauncher() {
  motor = new frc::PWMSparkMax(8);  // Create the motor object for PWM port 8
  simulationInit();
}

void GamepieceLauncher::setRunning(bool shouldRun) {
  curDesSpd = shouldRun ? LAUNCH_SPEED_RPM : 0.0;
}

void GamepieceLauncher::periodic() {
  // Calculate the maximum RPM
  double maxRPM =
      units::radians_per_second_t(frc::DCMotor::Falcon500(1).freeSpeed)
          .to<double>() *
      60 / (2 * std::numbers::pi);
  curMotorCmd = curDesSpd / maxRPM;
  curMotorCmd = std::clamp(curMotorCmd, 0.0, 1.0);
  motor->Set(curMotorCmd);

  frc::SmartDashboard::PutNumber("GPLauncher Des Spd (RPM)", curDesSpd);
}

void GamepieceLauncher::simulationPeriodic() {
  launcherSim.SetInputVoltage(curMotorCmd *
                              frc::RobotController::GetBatteryVoltage());
  launcherSim.Update(0.02_s);
  auto spd = launcherSim.GetAngularVelocity().to<double>() * 60 /
             (2 * std::numbers::pi);
  frc::SmartDashboard::PutNumber("GPLauncher Act Spd (RPM)", spd);
}

void GamepieceLauncher::simulationInit() {}
