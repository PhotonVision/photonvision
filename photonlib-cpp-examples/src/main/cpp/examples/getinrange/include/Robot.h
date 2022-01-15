/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

#include <photonlib/PhotonCamera.h>

#include <frc/TimedRobot.h>
#include <frc/XboxController.h>
#include <frc/controller/PIDController.h>
#include <frc/drive/DifferentialDrive.h>
#include <frc/motorcontrol/PWMVictorSPX.h>
#include <units/angle.h>
#include <units/length.h>

class Robot : public frc::TimedRobot {
 public:
  void TeleopPeriodic() override;

 private:
  // Constants such as camera and target height stored. Change per robot and
  // goal!
  const units::meter_t CAMERA_HEIGHT = 24_in;
  const units::meter_t TARGET_HEIGHT = 5_ft;

  // Angle between horizontal and the camera.
  const units::radian_t CAMERA_PITCH = 0_deg;

  // How far from the target we want to be
  const units::meter_t GOAL_RANGE_METERS = 3_ft;

  // PID constants should be tuned per robot
  const double P_GAIN = 0.1;
  const double D_GAIN = 0.0;
  frc2::PIDController controller{P_GAIN, 0.0, D_GAIN};

  // Change this to match the name of your camera
  photonlib::PhotonCamera camera{"photonvision"};

  frc::XboxController xboxController{0};

  // Drive motors
  frc::PWMVictorSPX leftMotor{0};
  frc::PWMVictorSPX rightMotor{1};
  frc::DifferentialDrive drive{leftMotor, rightMotor};
};
