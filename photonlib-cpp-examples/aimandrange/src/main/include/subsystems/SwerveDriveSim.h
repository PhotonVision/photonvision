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

#include <random>

#include <wpi/math/controller/SimpleMotorFeedforward.hpp>
#include <wpi/math/kinematics/wpi/math/kinematics/SwerveDriveKinematics.hpppp>
#include <wpi/math/system/LinearSystem.hpp>
#include <wpi/math/system/plant/DCMotor.hpp>
#include <wpi/units/voltage.hpp>

static constexpr int numModules{4};

class SwerveDriveSim {
 public:
  SwerveDriveSim(
      const wpi::math::SimpleMotorFeedforward<wpi::units::meters>& driveFF,
      const wpi::math::DCMotor& driveMotor, double driveGearing,
      wpi::units::meter_t driveWheelRadius,
      const wpi::math::SimpleMotorFeedforward<wpi::units::radians>& steerFF,
      const wpi::math::DCMotor& steerMotor, double steerGearing,
      const wpi::math::SwerveDriveKinematics<numModules>& kinematics);
  SwerveDriveSim(
      const wpi::math::LinearSystem<2, 1, 2>& drivePlant, wpi::units::volt_t driveKs,
      const wpi::math::DCMotor& driveMotor, double driveGearing,
      wpi::units::meter_t driveWheelRadius,
      const wpi::math::LinearSystem<2, 1, 2>& steerPlant, wpi::units::volt_t steerKs,
      const wpi::math::DCMotor& steerMotor, double steerGearing,
      const wpi::math::SwerveDriveKinematics<numModules>& kinematics);
  void SetDriveInputs(const std::array<wpi::units::volt_t, numModules>& inputs);
  void SetSteerInputs(const std::array<wpi::units::volt_t, numModules>& inputs);
  static Eigen::Matrix<double, 2, 1> CalculateX(
      const Eigen::Matrix<double, 2, 2>& discA,
      const Eigen::Matrix<double, 2, 1>& discB,
      const Eigen::Matrix<double, 2, 1>& x, wpi::units::volt_t input,
      wpi::units::volt_t kS);
  void Update(wpi::units::second_t dt);
  void Reset(const wpi::math::Pose2d& pose, bool preserveMotion);
  void Reset(const wpi::math::Pose2d& pose,
             const std::array<Eigen::Matrix<double, 2, 1>, numModules>&
                 moduleDriveStates,
             const std::array<Eigen::Matrix<double, 2, 1>, numModules>&
                 moduleSteerStates);
  wpi::math::Pose2d GetPose() const;
  std::array<wpi::math::SwerveModulePosition, numModules> GetModulePositions()
      const;
  std::array<wpi::math::SwerveModulePosition, numModules>
  GetNoisyModulePositions(wpi::units::meter_t driveStdDev,
                          wpi::units::radian_t steerStdDev);
  std::array<wpi::math::SwerveModuleState, numModules> GetModuleStates();
  std::array<Eigen::Matrix<double, 2, 1>, numModules> GetDriveStates() const;
  std::array<Eigen::Matrix<double, 2, 1>, numModules> GetSteerStates() const;
  wpi::units::radians_per_second_t GetOmega() const;
  wpi::units::ampere_t GetCurrentDraw(const wpi::math::DCMotor& motor,
                                 wpi::units::radians_per_second_t velocity,
                                 wpi::units::volt_t inputVolts,
                                 wpi::units::volt_t batteryVolts) const;
  std::array<wpi::units::ampere_t, numModules> GetDriveCurrentDraw() const;
  std::array<wpi::units::ampere_t, numModules> GetSteerCurrentDraw() const;
  wpi::units::ampere_t GetTotalCurrentDraw() const;

 private:
  std::random_device rd{};
  std::mt19937 generator{rd()};
  std::normal_distribution<double> randDist{0.0, 1.0};
  const wpi::math::LinearSystem<2, 1, 2> drivePlant;
  const wpi::units::volt_t driveKs;
  const wpi::math::DCMotor driveMotor;
  const double driveGearing;
  const wpi::units::meter_t driveWheelRadius;
  const wpi::math::LinearSystem<2, 1, 2> steerPlant;
  const wpi::units::volt_t steerKs;
  const wpi::math::DCMotor steerMotor;
  const double steerGearing;
  const wpi::math::SwerveDriveKinematics<numModules> kinematics;
  std::array<wpi::units::volt_t, numModules> driveInputs{};
  std::array<Eigen::Matrix<double, 2, 1>, numModules> driveStates{};
  std::array<wpi::units::volt_t, numModules> steerInputs{};
  std::array<Eigen::Matrix<double, 2, 1>, numModules> steerStates{};
  wpi::math::Pose2d pose{wpi::math::Pose2d{}};
  wpi::units::radians_per_second_t omega{0};
};
