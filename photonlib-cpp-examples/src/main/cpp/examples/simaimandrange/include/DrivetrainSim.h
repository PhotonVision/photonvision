/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include <photonlib/SimVisionSystem.h>

#include <frc/DriverStation.h>
#include <frc/RobotController.h>
#include <frc/geometry/Pose2d.h>
#include <frc/geometry/Rotation2d.h>
#include <frc/geometry/Translation2d.h>
#include <frc/simulation/DifferentialDrivetrainSim.h>
#include <frc/simulation/PWMSim.h>
#include <frc/smartdashboard/Field2d.h>
#include <frc/smartdashboard/SmartDashboard.h>
#include <frc/system/plant/LinearSystemId.h>
#include <units/angle.h>
#include <units/length.h>
#include <units/time.h>

#pragma once

class DrivetrainSim {
 public:
  DrivetrainSim() {
    simVision.AddSimVisionTarget(photonlib::SimVisionTarget(
        farTargetPose, 81.91_in, targetWidth, targetHeight));
    frc::SmartDashboard::PutData("Field", &field);
  }

  void update();

 private:
  // Simulated Motor Controllers
  frc::sim::PWMSim leftLeader{0};
  frc::sim::PWMSim rightLeader{1};

  // Simulation Physics
  // Configure these to match your drivetrain's physical dimensions
  // and characterization results.
  static constexpr decltype(1_V / 1_mps) kv = 1.98 * 1_V * 1_s / 1_m;
  static constexpr decltype(1_V / 1_mps_sq) ka = 0.2 * 1_V * 1_s * 1_s / 1_m;
  static constexpr decltype(1_V / 1_rad_per_s) kvAngular =
      1.5 * 1_V * 1_s / 1_rad;
  static constexpr decltype(1_V / 1_rad_per_s_sq) kaAngular =
      0.3 * 1_V * 1_s * 1_s / 1_rad;
  static constexpr units::meter_t kTrackWidth = 1_m;

  const frc::LinearSystem<2, 2, 2> kDrivetrainPlant =
      frc::LinearSystemId::IdentifyDrivetrainSystem(kv, ka, kvAngular,
                                                    kaAngular, kTrackWidth);

  frc::sim::DifferentialDrivetrainSim m_drivetrainSimulator{
      kDrivetrainPlant,     2.0_ft,
      frc::DCMotor::CIM(2), 8.0,
      6.0_in / 2,           {0.001, 0.001, 0.0001, 0.1, 0.1, 0.005, 0.005}};

  // Simulated Vision System.
  // Configure these to match your PhotonVision Camera,
  // pipeline, and LED setup.
  units::degree_t camDiagFOV = 170.0_deg;  // assume wide-angle camera
  units::degree_t camPitch = 15_deg;
  units::meter_t camHeightOffGround = 24_in;
  units::meter_t maxLEDRange = 20_m;
  int camResolutionWidth = 640;   // pixels
  int camResolutionHeight = 480;  // pixels
  double minTargetArea = 10;      // square pixels

  photonlib::SimVisionSystem simVision{
      "photonvision",     camDiagFOV,          camPitch,
      frc::Transform2d{}, camHeightOffGround,  maxLEDRange,
      camResolutionWidth, camResolutionHeight, minTargetArea};

  // See
  // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/2020FieldDrawing-SeasonSpecific.pdf
  // page 208
  const units::meter_t targetWidth = 41.30_in - 6.70_in;
  // See
  // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/2020FieldDrawing-SeasonSpecific.pdf
  // page 197
  const units::meter_t targetHeight = 98.19_in - 81.19_in;  // meters
  // See
  // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/LayoutandMarkingDiagram.pdf
  // pages 4 and 5
  const units::meter_t tgtXPos = 54_ft;
  const units::meter_t tgtYPos = (27.0_ft / 2) - 43.75_in - (48.0_in / 2.0);
  const frc::Translation2d targetTrans{tgtXPos, tgtYPos};
  const frc::Rotation2d targetRot{0.0_deg};
  frc::Pose2d farTargetPose{targetTrans, targetRot};

  frc::Field2d field{};
};
