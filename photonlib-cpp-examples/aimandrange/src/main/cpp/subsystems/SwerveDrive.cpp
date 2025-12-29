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

#include <string>

#include <wpi/opmode/TimedRobot.hpp>
#include <wpi/smartdashboard/SmartDashboard.hpp>

SwerveDrive::SwerveDrive()
    : poseEstimator(kinematics, GetGyroYaw(), GetModulePositions(),
                    wpi::math::Pose2d{}, {0.1, 0.1, 0.1}, {1.0, 1.0, 1.0}),
      // gyroSim(gyro),
      swerveDriveSim(
          constants::Swerve::kDriveFF, wpi::math::DCMotor::Falcon500(1),
          constants::Swerve::kDriveGearRatio,
          constants::Swerve::kWheelDiameter / 2, constants::Swerve::kSteerFF,
          wpi::math::DCMotor::Falcon500(1), constants::Swerve::kSteerGearRatio,
          kinematics) {}

void SwerveDrive::Periodic() {
  for (auto& currentModule : swerveMods) {
    currentModule.Periodic();
  }

  poseEstimator.Update(GetGyroYaw(), GetModulePositions());
}

void SwerveDrive::Drive(wpi::units::meters_per_second_t vx,
                        wpi::units::meters_per_second_t vy,
                        wpi::units::radians_per_second_t omega) {
  wpi::math::ChassisSpeeds newChassisSpeeds =
      wpi::math::ChassisSpeeds::FromFieldRelativeSpeeds(vx, vy, omega,
                                                        GetHeading());
  SetChassisSpeeds(newChassisSpeeds, true, false);
}

void SwerveDrive::SetChassisSpeeds(
    const wpi::math::ChassisSpeeds& newChassisSpeeds, bool openLoop,
    bool steerInPlace) {
  SetModuleStates(kinematics.ToSwerveModuleStates(newChassisSpeeds), true,
                  steerInPlace);
  this->targetChassisSpeeds = newChassisSpeeds;
}

void SwerveDrive::SetModuleStates(
    const std::array<wpi::math::SwerveModuleState, 4>& desiredStates,
    bool openLoop, bool steerInPlace) {
  std::array<wpi::math::SwerveModuleState, 4> desaturatedStates = desiredStates;
  wpi::math::SwerveDriveKinematics<4>::DesaturateWheelSpeeds(
      static_cast<wpi::util::array<wpi::math::SwerveModuleState, 4>*>(
          &desaturatedStates),
      constants::Swerve::kMaxLinearSpeed);
  for (int i = 0; i < swerveMods.size(); i++) {
    swerveMods[i].SetDesiredState(desaturatedStates[i], openLoop, steerInPlace);
  }
}

void SwerveDrive::Stop() { Drive(0_mps, 0_mps, 0_rad_per_s); }

void SwerveDrive::ResetPose(const wpi::math::Pose2d& pose, bool resetSimPose) {
  if (resetSimPose) {
    swerveDriveSim.Reset(pose, false);
    for (int i = 0; i < swerveMods.size(); i++) {
      swerveMods[i].SimulationUpdate(0_m, 0_mps, 0_A, 0_rad, 0_rad_per_s, 0_A);
    }
    // gyroSim.SetAngle(-pose.Rotation().Degrees());
    // gyroSim.SetRate(0_rad_per_s);
  }

  poseEstimator.ResetPosition(GetGyroYaw(), GetModulePositions(), pose);
}

wpi::math::Pose2d SwerveDrive::GetPose() const {
  return poseEstimator.GetEstimatedPosition();
}

wpi::math::Rotation2d SwerveDrive::GetHeading() const {
  return GetPose().Rotation();
}

wpi::math::Rotation2d SwerveDrive::GetGyroYaw() const {
  return gyro.GetRotation2d();
}

wpi::math::ChassisSpeeds SwerveDrive::GetChassisSpeeds() const {
  return kinematics.ToChassisSpeeds(GetModuleStates());
}

std::array<wpi::math::SwerveModuleState, 4> SwerveDrive::GetModuleStates()
    const {
  std::array<wpi::math::SwerveModuleState, 4> moduleStates;
  moduleStates[0] = swerveMods[0].GetState();
  moduleStates[1] = swerveMods[1].GetState();
  moduleStates[2] = swerveMods[2].GetState();
  moduleStates[3] = swerveMods[3].GetState();
  return moduleStates;
}

