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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package frc.robot;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import frc.robot.Constants.DriveTrainConstants;

public class Robot extends TimedRobot {
    private XboxController m_controller;
    private Drivetrain m_drive;

    // Slew rate limiters to make joystick inputs more gentle; 1/3 sec from 0 to 1.
    private final SlewRateLimiter m_speedLimiter = new SlewRateLimiter(3);
    private final SlewRateLimiter m_rotLimiter = new SlewRateLimiter(3);

    @Override
    public void robotInit() {
        if (Robot.isSimulation()) {
            NetworkTableInstance instance = NetworkTableInstance.getDefault();
            instance.stopServer();
            // set the NT server if simulating this code.
            // "localhost" for photon on desktop, or "photonvision.local" / "[ip-address]" for coprocessor
            instance.setServer("localhost");
            instance.startClient4("myRobot");
        }

        m_controller = new XboxController(0);
        m_drive = new Drivetrain();
    }

    @Override
    public void robotPeriodic() {
        m_drive.updateOdometry();
    }

    @Override
    public void autonomousInit() {}

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void teleopPeriodic() {
        // Get the x speed. We are inverting this because Xbox controllers return
        // negative values when we push forward.
        var joyY = m_controller.getLeftY();
        if (Math.abs(joyY) < 0.075) {
            joyY = 0;
        }
        final var xSpeed = -m_speedLimiter.calculate(joyY) * DriveTrainConstants.kMaxSpeed;

        // Get the rate of angular rotation. We are inverting this because we want a
        // positive value when we pull to the left (remember, CCW is positive in
        // mathematics). Xbox controllers return positive values when you pull to
        // the right by default.
        var joyX = m_controller.getRightX();
        if (Math.abs(joyX) < 0.075) {
            joyX = 0;
        }
        final var rot = -m_rotLimiter.calculate(joyX) * DriveTrainConstants.kMaxAngularSpeed;

        m_drive.drive(xSpeed, rot);
    }

    @Override
    public void simulationPeriodic() {
        m_drive.simulationPeriodic();
    }
}
