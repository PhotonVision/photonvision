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

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.PWMVictorSPX;
import org.photonvision.PhotonCamera;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
    // Constants such as camera and target height stored. Change per robot and goal!
    final double CAMERA_HEIGHT_METERS = Units.inchesToMeters(24);
    final double TARGET_HEIGHT_METERS = Units.feetToMeters(5);
    // Angle between horizontal and the camera.
    final double CAMERA_PITCH_RADIANS = Units.degreesToRadians(0);

    // How far from the target we want to be
    final double GOAL_RANGE_METERS = Units.feetToMeters(3);

    // Change this to match the name of your camera
    PhotonCamera camera = new PhotonCamera("photonvision");

    // PID constants should be tuned per robot
    final double LINEAR_P = 0.1;
    final double LINEAR_D = 0.0;
    PIDController forwardController = new PIDController(LINEAR_P, 0, LINEAR_D);

    final double ANGULAR_P = 0.1;
    final double ANGULAR_D = 0.0;
    PIDController turnController = new PIDController(ANGULAR_P, 0, ANGULAR_D);

    XboxController xboxController = new XboxController(0);

    // Drive motors
    PWMVictorSPX leftMotor = new PWMVictorSPX(0);
    PWMVictorSPX rightMotor = new PWMVictorSPX(1);
    DifferentialDrive drive = new DifferentialDrive(leftMotor, rightMotor);

    @Override
    public void teleopPeriodic() {
        double forwardSpeed;
        double rotationSpeed;

        forwardSpeed = -xboxController.getRightY();

        if (xboxController.getAButton()) {
            // Vision-alignment mode
            // Query the latest result from PhotonVision
            var result = camera.getLatestResult();

            if (result.hasTargets()) {
                // Calculate angular turn power
                // -1.0 required to ensure positive PID controller effort _increases_ yaw
                rotationSpeed = -turnController.calculate(result.getBestTarget().getYaw(), 0);
            } else {
                // If we have no targets, stay still.
                rotationSpeed = 0;
            }
        } else {
            // Manual Driver Mode
            rotationSpeed = xboxController.getLeftX();
        }

        // Use our forward/turn speeds to control the drivetrain
        drive.arcadeDrive(forwardSpeed, rotationSpeed);
    }
}
