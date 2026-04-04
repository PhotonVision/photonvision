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

#include <wpi/hardware/motor/PWMSparkMax.hpp>
#include <wpi/hardware/rotation/Encoder.hpp>
#include <wpi/math/controller/PIDController.hpp>
#include <wpi/math/kinematics/wpi/math/kinematics/SwerveModulePosition.hpppp>
#include <wpi/math/kinematics/wpi/math/kinematics/SwerveModuleState.hpppp>
#include <wpi/simulation/EncoderSim.hpp>
#include <wpi/units/current.hpp>

#include "Constants.h"

class SwerveModule {
 public:
  explicit SwerveModule(const constants::Swerve::ModuleConstants& consts);
  void Periodic();
  void SetDesiredState(wpi::math::SwerveModuleState newState,
                       bool shouldBeOpenLoop, bool steerInPlace);
  wpi::math::Rotation2d GetAbsoluteHeading() const;
  wpi::math::SwerveModuleState GetState() const;
  wpi::math::SwerveModulePosition GetPosition() const;
  wpi::units::volt_t GetDriveVoltage() const;
  wpi::units::volt_t GetSteerVoltage() const;
  wpi::units::ampere_t GetDriveCurrentSim() const;
  wpi::units::ampere_t GetSteerCurrentSim() const;
  constants::Swerve::ModuleConstants GetModuleConstants() const;
  void Log();
  void SimulationUpdate(wpi::units::meter_t driveEncoderDist,
                        wpi::units::meters_per_second_t driveEncoderRate,
                        wpi::units::ampere_t driveCurrent,
                        wpi::units::radian_t steerEncoderDist,
                        wpi::units::radians_per_second_t steerEncoderRate,
                        wpi::units::ampere_t steerCurrent);

 private:
  const constants::Swerve::ModuleConstants moduleConstants;

  wpi::PWMSparkMax driveMotor;
  wpi::Encoder driveEncoder;
  wpi::PWMSparkMax steerMotor;
  wpi::Encoder steerEncoder;

  wpi::math::SwerveModuleState desiredState{};
  bool openLoop{false};

  wpi::math::PIDController drivePIDController{constants::Swerve::kDriveKP,
                                              constants::Swerve::kDriveKI,
                                              constants::Swerve::kDriveKD};
  wpi::math::PIDController steerPIDController{constants::Swerve::kSteerKP,
                                              constants::Swerve::kSteerKI,
                                              constants::Swerve::kSteerKD};

  wpi::sim::EncoderSim driveEncoderSim;
  wpi::units::ampere_t driveCurrentSim{0};
  wpi::sim::EncoderSim steerEncoderSim;
  wpi::units::ampere_t steerCurrentSim{0};
};
