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

#include "subsystems/SwerveDrive.h"

#include <iostream>
#include <string>

#include <frc/TimedRobot.h>
#include <frc/smartdashboard/SmartDashboard.h>

SwerveDrive::SwerveDrive()
    : poseEstimator(kinematics, GetGyroYaw(), GetModulePositions(),
                    frc::Pose2d{}, {0.1, 0.1, 0.1}, {1.0, 1.0, 1.0}),
      gyroSim(gyro),
      swerveDriveSim(constants::Swerve::kDriveFF, frc::DCMotor::Falcon500(1),
                     constants::Swerve::kDriveGearRatio,
                     constants::Swerve::kWheelDiameter / 2,
                     constants::Swerve::kSteerFF, frc::DCMotor::Falcon500(1),
                     constants::Swerve::kSteerGearRatio, kinematics) {}

void SwerveDrive::Periodic() {
  for (auto& currentModule : swerveMods) {
    currentModule.Periodic();
  }

  poseEstimator.Update(GetGyroYaw(), GetModulePositions());
}

void SwerveDrive::Drive(units::meters_per_second_t vx,
                        units::meters_per_second_t vy,
                        units::radians_per_second_t omega) {
  frc::ChassisSpeeds newChassisSpeeds =
      frc::ChassisSpeeds::FromFieldRelativeSpeeds(vx, vy, omega, GetHeading());
  SetChassisSpeeds(newChassisSpeeds, true, false);
}

void SwerveDrive::SetChassisSpeeds(const frc::ChassisSpeeds& newChassisSpeeds,
                                   bool openLoop, bool steerInPlace) {
  SetModuleStates(kinematics.ToSwerveModuleStates(newChassisSpeeds), true,
                  steerInPlace);
  this->targetChassisSpeeds = newChassisSpeeds;
}

void SwerveDrive::SetModuleStates(
    const std::array<frc::SwerveModuleState, 4>& desiredStates, bool openLoop,
    bool steerInPlace) {
  std::array<frc::SwerveModuleState, 4> desaturatedStates = desiredStates;
  frc::SwerveDriveKinematics<4>::DesaturateWheelSpeeds(
      static_cast<wpi::array<frc::SwerveModuleState, 4>*>(&desaturatedStates),
      constants::Swerve::kMaxLinearSpeed);
  for (int i = 0; i < swerveMods.size(); i++) {
    swerveMods[i].SetDesiredState(desaturatedStates[i], openLoop, steerInPlace);
  }
}

void SwerveDrive::Stop() { Drive(0_mps, 0_mps, 0_rad_per_s); }

void SwerveDrive::ResetPose(const frc::Pose2d& pose, bool resetSimPose) {
  if (resetSimPose) {
    swerveDriveSim.Reset(pose, false);
    for (int i = 0; i < swerveMods.size(); i++) {
      swerveMods[i].SimulationUpdate(0_m, 0_mps, 0_A, 0_rad, 0_rad_per_s, 0_A);
    }
    gyroSim.SetAngle(-pose.Rotation().Degrees());
    gyroSim.SetRate(0_rad_per_s);
  }

  poseEstimator.ResetPosition(GetGyroYaw(), GetModulePositions(), pose);
}

frc::Pose2d SwerveDrive::GetPose() const {
  return poseEstimator.GetEstimatedPosition();
}

frc::Rotation2d SwerveDrive::GetHeading() const { return GetPose().Rotation(); }

frc::Rotation2d SwerveDrive::GetGyroYaw() const { return gyro.GetRotation2d(); }

frc::ChassisSpeeds SwerveDrive::GetChassisSpeeds() const {
  return kinematics.ToChassisSpeeds(GetModuleStates());
}

std::array<frc::SwerveModuleState, 4> SwerveDrive::GetModuleStates() const {
  std::array<frc::SwerveModuleState, 4> moduleStates;
  moduleStates[0] = swerveMods[0].GetState();
  moduleStates[1] = swerveMods[1].GetState();
  moduleStates[2] = swerveMods[2].GetState();
  moduleStates[3] = swerveMods[3].GetState();
  return moduleStates;
}

