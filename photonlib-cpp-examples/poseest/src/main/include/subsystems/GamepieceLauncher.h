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

#include <wpi/hardware/motor/PWMSparkMax.hpp>
#include <wpi/math/system/plant/DCMotor.hpp>
#include <wpi/math/system/plant/LinearSystemId.hpp>
#include <wpi/simulation/FlywheelSim.hpp>
#include <wpi/smartdashboard/SmartDashboard.hpp>
#include <wpi/system/RobotController.hpp>
#include <wpi/units/angle.hpp>
#include <wpi/units/moment_of_inertia.hpp>

class GamepieceLauncher {
 public:
  GamepieceLauncher();              // Constructor
  void setRunning(bool shouldRun);  // Method to start/stop the launcher
  void periodic();                  // Method to handle periodic updates
  void simulationPeriodic();        // Method to handle simulation updates

 private:
  wpi::PWMSparkMax* motor;  // Motor controller

  const double LAUNCH_SPEED_RPM = 2500;
  double curDesSpd = 0.0;
  double curMotorCmd = 0.0;

  static constexpr wpi::units::kilogram_square_meter_t
      kFlywheelMomentOfInertia = 0.5 * 1.5_lb * 4_in * 4_in;

  wpi::math::DCMotor m_gearbox = wpi::math::DCMotor::Falcon500(1);
  wpi::math::LinearSystem<1, 1, 1> m_plant{
      wpi::math::LinearSystemId::FlywheelSystem(m_gearbox,
                                                kFlywheelMomentOfInertia, 1.0)};

  wpi::sim::FlywheelSim launcherSim{m_plant, m_gearbox};

  void simulationInit();  // Method to initialize simulation components
};
