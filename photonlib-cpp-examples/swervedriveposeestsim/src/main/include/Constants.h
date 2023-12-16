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

#include <numbers>

#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/apriltag/AprilTagFields.h>
#include <frc/controller/SimpleMotorFeedforward.h>
#include <frc/geometry/Transform3d.h>
#include <frc/geometry/Translation2d.h>
#include <units/length.h>

namespace constants {
namespace Vision {
static constexpr std::string_view kCameraName{"YOUR CAMERA NAME"};
static const frc::Transform3d kRobotToCam{
    frc::Translation3d{0.5_m, 0.0_m, 0.5_m},
    frc::Rotation3d{0_rad, 0_rad, 0_rad}};
static const frc::AprilTagFieldLayout kTagLayout{
    frc::LoadAprilTagLayoutField(frc::AprilTagField::k2023ChargedUp)};

static const Eigen::Matrix<double, 3, 1> kSingleTagStdDevs{4, 4, 8};
static const Eigen::Matrix<double, 3, 1> kMultiTagStdDevs{0.5, 0.5, 1};
}  // namespace Vision
namespace Swerve {

static constexpr units::meter_t kTrackWidth{18.5_in};
static constexpr units::meter_t kTrackLength{18.5_in};
static constexpr units::meter_t kRobotWidth{25_in + 3.25_in * 2};
static constexpr units::meter_t kRobotLength{25_in + 3.25_in * 2};
static constexpr units::meters_per_second_t kMaxLinearSpeed{15.5_fps};
static constexpr units::radians_per_second_t kMaxAngularSpeed{720_deg_per_s};
static constexpr units::meter_t kWheelDiameter{4_in};
static constexpr units::meter_t kWheelCircumference{kWheelDiameter *
                                                    std::numbers::pi};

static constexpr double kDriveGearRatio = 6.75;
static constexpr double kSteerGearRatio = 12.8;

static constexpr units::meter_t kDriveDistPerPulse =
    kWheelCircumference / 1024.0 / kDriveGearRatio;
static constexpr units::radian_t kSteerRadPerPulse =
    units::radian_t{2 * std::numbers::pi} / 1024.0;

static constexpr double kDriveKP = 1.0;
static constexpr double kDriveKI = 0.0;
static constexpr double kDriveKD = 0.0;

static constexpr double kSteerKP = 20.0;
static constexpr double kSteerKI = 0.0;
static constexpr double kSteerKD = 0.25;

static const frc::SimpleMotorFeedforward<units::meters> kDriveFF{
    0.25_V, 2.5_V / 1_mps, 0.3_V / 1_mps_sq};

static const frc::SimpleMotorFeedforward<units::radians> kSteerFF{
    0.5_V, 0.25_V / 1_rad_per_s, 0.01_V / 1_rad_per_s_sq};

struct ModuleConstants {
 public:
  const int moduleNum;
  const int driveMotorId;
  const int driveEncoderA;
  const int driveEncoderB;
  const int steerMotorId;
  const int steerEncoderA;
  const int steerEncoderB;
  const double angleOffset;
  const frc::Translation2d centerOffset;

  ModuleConstants(int moduleNum, int driveMotorId, int driveEncoderA,
                  int driveEncoderB, int steerMotorId, int steerEncoderA,
                  int steerEncoderB, double angleOffset, units::meter_t xOffset,
                  units::meter_t yOffset)
      : moduleNum(moduleNum),
        driveMotorId(driveMotorId),
        driveEncoderA(driveEncoderA),
        driveEncoderB(driveEncoderB),
        steerMotorId(steerMotorId),
        steerEncoderA(steerEncoderA),
        steerEncoderB(steerEncoderB),
        angleOffset(angleOffset),
        centerOffset(frc::Translation2d{xOffset, yOffset}) {}
};

static const ModuleConstants FL_CONSTANTS{
    1, 0, 0, 1, 1, 2, 3, 0, kTrackLength / 2, kTrackWidth / 2};
static const ModuleConstants FR_CONSTANTS{
    2, 2, 4, 5, 3, 6, 7, 0, kTrackLength / 2, -kTrackWidth / 2};
static const ModuleConstants BL_CONSTANTS{
    3, 4, 8, 9, 5, 10, 11, 0, -kTrackLength / 2, kTrackWidth / 2};
static const ModuleConstants BR_CONSTANTS{
    4, 6, 12, 13, 7, 14, 15, 0, -kTrackLength / 2, -kTrackWidth / 2};
}  // namespace Swerve
}  // namespace constants
