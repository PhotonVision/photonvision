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

#include <wpi/apriltag/AprilTagFieldLayout.hpp>
#include <wpi/apriltag/AprilTagFields.hpp>
#include <wpi/math/controller/SimpleMotorFeedforward.hpp>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/math/geometry/Translation2d.hpp>
#include <wpi/units/acceleration.hpp>
#include <wpi/units/angular_acceleration.hpp>
#include <wpi/units/angular_velocity.hpp>
#include <wpi/units/length.hpp>
#include <wpi/units/velocity.hpp>

namespace constants {
namespace Vision {
inline constexpr std::string_view kCameraName{"YOUR CAMERA NAME"};
inline const wpi::math::Transform3d kRobotToCam{
    wpi::math::Translation3d{0.5_m, 0.0_m, 0.5_m},
    wpi::math::Rotation3d{0_rad, -30_deg, 0_rad}};
inline const wpi::apriltag::AprilTagFieldLayout kTagLayout{
    wpi::apriltag::AprilTagFieldLayout::LoadField(
        wpi::apriltag::AprilTagField::kDefaultField)};

inline const Eigen::Matrix<double, 3, 1> kSingleTagStdDevs{4, 4, 8};
inline const Eigen::Matrix<double, 3, 1> kMultiTagStdDevs{0.5, 0.5, 1};
}  // namespace Vision
namespace Swerve {
using namespace units;

inline constexpr wpi::units::meter_t kTrackWidth{18.5_in};
inline constexpr wpi::units::meter_t kTrackLength{18.5_in};
inline constexpr wpi::units::meter_t kRobotWidth{25_in + 3.25_in * 2};
inline constexpr wpi::units::meter_t kRobotLength{25_in + 3.25_in * 2};
inline constexpr wpi::units::meters_per_second_t kMaxLinearSpeed{15.5_fps};
inline constexpr wpi::units::radians_per_second_t kMaxAngularSpeed{720_deg_per_s};
inline constexpr wpi::units::meter_t kWheelDiameter{4_in};
inline constexpr wpi::units::meter_t kWheelCircumference{kWheelDiameter *
                                                    std::numbers::pi};

inline constexpr double kDriveGearRatio = 6.75;
inline constexpr double kSteerGearRatio = 12.8;

inline constexpr wpi::units::meter_t kDriveDistPerPulse =
    kWheelCircumference / 1024.0 / kDriveGearRatio;
inline constexpr wpi::units::radian_t kSteerRadPerPulse =
    wpi::units::radian_t{2 * std::numbers::pi} / 1024.0;

inline constexpr double kDriveKP = 1.0;
inline constexpr double kDriveKI = 0.0;
inline constexpr double kDriveKD = 0.0;

inline constexpr double kSteerKP = 20.0;
inline constexpr double kSteerKI = 0.0;
inline constexpr double kSteerKD = 0.25;

using namespace units;

inline const wpi::math::SimpleMotorFeedforward<wpi::units::meters> kDriveFF{
    0.25_V, 2.5_V / 1_mps, 0.3_V / 1_mps_sq};

inline const wpi::math::SimpleMotorFeedforward<wpi::units::radians> kSteerFF{
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
  const wpi::math::Translation2d centerOffset;

  ModuleConstants(int moduleNum, int driveMotorId, int driveEncoderA,
                  int driveEncoderB, int steerMotorId, int steerEncoderA,
                  int steerEncoderB, double angleOffset, wpi::units::meter_t xOffset,
                  wpi::units::meter_t yOffset)
      : moduleNum(moduleNum),
        driveMotorId(driveMotorId),
        driveEncoderA(driveEncoderA),
        driveEncoderB(driveEncoderB),
        steerMotorId(steerMotorId),
        steerEncoderA(steerEncoderA),
        steerEncoderB(steerEncoderB),
        angleOffset(angleOffset),
        centerOffset(wpi::math::Translation2d{xOffset, yOffset}) {}
};

inline const ModuleConstants FL_CONSTANTS{
    1, 0, 0, 1, 1, 2, 3, 0, kTrackLength / 2, kTrackWidth / 2};
inline const ModuleConstants FR_CONSTANTS{
    2, 2, 4, 5, 3, 6, 7, 0, kTrackLength / 2, -kTrackWidth / 2};
inline const ModuleConstants BL_CONSTANTS{
    3, 4, 8, 9, 5, 10, 11, 0, -kTrackLength / 2, kTrackWidth / 2};
inline const ModuleConstants BR_CONSTANTS{
    4, 6, 12, 13, 7, 14, 15, 0, -kTrackLength / 2, -kTrackWidth / 2};
}  // namespace Swerve
}  // namespace constants
