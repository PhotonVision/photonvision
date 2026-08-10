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

#include <string>

#include <wpi/math/util/MathUtil.hpp>
#include <wpi/smartdashboard/SmartDashboard.hpp>
#include <wpi/system/RobotController.hpp>

SwerveModule::SwerveModule(const constants::Swerve::ModuleConstants& consts)
    : moduleConstants(consts),
      driveMotor(wpi::PWMSparkMax{moduleConstants.driveMotorId}),
      driveEncoder(wpi::Encoder{moduleConstants.driveEncoderA,
                                moduleConstants.driveEncoderB}),
      steerMotor(wpi::PWMSparkMax{moduleConstants.steerMotorId}),
      steerEncoder(wpi::Encoder{moduleConstants.steerEncoderA,
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
  wpi::units::volt_t steerPID = wpi::units::volt_t{
      steerPIDController.Calculate(GetAbsoluteHeading().Radians().to<double>(),
                                   desiredState.angle.Radians().to<double>())};
  steerMotor.SetVoltage(steerPID);

  wpi::units::volt_t driveFF =
      constants::Swerve::kDriveFF.Calculate(desiredState.speed);
  wpi::units::volt_t drivePID{0};
  if (!openLoop) {
    drivePID = wpi::units::volt_t{drivePIDController.Calculate(
        driveEncoder.GetRate(), desiredState.speed.to<double>())};
  }
  driveMotor.SetVoltage(driveFF + drivePID);
}

void SwerveModule::SetDesiredState(wpi::math::SwerveModuleState newState,
                                   bool shouldBeOpenLoop, bool steerInPlace) {
  wpi::math::Rotation2d currentRotation = GetAbsoluteHeading();
  newState.Optimize(currentRotation);
  desiredState = newState;
}

wpi::math::Rotation2d SwerveModule::GetAbsoluteHeading() const {
  return wpi::math::Rotation2d{
      wpi::units::radian_t{steerEncoder.GetDistance()}};
}

wpi::math::SwerveModuleState SwerveModule::GetState() const {
  return wpi::math::SwerveModuleState{driveEncoder.GetRate() * 1_mps,
                                      GetAbsoluteHeading()};
}

wpi::math::SwerveModulePosition SwerveModule::GetPosition() const {
  return wpi::math::SwerveModulePosition{driveEncoder.GetDistance() * 1_m,
                                         GetAbsoluteHeading()};
}

wpi::units::volt_t SwerveModule::GetDriveVoltage() const {
  return driveMotor.Get() * wpi::RobotController::GetBatteryVoltage();
}

wpi::units::volt_t SwerveModule::GetSteerVoltage() const {
  return steerMotor.Get() * wpi::RobotController::GetBatteryVoltage();
}

wpi::units::ampere_t SwerveModule::GetDriveCurrentSim() const {
  return driveCurrentSim;
}

wpi::units::ampere_t SwerveModule::GetSteerCurrentSim() const {
  return steerCurrentSim;
}

constants::Swerve::ModuleConstants SwerveModule::GetModuleConstants() const {
  return moduleConstants;
}

void SwerveModule::Log() {
  wpi::math::SwerveModuleState state = GetState();

  std::string table =
      "Module " + std::to_string(moduleConstants.moduleNum) + "/";
  wpi::SmartDashboard::PutNumber(table + "Steer Degrees",
                                 wpi::math::AngleModulus(state.angle.Radians())
                                     .convert<wpi::units::degrees>()
                                     .to<double>());
  wpi::SmartDashboard::PutNumber(
      table + "Steer Target Degrees",
      wpi::units::radian_t{steerPIDController.GetSetpoint()}
          .convert<wpi::units::degrees>()
          .to<double>());
  wpi::SmartDashboard::PutNumber(
      table + "Drive Velocity Feet",
      state.speed.convert<wpi::units::feet_per_second>().to<double>());
  wpi::SmartDashboard::PutNumber(
      table + "Drive Velocity Target Feet",
      desiredState.speed.convert<wpi::units::feet_per_second>().to<double>());
  wpi::SmartDashboard::PutNumber(table + "Drive Current",
                                 driveCurrentSim.to<double>());
  wpi::SmartDashboard::PutNumber(table + "Steer Current",
                                 steerCurrentSim.to<double>());
}

void SwerveModule::SimulationUpdate(
    wpi::units::meter_t driveEncoderDist,
    wpi::units::meters_per_second_t driveEncoderRate,
    wpi::units::ampere_t driveCurrent, wpi::units::radian_t steerEncoderDist,
    wpi::units::radians_per_second_t steerEncoderRate,
    wpi::units::ampere_t steerCurrent) {
  driveEncoderSim.SetDistance(driveEncoderDist.to<double>());
  driveEncoderSim.SetRate(driveEncoderRate.to<double>());
  driveCurrentSim = driveCurrent;
  steerEncoderSim.SetDistance(steerEncoderDist.to<double>());
  steerEncoderSim.SetRate(steerEncoderRate.to<double>());
  steerCurrentSim = steerCurrent;
}
