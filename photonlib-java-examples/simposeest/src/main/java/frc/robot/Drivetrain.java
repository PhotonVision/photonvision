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
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.motorcontrol.PWMVictorSPX;

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

    MotorControllerGroup leftGroup = new MotorControllerGroup(leftLeader, leftFollower);
    MotorControllerGroup rightGroup = new MotorControllerGroup(rightLeader, rightFollower);

    // Drivetrain wheel speed sensors
    // Used both for speed control and pose estimation.
    Encoder leftEncoder = new Encoder(Constants.kDtLeftEncoderPinA, Constants.kDtLeftEncoderPinB);
    Encoder rightEncoder = new Encoder(Constants.kDtRightEncoderPinA, Constants.kDtRightEncoderPinB);

    // Drivetrain Pose Estimation
    final DrivetrainPoseEstimator poseEst;

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

        poseEst = new DrivetrainPoseEstimator(leftEncoder.getDistance(), rightEncoder.getDistance());
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
        poseEst.update(leftEncoder.getDistance(), rightEncoder.getDistance());
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
        poseEst.resetToPose(pose, leftEncoder.getDistance(), rightEncoder.getDistance());
    }

    /** @return The current best-guess at drivetrain Pose on the field. */
    public Pose2d getCtrlsPoseEstimate() {
        return poseEst.getPoseEst();
    }
}
