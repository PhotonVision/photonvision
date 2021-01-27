/**
 * Copyright (C) 2018-2020 Photon Vision.
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

#pragma once

#include "DrivetrainSim.h"

#include <photonlib/PhotonCamera.h>

#include <frc/PWMVictorSPX.h>
#include <frc/TimedRobot.h>
#include <frc/XboxController.h>
#include <frc/controller/PIDController.h>
#include <frc/drive/DifferentialDrive.h>
#include <units/angle.h>
#include <units/length.h>

class Robot : public frc::TimedRobot {
public:
  void TeleopPeriodic() override;
  void SimulationPeriodic() override;

private:
  // Constants such as camera and target height stored. Change per robot and
  // goal!
  const units::meter_t CAMERA_HEIGHT = 24_in;
  const units::meter_t TARGET_HEIGHT = 81.19_in;

  // Angle between horizontal and the camera.
  const units::radian_t CAMERA_PITCH = 15_deg;

  // How far from the target we want to be
  const units::meter_t GOAL_RANGE_METERS = 10_ft;

  // PID constants should be tuned per robot
  const double LINEAR_P = 2.0;
  const double LINEAR_D = 0.0;
  frc2::PIDController forwardController{LINEAR_P, 0.0, LINEAR_D};

  const double ANGULAR_P = 0.03;
  const double ANGULAR_D = 0.003;
  frc2::PIDController turnController{ANGULAR_P, 0.0, ANGULAR_D};

  // Change this to match the name of your camera
  photonlib::PhotonCamera camera{"photonvision"};

  frc::XboxController xboxController{0};

  // Drive motors
  frc::PWMVictorSPX leftMotor{0};
  frc::PWMVictorSPX rightMotor{1};
  frc::DifferentialDrive drive{leftMotor, rightMotor};

  // Simulation Support
  DrivetrainSim dtSim{};
};
