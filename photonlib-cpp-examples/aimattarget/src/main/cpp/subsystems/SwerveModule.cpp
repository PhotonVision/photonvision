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

#include "subsystems/SwerveModule.h"

#include <iostream>
#include <string>

#include <frc/MathUtil.h>
#include <frc/RobotController.h>
#include <frc/smartdashboard/SmartDashboard.h>

SwerveModule::SwerveModule(const constants::Swerve::ModuleConstants& consts)
    : moduleConstants(consts),
      driveMotor(frc::PWMSparkMax{moduleConstants.driveMotorId}),
      driveEncoder(frc::Encoder{moduleConstants.driveEncoderA,
                                moduleConstants.driveEncoderB}),
      steerMotor(frc::PWMSparkMax{moduleConstants.steerMotorId}),
      steerEncoder(frc::Encoder{moduleConstants.steerEncoderA,
                                moduleConstants.steerEncoderB}),
      driveEncoderSim(driveEncoder),
      steerEncoderSim(steerEncoder) {
  driveEncoder.SetDistancePerPulse(
      constants::Swerve::kDriveDistPerPulse.to<double>());
  steerEncoder.SetDistancePerPulse(
      constants::Swerve::kSteerRadPerPulse.to<double>());
  steerPIDController.EnableContinuousInput(-std::numbers::pi, std::numbers::pi);
}

void SwerveModule::Periodic() {
  units::volt_t steerPID = units::volt_t{
      steerPIDController.Calculate(GetAbsoluteHeading().Radians().to<double>(),
                                   desiredState.angle.Radians().to<double>())};
  steerMotor.SetVoltage(steerPID);

  units::volt_t driveFF =
      constants::Swerve::kDriveFF.Calculate(desiredState.speed);
  units::volt_t drivePID{0};
  if (!openLoop) {
    drivePID = units::volt_t{drivePIDController.Calculate(
        driveEncoder.GetRate(), desiredState.speed.to<double>())};
  }
  driveMotor.SetVoltage(driveFF + drivePID);
}

void SwerveModule::SetDesiredState(frc::SwerveModuleState newState,
                                   bool shouldBeOpenLoop, bool steerInPlace) {
  frc::Rotation2d currentRotation = GetAbsoluteHeading();
  newState.Optimize(currentRotation);
  desiredState = newState;
}

frc::Rotation2d SwerveModule::GetAbsoluteHeading() const {
  return frc::Rotation2d{units::radian_t{steerEncoder.GetDistance()}};
}

frc::SwerveModuleState SwerveModule::GetState() const {
  return frc::SwerveModuleState{driveEncoder.GetRate() * 1_mps,
                                GetAbsoluteHeading()};
}

frc::SwerveModulePosition SwerveModule::GetPosition() const {
  return frc::SwerveModulePosition{driveEncoder.GetDistance() * 1_m,
                                   GetAbsoluteHeading()};
}

units::volt_t SwerveModule::GetDriveVoltage() const {
  return driveMotor.Get() * frc::RobotController::GetBatteryVoltage();
}

units::volt_t SwerveModule::GetSteerVoltage() const {
  return steerMotor.Get() * frc::RobotController::GetBatteryVoltage();
}

units::ampere_t SwerveModule::GetDriveCurrentSim() const {
  return driveCurrentSim;
}

units::ampere_t SwerveModule::GetSteerCurrentSim() const {
  return steerCurrentSim;
}

constants::Swerve::ModuleConstants SwerveModule::GetModuleConstants() const {
  return moduleConstants;
}

void SwerveModule::Log() {
  frc::SwerveModuleState state = GetState();

  std::string table =
      "Module " + std::to_string(moduleConstants.moduleNum) + "/";
  frc::SmartDashboard::PutNumber(table + "Steer Degrees",
                                 frc::AngleModulus(state.angle.Radians())
                                     .convert<units::degrees>()
                                     .to<double>());
  frc::SmartDashboard::PutNumber(
      table + "Steer Target Degrees",
      units::radian_t{steerPIDController.GetSetpoint()}
          .convert<units::degrees>()
          .to<double>());
  frc::SmartDashboard::PutNumber(
      table + "Drive Velocity Feet",
      state.speed.convert<units::feet_per_second>().to<double>());
  frc::SmartDashboard::PutNumber(
      table + "Drive Velocity Target Feet",
      desiredState.speed.convert<units::feet_per_second>().to<double>());
  frc::SmartDashboard::PutNumber(table + "Drive Current",
                                 driveCurrentSim.to<double>());
  frc::SmartDashboard::PutNumber(table + "Steer Current",
                                 steerCurrentSim.to<double>());
}

void SwerveModule::SimulationUpdate(
    units::meter_t driveEncoderDist,
    units::meters_per_second_t driveEncoderRate, units::ampere_t driveCurrent,
    units::radian_t steerEncoderDist,
    units::radians_per_second_t steerEncoderRate,
    units::ampere_t steerCurrent) {
  driveEncoderSim.SetDistance(driveEncoderDist.to<double>());
  driveEncoderSim.SetRate(driveEncoderRate.to<double>());
  driveCurrentSim = driveCurrent;
  steerEncoderSim.SetDistance(steerEncoderDist.to<double>());
  steerEncoderSim.SetRate(steerEncoderRate.to<double>());
  steerCurrentSim = steerCurrent;
}
