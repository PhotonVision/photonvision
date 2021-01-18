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

package org.photonlib.examples.simposeest.sim;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Transform2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.simulation.AnalogGyroSim;
import edu.wpi.first.wpilibj.simulation.DifferentialDrivetrainSim;
import edu.wpi.first.wpilibj.simulation.EncoderSim;
import edu.wpi.first.wpilibj.simulation.PWMSim;
import edu.wpi.first.wpilibj.system.LinearSystem;
import edu.wpi.first.wpilibj.system.plant.DCMotor;
import edu.wpi.first.wpilibj.system.plant.LinearSystemId;
import edu.wpi.first.wpiutil.math.numbers.N2;
import org.photonlib.examples.simposeest.robot.Constants;
import org.photonvision.SimVisionSystem;

/**
* Implementation of a simulation of robot physics, sensors, motor controllers Includes a Simulated
* PhotonVision system and one vision target.
*
* <p>This class and its methods are only relevant during simulation. While on the real robot, the
* real motors/sensors/physics are used instead.
*/
public class DrivetrainSim {

    // Simulated Sensors
    AnalogGyroSim gyroSim = new AnalogGyroSim(Constants.kGyroPin);
    EncoderSim leftEncoderSim = EncoderSim.createForChannel(Constants.kDtLeftEncoderPinA);
    EncoderSim rightEncoderSim = EncoderSim.createForChannel(Constants.kDtRightEncoderPinA);

    // Simulated Motor Controllers
    PWMSim leftLeader = new PWMSim(Constants.kDtLeftLeaderPin);
    PWMSim leftFollower = new PWMSim(Constants.kDtLeftFollowerPin);
    PWMSim rightLeader = new PWMSim(Constants.kDtRightLeaderPin);
    PWMSim rightFollower = new PWMSim(Constants.kDtRightFollowerPin);

    // Simulation Physics
    // Configure these to match your drivetrain's physical dimensions
    // and characterization results.
    LinearSystem<N2, N2, N2> drivetrainSystem =
            LinearSystemId.identifyDrivetrainSystem(1.98, 0.2, 1.5, 0.3);
    DifferentialDrivetrainSim drivetrainSimulator =
            new DifferentialDrivetrainSim(
                    drivetrainSystem,
                    DCMotor.getCIM(2),
                    8,
                    Constants.kTrackWidth,
                    Constants.kWheelRadius,
                    null);

    // Simulated Vision System.
    // Configure these to match your PhotonVision Camera,
    // pipeline, and LED setup.
    double camDiagFOV = 75.0; // degrees
    double camPitch = 15.0; // degrees
    double camHeightOffGround = 0.85; // meters
    double maxLEDRange = 20; // meters
    int camResolutionWidth = 640; // pixels
    int camResolutionHeight = 480; // pixels
    double minTargetArea = 10; // square pixels

    SimVisionSystem simVision =
            new SimVisionSystem(
                    Constants.kCamName,
                    camDiagFOV,
                    camPitch,
                    Constants.kCameraToRobot,
                    camHeightOffGround,
                    maxLEDRange,
                    camResolutionWidth,
                    camResolutionHeight,
                    minTargetArea);

    public DrivetrainSim() {
        simVision.addSimVisionTarget(Constants.kFarTarget);
    }

    /**
    * Perform all periodic drivetrain simulation related tasks to advance our simulation of robot
    * physics forward by a single 20ms step.
    */
    public void update() {

        double leftMotorCmd = 0;
        double rightMotorCmd = 0;

        if (DriverStation.getInstance().isEnabled() && !RobotController.isBrownedOut()) {
            // If the motor controllers are enabled...
            // Roughly model the effect of leader and follower motor pushing on the same
            // gearbox.
            // Note if the software is incorrect and drives them against each other, speed
            // will be
            // roughly matching the physical situation, but current draw will _not_ be
            // accurate.
            leftMotorCmd = (leftLeader.getSpeed() + leftFollower.getSpeed()) / 2.0;
            rightMotorCmd = (rightLeader.getSpeed() + rightFollower.getSpeed()) / 2.0;
        }

        // Update the physics simulation
        drivetrainSimulator.setInputs(
                leftMotorCmd * RobotController.getInputVoltage(),
                -rightMotorCmd * RobotController.getInputVoltage());
        drivetrainSimulator.update(0.02);

        // Update our sensors based on the simulated motion of the robot
        leftEncoderSim.setDistance((drivetrainSimulator.getLeftPositionMeters()));
        leftEncoderSim.setRate((drivetrainSimulator.getLeftVelocityMetersPerSecond()));
        rightEncoderSim.setDistance((drivetrainSimulator.getRightPositionMeters()));
        rightEncoderSim.setRate((drivetrainSimulator.getRightVelocityMetersPerSecond()));
        gyroSim.setAngle(
                -drivetrainSimulator
                        .getHeading()
                        .getDegrees()); // Gyros have an inverted reference frame for
        // angles, so multiply by -1.0;

        // Update PhotonVision based on our new robot position.
        simVision.processFrame(drivetrainSimulator.getPose());
    }

    /**
    * Resets the simulation back to a pre-defined pose Useful to simulate the action of placing the
    * robot onto a specific spot in the field (IE, at the start of each match).
    *
    * @param pose
    */
    public void resetPose(Pose2d pose) {
        drivetrainSimulator.setPose(pose);
    }

    /** @return The simulated robot's position, in meters. */
    public Pose2d getCurPose() {
        return drivetrainSimulator.getPose();
    }

    /**
    * For testing purposes only! Applies an unmodeled, undetected offset to the pose Similar to if
    * you magically kicked your robot to the side in a way the encoders and gyro didn't measure.
    *
    * <p>This distrubance should be corrected for once a vision target is in view.
    */
    public void applyKick() {
        Pose2d newPose =
                drivetrainSimulator
                        .getPose()
                        .transformBy(new Transform2d(new Translation2d(0, 0.5), new Rotation2d()));
        drivetrainSimulator.setPose(newPose);
    }
}
