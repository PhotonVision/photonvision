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

#include "Robot.h"

#include <frc/RobotBase.h>
#include <networktables/NetworkTable.h>

void Robot::RobotInit() {
  if constexpr (frc::RobotBase::IsSimulation()) {
    auto inst = nt::NetworkTableInstance::GetDefault();
    inst.StopServer();
    // set the NT server if simulating this code.
    // "localhost" for photon on desktop, or "photonvision.local" or
    // "[ip-address]" for coprocessor
    inst.SetServer("localhost");
    inst.StartClient4("Robot Simulation");
  }
}

void Robot::TeleopPeriodic() {
  // Get the x speed. We are inverting this because Xbox controllers return
  // negative values when we push forward.
  const auto xSpeed = -m_controller.GetLeftY() * Drivetrain::kMaxSpeed;

  // Get the rate of angular rotation. We are inverting this because we want a
  // positive value when we pull to the left (remember, CCW is positive in
  // mathematics). Xbox controllers return positive values when you pull to
  // the right by default.
  auto rot = -m_controller.GetRightX() * Drivetrain::kMaxAngularSpeed;

  m_drive.Drive(xSpeed, rot);
}

void Robot::RobotPeriodic() { m_drive.Periodic(); }
void Robot::SimulationPeriodic() { m_drive.SimulationPeriodic(); }

#ifndef RUNNING_FRC_TESTS
int main() { return frc::StartRobot<Robot>(); }
#endif
