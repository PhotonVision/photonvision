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

private:
  // Change this to match the name of your camera
  photonlib::PhotonCamera camera{"photonvision"};
  // PID constants should be tuned per robot
  frc2::PIDController controller{.1, 0, 0};

  frc::XboxController xboxController{0};

  // Drive motors
  frc::PWMVictorSPX leftMotor{0};
  frc::PWMVictorSPX rightMotor{1};
  frc::DifferentialDrive drive{leftMotor, rightMotor};
};
