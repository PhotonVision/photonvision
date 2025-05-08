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

#include "subsystems/SwerveDriveSim.h"

#include <iostream>

#include <frc/RobotController.h>
#include <frc/system/Discretization.h>

template <typename T>
int sgn(T val) {
  return (T(0) < val) - (val < T(0));
}

SwerveDriveSim::SwerveDriveSim(
    const frc::SimpleMotorFeedforward<units::meters>& driveFF,
    const frc::DCMotor& driveMotor, double driveGearing,
    units::meter_t driveWheelRadius,
    const frc::SimpleMotorFeedforward<units::radians>& steerFF,
    const frc::DCMotor& steerMotor, double steerGearing,
    const frc::SwerveDriveKinematics<numModules>& kinematics)
    : SwerveDriveSim(
          frc::LinearSystem<2, 1, 2>{
              (Eigen::MatrixXd(2, 2) << 0.0, 1.0, 0.0,
               -driveFF.GetKv().to<double>() / driveFF.GetKa().to<double>())
                  .finished(),
              Eigen::Matrix<double, 2, 1>{0.0,
                                          1.0 / driveFF.GetKa().to<double>()},
              (Eigen::MatrixXd(2, 2) << 1.0, 0.0, 0.0, 1.0).finished(),
              Eigen::Matrix<double, 2, 1>{0.0, 0.0}},
          driveFF.GetKs(), driveMotor, driveGearing, driveWheelRadius,
          frc::LinearSystem<2, 1, 2>{
              (Eigen::MatrixXd(2, 2) << 0.0, 1.0, 0.0,
               -steerFF.GetKv().to<double>() / steerFF.GetKa().to<double>())
                  .finished(),
              Eigen::Matrix<double, 2, 1>{0.0,
                                          1.0 / steerFF.GetKa().to<double>()},
              (Eigen::MatrixXd(2, 2) << 1.0, 0.0, 0.0, 1.0).finished(),
              Eigen::Matrix<double, 2, 1>{0.0, 0.0}},
          steerFF.GetKs(), steerMotor, steerGearing, kinematics) {}

SwerveDriveSim::SwerveDriveSim(
    const frc::LinearSystem<2, 1, 2>& drivePlant, units::volt_t driveKs,
    const frc::DCMotor& driveMotor, double driveGearing,
    units::meter_t driveWheelRadius,
    const frc::LinearSystem<2, 1, 2>& steerPlant, units::volt_t steerKs,
    const frc::DCMotor& steerMotor, double steerGearing,
    const frc::SwerveDriveKinematics<numModules>& kinematics)
    : drivePlant(drivePlant),
      driveKs(driveKs),
      driveMotor(driveMotor),
      driveGearing(driveGearing),
      driveWheelRadius(driveWheelRadius),
      steerPlant(steerPlant),
      steerKs(steerKs),
      steerMotor(steerMotor),
      steerGearing(steerGearing),
      kinematics(kinematics) {}

void SwerveDriveSim::SetDriveInputs(
    const std::array<units::volt_t, numModules>& inputs) {
  units::volt_t battVoltage = frc::RobotController::GetBatteryVoltage();
  for (int i = 0; i < driveInputs.size(); i++) {
    units::volt_t input = inputs[i];
    driveInputs[i] = std::clamp(input, -battVoltage, battVoltage);
  }
}

void SwerveDriveSim::SetSteerInputs(
    const std::array<units::volt_t, numModules>& inputs) {
  units::volt_t battVoltage = frc::RobotController::GetBatteryVoltage();
  for (int i = 0; i < steerInputs.size(); i++) {
    units::volt_t input = inputs[i];
    steerInputs[i] = std::clamp(input, -battVoltage, battVoltage);
  }
}

