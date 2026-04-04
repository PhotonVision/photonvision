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
#include <frc/simulation/ADXRS450_GyroSim.h>
#include <wpi/math/estimator/SwerveDrivePoseEstimator.hpp>
#include <wpi/math/kinematics/wpi/math/kinematics/SwerveDriveKinematics.hpppp>

#include "SwerveDriveSim.h"
#include "SwerveModule.h"

class SwerveDrive {
 public:
  SwerveDrive();
  void Periodic();
  void Drive(wpi::units::meters_per_second_t vx, wpi::units::meters_per_second_t vy,
             wpi::units::radians_per_second_t omega);
  void SetChassisSpeeds(const wpi::math::ChassisSpeeds& targetChassisSpeeds,
                        bool openLoop, bool steerInPlace);
  void SetModuleStates(
      const std::array<wpi::math::SwerveModuleState, 4>& desiredStates,
      bool openLoop, bool steerInPlace);
  void Stop();
  void AddVisionMeasurement(const wpi::math::Pose2d& visionMeasurement,
                            wpi::units::second_t timestamp);
  void AddVisionMeasurement(const wpi::math::Pose2d& visionMeasurement,
                            wpi::units::second_t timestamp,
                            const Eigen::Vector3d& stdDevs);
  void ResetPose(const wpi::math::Pose2d& pose, bool resetSimPose);
  wpi::math::Pose2d GetPose() const;
  wpi::math::Rotation2d GetHeading() const;
  wpi::math::Rotation2d GetGyroYaw() const;
  wpi::math::ChassisSpeeds GetChassisSpeeds() const;
  std::array<wpi::math::SwerveModuleState, 4> GetModuleStates() const;
  std::array<wpi::math::SwerveModulePosition, 4> GetModulePositions() const;
  std::array<wpi::math::Pose2d, 4> GetModulePoses() const;
  void Log();
  void SimulationPeriodic();
  wpi::math::Pose2d GetSimPose() const;
  wpi::units::ampere_t GetCurrentDraw() const;

 private:
  std::array<SwerveModule, 4> swerveMods{
      SwerveModule{constants::Swerve::FL_CONSTANTS},
      SwerveModule{constants::Swerve::FR_CONSTANTS},
      SwerveModule{constants::Swerve::BL_CONSTANTS},
      SwerveModule{constants::Swerve::BR_CONSTANTS}};
  wpi::math::SwerveDriveKinematics<4> kinematics{
      swerveMods[0].GetModuleConstants().centerOffset,
      swerveMods[1].GetModuleConstants().centerOffset,
      swerveMods[2].GetModuleConstants().centerOffset,
      swerveMods[3].GetModuleConstants().centerOffset,
  };
  wpi::ADXRS450_Gyro gyro{wpi::SPI::Port::kOnboardCS0};
  wpi::math::SwerveDrivePoseEstimator<4> poseEstimator;
  wpi::math::ChassisSpeeds targetChassisSpeeds{};

  wpi::sim::ADXRS450_GyroSim gyroSim;
  SwerveDriveSim swerveDriveSim;
  wpi::units::ampere_t totalCurrentDraw{0};
};