std::array<wpi::math::SwerveModulePosition, 4> SwerveDrive::GetModulePositions()
    const {
  std::array<wpi::math::SwerveModulePosition, 4> modulePositions;
  modulePositions[0] = swerveMods[0].GetPosition();
  modulePositions[1] = swerveMods[1].GetPosition();
  modulePositions[2] = swerveMods[2].GetPosition();
  modulePositions[3] = swerveMods[3].GetPosition();
  return modulePositions;
}

std::array<wpi::math::Pose2d, 4> SwerveDrive::GetModulePoses() const {
  std::array<wpi::math::Pose2d, 4> modulePoses;
  for (int i = 0; i < swerveMods.size(); i++) {
    const SwerveModule& module = swerveMods[i];
    modulePoses[i] = GetPose().TransformBy(wpi::math::Transform2d{
        module.GetModuleConstants().centerOffset, module.GetAbsoluteHeading()});
  }
  return modulePoses;
}

void SwerveDrive::Log() {
  std::string table = "Drive/";
  wpi::math::Pose2d pose = GetPose();
  wpi::SmartDashboard::PutNumber(table + "X", pose.X().to<double>());
  wpi::SmartDashboard::PutNumber(table + "Y", pose.Y().to<double>());
  wpi::SmartDashboard::PutNumber(table + "Heading",
                                 pose.Rotation().Degrees().to<double>());
  wpi::math::ChassisSpeeds chassisSpeeds = GetChassisSpeeds();
  wpi::SmartDashboard::PutNumber(table + "VX", chassisSpeeds.vx.to<double>());
  wpi::SmartDashboard::PutNumber(table + "VY", chassisSpeeds.vy.to<double>());
  wpi::SmartDashboard::PutNumber(
      table + "Omega Degrees",
      chassisSpeeds.omega.convert<wpi::units::degrees_per_second>().to<double>());
  wpi::SmartDashboard::PutNumber(table + "Target VX",
                                 targetChassisSpeeds.vx.to<double>());
  wpi::SmartDashboard::PutNumber(table + "Target VY",
                                 targetChassisSpeeds.vy.to<double>());
  wpi::SmartDashboard::PutNumber(
      table + "Target Omega Degrees",
      targetChassisSpeeds.omega.convert<wpi::units::degrees_per_second>()
          .to<double>());

  for (auto& module : swerveMods) {
    module.Log();
  }
}

void SwerveDrive::SimulationPeriodic() {
  std::array<wpi::units::volt_t, 4> driveInputs;
  std::array<wpi::units::volt_t, 4> steerInputs;
  for (int i = 0; i < swerveMods.size(); i++) {
    driveInputs[i] = swerveMods[i].GetDriveVoltage();
    steerInputs[i] = swerveMods[i].GetSteerVoltage();
  }
  swerveDriveSim.SetDriveInputs(driveInputs);
  swerveDriveSim.SetSteerInputs(steerInputs);

  swerveDriveSim.Update(wpi::TimedRobot::kDefaultPeriod);

  auto driveStates = swerveDriveSim.GetDriveStates();
  auto steerStates = swerveDriveSim.GetSteerStates();
  totalCurrentDraw = 0_A;
  std::array<wpi::units::ampere_t, 4> driveCurrents =
      swerveDriveSim.GetDriveCurrentDraw();
  for (const auto& current : driveCurrents) {
    totalCurrentDraw += current;
  }
  std::array<wpi::units::ampere_t, 4> steerCurrents =
      swerveDriveSim.GetSteerCurrentDraw();
  for (const auto& current : steerCurrents) {
    totalCurrentDraw += current;
  }
  for (int i = 0; i < swerveMods.size(); i++) {
    wpi::units::meter_t drivePos{driveStates[i](0, 0)};
    wpi::units::meters_per_second_t driveRate{driveStates[i](1, 0)};
    wpi::units::radian_t steerPos{steerStates[i](0, 0)};
    wpi::units::radians_per_second_t steerRate{steerStates[i](1, 0)};
    swerveMods[i].SimulationUpdate(drivePos, driveRate, driveCurrents[i],
                                   steerPos, steerRate, steerCurrents[i]);
  }
  // gyroSim.SetRate(-swerveDriveSim.GetOmega());
  // gyroSim.SetAngle(-swerveDriveSim.GetPose().Rotation().Degrees());
}

wpi::math::Pose2d SwerveDrive::GetSimPose() const {
  return swerveDriveSim.GetPose();
}

wpi::units::ampere_t SwerveDrive::GetCurrentDraw() const { return totalCurrentDraw; }