Eigen::Matrix<double, 2, 1> SwerveDriveSim::CalculateX(
    const Eigen::Matrix<double, 2, 2>& discA,
    const Eigen::Matrix<double, 2, 1>& discB,
    const Eigen::Matrix<double, 2, 1>& x, units::volt_t input,
    units::volt_t kS) {
  auto Ax = discA * x;
  double nextStateVel = Ax(1, 0);
  double inputToStop = nextStateVel / -discB(1, 0);
  double ksSystemEffect =
      std::clamp(inputToStop, -kS.to<double>(), kS.to<double>());

  nextStateVel += discB(1, 0) * ksSystemEffect;
  inputToStop = nextStateVel / -discB(1, 0);
  double signToStop = sgn(inputToStop);
  double inputSign = sgn(input.to<double>());
  double ksInputEffect = 0;

  if (std::abs(ksSystemEffect) < kS.to<double>()) {
    double absInput = std::abs(input.to<double>());
    ksInputEffect =
        -std::clamp(kS.to<double>() * inputSign, -absInput, absInput);
  } else if ((input.to<double>() * signToStop) > (inputToStop * signToStop)) {
    double absInput = std::abs(input.to<double>() - inputToStop);
    ksInputEffect =
        -std::clamp(kS.to<double>() * inputSign, -absInput, absInput);
  }

  auto sF = Eigen::Matrix<double, 1, 1>{input.to<double>() + ksSystemEffect +
                                        ksInputEffect};
  auto Bu = discB * sF;
  auto retVal = Ax + Bu;
  return retVal;
}

void SwerveDriveSim::Update(units::second_t dt) {
  Eigen::Matrix<double, 2, 2> driveDiscA;
  Eigen::Matrix<double, 2, 1> driveDiscB;
  frc::DiscretizeAB<2, 1>(drivePlant.A(), drivePlant.B(), dt, &driveDiscA,
                          &driveDiscB);

  Eigen::Matrix<double, 2, 2> steerDiscA;
  Eigen::Matrix<double, 2, 1> steerDiscB;
  frc::DiscretizeAB<2, 1>(steerPlant.A(), steerPlant.B(), dt, &steerDiscA,
                          &steerDiscB);

  std::array<frc::SwerveModulePosition, 4> moduleDeltas;

  for (int i = 0; i < numModules; i++) {
    double prevDriveStatePos = driveStates[i](0, 0);
    driveStates[i] = CalculateX(driveDiscA, driveDiscB, driveStates[i],
                                driveInputs[i], driveKs);
    double currentDriveStatePos = driveStates[i](0, 0);
    steerStates[i] = CalculateX(steerDiscA, steerDiscB, steerStates[i],
                                steerInputs[i], steerKs);
    double currentSteerStatePos = steerStates[i](0, 0);
    moduleDeltas[i] = frc::SwerveModulePosition{
        units::meter_t{currentDriveStatePos - prevDriveStatePos},
        frc::Rotation2d{units::radian_t{currentSteerStatePos}}};
  }

  frc::Twist2d twist = kinematics.ToTwist2d(moduleDeltas);
  pose = pose.Exp(twist);
  omega = twist.dtheta / dt;
}

void SwerveDriveSim::Reset(const frc::Pose2d& pose, bool preserveMotion) {
  this->pose = pose;
  if (!preserveMotion) {
    for (int i = 0; i < numModules; i++) {
      driveStates[i] = Eigen::Matrix<double, 2, 1>{0, 0};
      steerStates[i] = Eigen::Matrix<double, 2, 1>{0, 0};
    }
    omega = 0_rad_per_s;
  }
}

void SwerveDriveSim::Reset(const frc::Pose2d& pose,
                           const std::array<Eigen::Matrix<double, 2, 1>,
                                            numModules>& moduleDriveStates,
                           const std::array<Eigen::Matrix<double, 2, 1>,
                                            numModules>& moduleSteerStates) {
  this->pose = pose;
  driveStates = moduleDriveStates;
  steerStates = moduleSteerStates;
  omega = kinematics.ToChassisSpeeds(GetModuleStates()).omega;
}

frc::Pose2d SwerveDriveSim::GetPose() const { return pose; }

