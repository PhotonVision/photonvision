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

package org.photonlib.examples.simposeest.robot;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.PWMVictorSPX;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.controller.SimpleMotorFeedforward;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveWheelSpeeds;

/**
* Implements a controller for the drivetrain. Converts a set of chassis motion commands into motor
* controller PWM values which attempt to speed up or slow down the wheels to match the desired
* speed.
*/
public class Drivetrain {

    // PWM motor controller output definitions
    PWMVictorSPX leftLeader = new PWMVictorSPX(Constants.kDtLeftLeaderPin);
    PWMVictorSPX leftFollower = new PWMVictorSPX(Constants.kDtLeftFollowerPin);
    PWMVictorSPX rightLeader = new PWMVictorSPX(Constants.kDtRightLeaderPin);
    PWMVictorSPX rightFollower = new PWMVictorSPX(Constants.kDtRightFollowerPin);

    SpeedControllerGroup leftGroup = new SpeedControllerGroup(leftLeader, leftFollower);
    SpeedControllerGroup rightGroup = new SpeedControllerGroup(rightLeader, rightFollower);

    // Drivetrain wheel speed sensors
    // Used both for speed control and pose estimation.
    Encoder leftEncoder = new Encoder(Constants.kDtLeftEncoderPinA, Constants.kDtLeftEncoderPinB);
    Encoder rightEncoder = new Encoder(Constants.kDtRightEncoderPinA, Constants.kDtRightEncoderPinB);

    // Drivetrain Pose Estimation
    DrivetrainPoseEstimator poseEst = new DrivetrainPoseEstimator();

    // Kinematics - defines the physical size and shape of the drivetrain, which is
    // required to convert from
    // chassis speed commands to wheel speed commands.
    DifferentialDriveKinematics kinematics = new DifferentialDriveKinematics(Constants.kTrackWidth);

    // Closed-loop PIDF controllers for servoing each side of the drivetrain to a
    // specific speed.
    // Gains are for example purposes only - must be determined for your own robot!
    SimpleMotorFeedforward feedforward = new SimpleMotorFeedforward(1, 3);
    PIDController leftPIDController = new PIDController(8.5, 0, 0);
    PIDController rightPIDController = new PIDController(8.5, 0, 0);

    public Drivetrain() {
        // Set the distance per pulse for the drive encoders. We can simply use the
        // distance traveled for one rotation of the wheel divided by the encoder
        // resolution.
        leftEncoder.setDistancePerPulse(
                2 * Math.PI * Constants.kWheelRadius / Constants.kEncoderResolution);
        rightEncoder.setDistancePerPulse(
                2 * Math.PI * Constants.kWheelRadius / Constants.kEncoderResolution);

        leftEncoder.reset();
        rightEncoder.reset();

        rightGroup.setInverted(true);
    }

    /**
    * Given a set of chassis (fwd/rev + rotate) speed commands, perform all periodic tasks to assign
    * new outputs to the motor controllers.
    *
    * @param xSpeed Desired chassis Forward or Reverse speed (in meters/sec). Positive is forward.
    * @param rot Desired chassis rotation speed in radians/sec. Positive is counter-clockwise.
    */
    public void drive(double xSpeed, double rot) {
        // Convert our fwd/rev and rotate commands to wheel speed commands
        DifferentialDriveWheelSpeeds speeds =
                kinematics.toWheelSpeeds(new ChassisSpeeds(xSpeed, 0, rot));

        // Calculate the feedback (PID) portion of our motor command, based on desired
        // wheel speed
        var leftOutput = leftPIDController.calculate(leftEncoder.getRate(), speeds.leftMetersPerSecond);
        var rightOutput =
                rightPIDController.calculate(rightEncoder.getRate(), speeds.rightMetersPerSecond);

        // Calculate the feedforward (F) portion of our motor command, based on desired
        // wheel speed
        var leftFeedforward = feedforward.calculate(speeds.leftMetersPerSecond);
        var rightFeedforward = feedforward.calculate(speeds.rightMetersPerSecond);

        // Update the motor controllers with our new motor commands
        leftGroup.setVoltage(leftOutput + leftFeedforward);
        rightGroup.setVoltage(rightOutput + rightFeedforward);

        // Update the pose estimator with the most recent sensor readings.
        poseEst.update(
                new DifferentialDriveWheelSpeeds(leftEncoder.getRate(), rightEncoder.getRate()),
                leftEncoder.getDistance(),
                rightEncoder.getDistance());
    }

    /**
    * Force the pose estimator and all sensors to a particular pose. This is useful for indicating to
    * the software when you have manually moved your robot in a particular position on the field (EX:
    * when you place it on the field at the start of the match).
    *
    * @param pose
    */
    public void resetOdometry(Pose2d pose) {
        leftEncoder.reset();
        rightEncoder.reset();
        poseEst.resetToPose(pose);
    }

    /** @return The current best-guess at drivetrain Pose on the field. */
    public Pose2d getCtrlsPoseEstimate() {
        return poseEst.getPoseEst();
    }
}
