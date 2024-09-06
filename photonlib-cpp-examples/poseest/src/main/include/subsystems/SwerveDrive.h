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

#include <frc/ADXRS450_Gyro.h>
#include <frc/SPI.h>
#include <frc/estimator/SwerveDrivePoseEstimator.h>
#include <frc/kinematics/SwerveDriveKinematics.h>
#include <frc/simulation/ADXRS450_GyroSim.h>

#include "SwerveDriveSim.h"
#include "SwerveModule.h"

class SwerveDrive {
 public:
  SwerveDrive();
  void Periodic();
  void Drive(units::meters_per_second_t vx, units::meters_per_second_t vy,
             units::radians_per_second_t omega);
  void SetChassisSpeeds(const frc::ChassisSpeeds& targetChassisSpeeds,
                        bool openLoop, bool steerInPlace);
  void SetModuleStates(
      const std::array<frc::SwerveModuleState, 4>& desiredStates, bool openLoop,
      bool steerInPlace);
  void Stop();
  void AddVisionMeasurement(const frc::Pose2d& visionMeasurement,
                            units::second_t timestamp);
  void AddVisionMeasurement(const frc::Pose2d& visionMeasurement,
                            units::second_t timestamp,
                            const Eigen::Vector3d& stdDevs);
  void ResetPose(const frc::Pose2d& pose, bool resetSimPose);
  frc::Pose2d GetPose() const;
  frc::Rotation2d GetHeading() const;
  frc::Rotation2d GetGyroYaw() const;
  frc::ChassisSpeeds GetChassisSpeeds() const;
  std::array<frc::SwerveModuleState, 4> GetModuleStates() const;
  std::array<frc::SwerveModulePosition, 4> GetModulePositions() const;
  std::array<frc::Pose2d, 4> GetModulePoses() const;
  void Log();
  void SimulationPeriodic();
  frc::Pose2d GetSimPose() const;
  units::ampere_t GetCurrentDraw() const;

 private:
  std::array<SwerveModule, 4> swerveMods{
      SwerveModule{constants::Swerve::FL_CONSTANTS},
      SwerveModule{constants::Swerve::FR_CONSTANTS},
      SwerveModule{constants::Swerve::BL_CONSTANTS},
      SwerveModule{constants::Swerve::BR_CONSTANTS}};
  frc::SwerveDriveKinematics<4> kinematics{
      swerveMods[0].GetModuleConstants().centerOffset,
      swerveMods[1].GetModuleConstants().centerOffset,
      swerveMods[2].GetModuleConstants().centerOffset,
      swerveMods[3].GetModuleConstants().centerOffset,
  };
  frc::ADXRS450_Gyro gyro{frc::SPI::Port::kOnboardCS0};
  frc::SwerveDrivePoseEstimator<4> poseEstimator;
  frc::ChassisSpeeds targetChassisSpeeds{};

  frc::sim::ADXRS450_GyroSim gyroSim;
  SwerveDriveSim swerveDriveSim;
  units::ampere_t totalCurrentDraw{0};
};
