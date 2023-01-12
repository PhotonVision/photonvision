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
import edu.wpi.first.math.estimator.DifferentialDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.simulation.AnalogGyroSim;
import edu.wpi.first.wpilibj.simulation.DifferentialDrivetrainSim;
import edu.wpi.first.wpilibj.simulation.EncoderSim;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.DriveTrainConstants;
import java.util.Optional;
import org.photonvision.EstimatedRobotPose;

/** Represents a differential drive style drivetrain. */
public class Drivetrain {
    private final MotorController m_leftLeader = new PWMSparkMax(1);
    private final MotorController m_leftFollower = new PWMSparkMax(2);
    private final MotorController m_rightLeader = new PWMSparkMax(3);
    private final MotorController m_rightFollower = new PWMSparkMax(4);

    private final Encoder m_leftEncoder = new Encoder(0, 1);
    private final Encoder m_rightEncoder = new Encoder(2, 3);

    private final MotorControllerGroup m_leftGroup =
            new MotorControllerGroup(m_leftLeader, m_leftFollower);
    private final MotorControllerGroup m_rightGroup =
            new MotorControllerGroup(m_rightLeader, m_rightFollower);

    private final AnalogGyro m_gyro = new AnalogGyro(0);

    private final PIDController m_leftPIDController = new PIDController(1, 0, 0);
    private final PIDController m_rightPIDController = new PIDController(1, 0, 0);

    private final DifferentialDriveKinematics m_kinematics =
            new DifferentialDriveKinematics(Constants.DriveTrainConstants.kTrackWidth);

    public PhotonCameraWrapper pcw;

    /*
     * Here we use DifferentialDrivePoseEstimator so that we can fuse odometry
     * readings. The
     * numbers used below are robot specific, and should be tuned.
     */
    private final DifferentialDrivePoseEstimator m_poseEstimator =
            new DifferentialDrivePoseEstimator(
                    m_kinematics, m_gyro.getRotation2d(), 0.0, 0.0, new Pose2d());

    // Gains are for example purposes only - must be determined for your own robot!
    private final SimpleMotorFeedforward m_feedforward = new SimpleMotorFeedforward(1, 3);

    // Simulation classes help us simulate our robot
    private final AnalogGyroSim m_gyroSim = new AnalogGyroSim(m_gyro);
    private final EncoderSim m_leftEncoderSim = new EncoderSim(m_leftEncoder);
    private final EncoderSim m_rightEncoderSim = new EncoderSim(m_rightEncoder);
    private final Field2d m_fieldSim = new Field2d();
    private final LinearSystem<N2, N2, N2> m_drivetrainSystem =
            LinearSystemId.identifyDrivetrainSystem(1.98, 0.2, 1.5, 0.3);
    private final DifferentialDrivetrainSim m_drivetrainSimulator =
            new DifferentialDrivetrainSim(
                    m_drivetrainSystem,
                    DCMotor.getCIM(2),
                    8,
                    DriveTrainConstants.kTrackWidth,
                    DriveTrainConstants.kWheelRadius,
                    null);

    /**
     * Constructs a differential drive object. Sets the encoder distance per pulse and resets the
     * gyro.
     */
    public Drivetrain() {
        pcw = new PhotonCameraWrapper();

        m_gyro.reset();

        // We need to invert one side of the drivetrain so that positive voltages
        // result in both sides moving forward. Depending on how your robot's
        // gearbox is constructed, you might have to invert the left side instead.
        m_rightGroup.setInverted(true);

        // Set the distance per pulse for the drive encoders. We can simply use the
        // distance traveled for one rotation of the wheel divided by the encoder
        // resolution.
        m_leftEncoder.setDistancePerPulse(DriveTrainConstants.distancePerPulse);
        m_rightEncoder.setDistancePerPulse(DriveTrainConstants.distancePerPulse);

        m_leftEncoder.reset();
        m_rightEncoder.reset();

        SmartDashboard.putData("Field", m_fieldSim);
    }

    /**
     * Sets the desired wheel speeds.
     *
     * @param speeds The desired wheel speeds.
     */
    public void setSpeeds(DifferentialDriveWheelSpeeds speeds) {
        final double leftFeedforward = m_feedforward.calculate(speeds.leftMetersPerSecond);
        final double rightFeedforward = m_feedforward.calculate(speeds.rightMetersPerSecond);

        final double leftOutput =
                m_leftPIDController.calculate(m_leftEncoder.getRate(), speeds.leftMetersPerSecond);
        final double rightOutput =
                m_rightPIDController.calculate(m_rightEncoder.getRate(), speeds.rightMetersPerSecond);
        m_leftGroup.setVoltage(leftOutput + leftFeedforward);
        m_rightGroup.setVoltage(rightOutput + rightFeedforward);
    }

    /**
     * Drives the robot with the given linear velocity and angular velocity.
     *
     * @param xSpeed Linear velocity in m/s.
     * @param rot Angular velocity in rad/s.
     */
    public void drive(double xSpeed, double rot) {
        var wheelSpeeds = m_kinematics.toWheelSpeeds(new ChassisSpeeds(xSpeed, 0.0, rot));
        setSpeeds(wheelSpeeds);
    }

    /** Update our simulation. This should be run every robot loop in simulation. */
    public void simulationPeriodic() {
        // To update our simulation, we set motor voltage inputs, update the
        // simulation, and write the simulated positions and velocities to our
        // simulated encoder and gyro. We negate the right side so that positive
        // voltages make the right side move forward.
        m_drivetrainSimulator.setInputs(
                m_leftGroup.get() * RobotController.getInputVoltage(),
                m_rightGroup.get() * RobotController.getInputVoltage());
        m_drivetrainSimulator.update(0.02);

        m_leftEncoderSim.setDistance(m_drivetrainSimulator.getLeftPositionMeters());
        m_leftEncoderSim.setRate(m_drivetrainSimulator.getLeftVelocityMetersPerSecond());
        m_rightEncoderSim.setDistance(m_drivetrainSimulator.getRightPositionMeters());
        m_rightEncoderSim.setRate(m_drivetrainSimulator.getRightVelocityMetersPerSecond());
        m_gyroSim.setAngle(-m_drivetrainSimulator.getHeading().getDegrees());
    }

    /** Updates the field-relative position. */
    public void updateOdometry() {
        m_poseEstimator.update(
                m_gyro.getRotation2d(), m_leftEncoder.getDistance(), m_rightEncoder.getDistance());

        // Also apply vision measurements. We use 0.3 seconds in the past as an example
        // -- on
        // a real robot, this must be calculated based either on latency or timestamps.
        Optional<EstimatedRobotPose> result =
                pcw.getEstimatedGlobalPose(m_poseEstimator.getEstimatedPosition());

        if (result.isPresent()) {
            EstimatedRobotPose camPose = result.get();
            m_poseEstimator.addVisionMeasurement(
                    camPose.estimatedPose.toPose2d(), camPose.timestampSeconds);
            m_fieldSim.getObject("Cam Est Pos").setPose(camPose.estimatedPose.toPose2d());
        } else {
            // move it way off the screen to make it disappear
            m_fieldSim.getObject("Cam Est Pos").setPose(new Pose2d(-100, -100, new Rotation2d()));
        }

        m_fieldSim.getObject("Actual Pos").setPose(m_drivetrainSimulator.getPose());
        m_fieldSim.setRobotPose(m_poseEstimator.getEstimatedPosition());
    }
}
