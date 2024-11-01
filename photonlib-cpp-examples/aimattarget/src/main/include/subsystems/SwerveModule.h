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

#include <frc/Encoder.h>
#include <frc/controller/PIDController.h>
#include <frc/kinematics/SwerveModulePosition.h>
#include <frc/kinematics/SwerveModuleState.h>
#include <frc/motorcontrol/PWMSparkMax.h>
#include <frc/simulation/EncoderSim.h>
#include <units/current.h>

#include "Constants.h"

class SwerveModule {
 public:
  explicit SwerveModule(const constants::Swerve::ModuleConstants& consts);
  void Periodic();
  void SetDesiredState(frc::SwerveModuleState newState, bool shouldBeOpenLoop,
                       bool steerInPlace);
  frc::Rotation2d GetAbsoluteHeading() const;
  frc::SwerveModuleState GetState() const;
  frc::SwerveModulePosition GetPosition() const;
  units::volt_t GetDriveVoltage() const;
  units::volt_t GetSteerVoltage() const;
  units::ampere_t GetDriveCurrentSim() const;
  units::ampere_t GetSteerCurrentSim() const;
  constants::Swerve::ModuleConstants GetModuleConstants() const;
  void Log();
  void SimulationUpdate(units::meter_t driveEncoderDist,
                        units::meters_per_second_t driveEncoderRate,
                        units::ampere_t driveCurrent,
                        units::radian_t steerEncoderDist,
                        units::radians_per_second_t steerEncoderRate,
                        units::ampere_t steerCurrent);

 private:
  const constants::Swerve::ModuleConstants moduleConstants;

  frc::PWMSparkMax driveMotor;
  frc::Encoder driveEncoder;
  frc::PWMSparkMax steerMotor;
  frc::Encoder steerEncoder;

  frc::SwerveModuleState desiredState{};
  bool openLoop{false};

  frc::PIDController drivePIDController{constants::Swerve::kDriveKP,
                                        constants::Swerve::kDriveKI,
                                        constants::Swerve::kDriveKD};
  frc::PIDController steerPIDController{constants::Swerve::kSteerKP,
                                        constants::Swerve::kSteerKI,
                                        constants::Swerve::kSteerKD};

  frc::sim::EncoderSim driveEncoderSim;
  units::ampere_t driveCurrentSim{0};
  frc::sim::EncoderSim steerEncoderSim;
  units::ampere_t steerCurrentSim{0};
};