std::array<frc::SwerveModulePosition, 4> SwerveDrive::GetModulePositions()
    const {
  std::array<frc::SwerveModulePosition, 4> modulePositions;
  modulePositions[0] = swerveMods[0].GetPosition();
  modulePositions[1] = swerveMods[1].GetPosition();
  modulePositions[2] = swerveMods[2].GetPosition();
  modulePositions[3] = swerveMods[3].GetPosition();
  return modulePositions;
}

std::array<frc::Pose2d, 4> SwerveDrive::GetModulePoses() const {
  std::array<frc::Pose2d, 4> modulePoses;
  for (int i = 0; i < swerveMods.size(); i++) {
    const SwerveModule& module = swerveMods[i];
    modulePoses[i] = GetPose().TransformBy(frc::Transform2d{
        module.GetModuleConstants().centerOffset, module.GetAbsoluteHeading()});
  }
  return modulePoses;
}

void SwerveDrive::Log() {
  std::string table = "Drive/";
  frc::Pose2d pose = GetPose();
  frc::SmartDashboard::PutNumber(table + "X", pose.X().to<double>());
  frc::SmartDashboard::PutNumber(table + "Y", pose.Y().to<double>());
  frc::SmartDashboard::PutNumber(table + "Heading",
                                 pose.Rotation().Degrees().to<double>());
  frc::ChassisSpeeds chassisSpeeds = GetChassisSpeeds();
  frc::SmartDashboard::PutNumber(table + "VX", chassisSpeeds.vx.to<double>());
  frc::SmartDashboard::PutNumber(table + "VY", chassisSpeeds.vy.to<double>());
  frc::SmartDashboard::PutNumber(
      table + "Omega Degrees",
      chassisSpeeds.omega.convert<units::degrees_per_second>().to<double>());
  frc::SmartDashboard::PutNumber(table + "Target VX",
                                 targetChassisSpeeds.vx.to<double>());
  frc::SmartDashboard::PutNumber(table + "Target VY",
                                 targetChassisSpeeds.vy.to<double>());
  frc::SmartDashboard::PutNumber(
      table + "Target Omega Degrees",
      targetChassisSpeeds.omega.convert<units::degrees_per_second>()
          .to<double>());

  for (auto& module : swerveMods) {
    module.Log();
  }
}

void SwerveDrive::SimulationPeriodic() {
  std::array<units::volt_t, 4> driveInputs;
  std::array<units::volt_t, 4> steerInputs;
  for (int i = 0; i < swerveMods.size(); i++) {
    driveInputs[i] = swerveMods[i].GetDriveVoltage();
    steerInputs[i] = swerveMods[i].GetSteerVoltage();
  }
  swerveDriveSim.SetDriveInputs(driveInputs);
  swerveDriveSim.SetSteerInputs(steerInputs);

  swerveDriveSim.Update(frc::TimedRobot::kDefaultPeriod);

  auto driveStates = swerveDriveSim.GetDriveStates();
  auto steerStates = swerveDriveSim.GetSteerStates();
  totalCurrentDraw = 0_A;
  std::array<units::ampere_t, 4> driveCurrents =
      swerveDriveSim.GetDriveCurrentDraw();
  for (const auto& current : driveCurrents) {
    totalCurrentDraw += current;
  }
  std::array<units::ampere_t, 4> steerCurrents =
      swerveDriveSim.GetSteerCurrentDraw();
  for (const auto& current : steerCurrents) {
    totalCurrentDraw += current;
  }
  for (int i = 0; i < swerveMods.size(); i++) {
    units::meter_t drivePos{driveStates[i](0, 0)};
    units::meters_per_second_t driveRate{driveStates[i](1, 0)};
    units::radian_t steerPos{steerStates[i](0, 0)};
    units::radians_per_second_t steerRate{steerStates[i](1, 0)};
    swerveMods[i].SimulationUpdate(drivePos, driveRate, driveCurrents[i],
                                   steerPos, steerRate, steerCurrents[i]);
  }
  gyroSim.SetRate(-swerveDriveSim.GetOmega());
  gyroSim.SetAngle(-swerveDriveSim.GetPose().Rotation().Degrees());
}

frc::Pose2d SwerveDrive::GetSimPose() const { return swerveDriveSim.GetPose(); }

units::ampere_t SwerveDrive::GetCurrentDraw() const { return totalCurrentDraw; }
