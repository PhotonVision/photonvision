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

#pragma once

#include <cmath>
#include <numbers>

#include <frc/RobotController.h>
#include <frc/motorcontrol/PWMSparkMax.h>
#include <frc/simulation/FlywheelSim.h>
#include <frc/smartdashboard/SmartDashboard.h>
#include <frc/system/plant/DCMotor.h>
#include <frc/system/plant/LinearSystemId.h>
#include <units/angle.h>
#include <units/moment_of_inertia.h>

class GamepieceLauncher {
 public:
  GamepieceLauncher();              // Constructor
  void setRunning(bool shouldRun);  // Method to start/stop the launcher
  void periodic();                  // Method to handle periodic updates
  void simulationPeriodic();        // Method to handle simulation updates

 private:
  frc::PWMSparkMax* motor;  // Motor controller

  const double LAUNCH_SPEED_RPM = 2500;
  double curDesSpd = 0.0;
  double curMotorCmd = 0.0;

  static constexpr units::kilogram_square_meter_t kFlywheelMomentOfInertia =
      0.5 * 1.5_lb * 4_in * 4_in;

  frc::DCMotor m_gearbox = frc::DCMotor::Falcon500(1);
  frc::LinearSystem<1, 1, 1> m_plant{frc::LinearSystemId::FlywheelSystem(
      m_gearbox, kFlywheelMomentOfInertia, 1.0)};

  frc::sim::FlywheelSim launcherSim{m_plant, m_gearbox};

  void simulationInit();  // Method to initialize simulation components
};