std::array<frc::SwerveModulePosition, numModules>
SwerveDriveSim::GetModulePositions() const {
  std::array<frc::SwerveModulePosition, numModules> positions;
  for (int i = 0; i < numModules; i++) {
    positions[i] = frc::SwerveModulePosition{
        units::meter_t{driveStates[i](0, 0)},
        frc::Rotation2d{units::radian_t{steerStates[i](0, 0)}}};
  }
  return positions;
}

std::array<frc::SwerveModulePosition, numModules>
SwerveDriveSim::GetNoisyModulePositions(units::meter_t driveStdDev,
                                        units::radian_t steerStdDev) {
  std::array<frc::SwerveModulePosition, numModules> positions;
  for (int i = 0; i < numModules; i++) {
    positions[i] = frc::SwerveModulePosition{
        units::meter_t{driveStates[i](0, 0)} +
            randDist(generator) * driveStdDev,
        frc::Rotation2d{units::radian_t{steerStates[i](0, 0)} +
                        randDist(generator) * steerStdDev}};
  }
  return positions;
}

std::array<frc::SwerveModuleState, numModules>
SwerveDriveSim::GetModuleStates() {
  std::array<frc::SwerveModuleState, numModules> states;
  for (int i = 0; i < numModules; i++) {
    states[i] = frc::SwerveModuleState{
        units::meters_per_second_t{driveStates[i](1, 0)},
        frc::Rotation2d{units::radian_t{steerStates[i](0, 0)}}};
  }
  return states;
}

std::array<Eigen::Matrix<double, 2, 1>, numModules>
SwerveDriveSim::GetDriveStates() const {
  return driveStates;
}

std::array<Eigen::Matrix<double, 2, 1>, numModules>
SwerveDriveSim::GetSteerStates() const {
  return steerStates;
}

units::radians_per_second_t SwerveDriveSim::GetOmega() const { return omega; }

units::ampere_t SwerveDriveSim::GetCurrentDraw(
    const frc::DCMotor& motor, units::radians_per_second_t velocity,
    units::volt_t inputVolts, units::volt_t batteryVolts) const {
  units::volt_t effVolts = inputVolts - velocity / motor.Kv;
  if (inputVolts >= 0_V) {
    effVolts = std::clamp(effVolts, 0_V, inputVolts);
  } else {
    effVolts = std::clamp(effVolts, inputVolts, 0_V);
  }
  auto retVal = (inputVolts / batteryVolts) * (effVolts / motor.R);
  return retVal;
}

std::array<units::ampere_t, numModules> SwerveDriveSim::GetDriveCurrentDraw()
    const {
  std::array<units::ampere_t, numModules> currents;
  for (int i = 0; i < numModules; i++) {
    units::radians_per_second_t speed =
        units::radians_per_second_t{driveStates[i](1, 0)} * driveGearing /
        driveWheelRadius.to<double>();
    currents[i] = GetCurrentDraw(driveMotor, speed, driveInputs[i],
                                 frc::RobotController::GetBatteryVoltage());
  }
  return currents;
}

std::array<units::ampere_t, numModules> SwerveDriveSim::GetSteerCurrentDraw()
    const {
  std::array<units::ampere_t, numModules> currents;
  for (int i = 0; i < numModules; i++) {
    units::radians_per_second_t speed =
        units::radians_per_second_t{steerStates[i](1, 0) * steerGearing};
    // TODO: If uncommented we get huge current values.. Not sure how to fix
    // atm. :(
    currents[i] = 20_A;
    // currents[i] = GetCurrentDraw(steerMotor, speed, steerInputs[i],
    // frc::RobotController::GetBatteryVoltage());
  }
  return currents;
}

units::ampere_t SwerveDriveSim::GetTotalCurrentDraw() const {
  units::ampere_t total{0};
  for (const auto& val : GetDriveCurrentDraw()) {
    total += val;
  }
  for (const auto& val : GetSteerCurrentDraw()) {
    total += val;
  }
  return total;
}
