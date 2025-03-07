/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

package frc.robot.subsystems.drivetrain;

import static frc.robot.Constants.Swerve.*;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.SPI.Port;
import edu.wpi.first.wpilibj.simulation.ADXRS450_GyroSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Robot;

public class SwerveDrive {
    // Construct the swerve modules with their respective constants.
    // The SwerveModule class will handle all the details of controlling the modules.
    private final SwerveModule[] swerveMods = {
        new SwerveModule(ModuleConstants.FL),
        new SwerveModule(ModuleConstants.FR),
        new SwerveModule(ModuleConstants.BL),
        new SwerveModule(ModuleConstants.BR)
    };

    // The kinematics for translating between robot chassis speeds and module states.
    private final SwerveDriveKinematics kinematics =
            new SwerveDriveKinematics(
                    swerveMods[0].getModuleConstants().centerOffset,
                    swerveMods[1].getModuleConstants().centerOffset,
                    swerveMods[2].getModuleConstants().centerOffset,
                    swerveMods[3].getModuleConstants().centerOffset);

    private final ADXRS450_Gyro gyro = new ADXRS450_Gyro(Port.kOnboardCS0);

    // The robot pose estimator for tracking swerve odometry and applying vision corrections.
    private final SwerveDrivePoseEstimator poseEstimator;

    private ChassisSpeeds targetChassisSpeeds = new ChassisSpeeds();

    // ----- Simulation
    private final ADXRS450_GyroSim gyroSim;
    private final SwerveDriveSim swerveDriveSim;
    private double totalCurrentDraw = 0;

    public SwerveDrive() {
        // Define the standard deviations for the pose estimator, which determine how fast the pose
        // estimate converges to the vision measurement. This should depend on the vision measurement
        // noise
        // and how many or how frequently vision measurements are applied to the pose estimator.
        var stateStdDevs = VecBuilder.fill(0.1, 0.1, 0.1);
        var visionStdDevs = VecBuilder.fill(1, 1, 1);
        poseEstimator =
                new SwerveDrivePoseEstimator(
                        kinematics,
                        getGyroYaw(),
                        getModulePositions(),
                        new Pose2d(),
                        stateStdDevs,
                        visionStdDevs);

        // ----- Simulation
        gyroSim = new ADXRS450_GyroSim(gyro);
        swerveDriveSim =
                new SwerveDriveSim(
                        kDriveFF,
                        DCMotor.getFalcon500(1),
                        kDriveGearRatio,
                        kWheelDiameter / 2.0,
                        kSteerFF,
                        DCMotor.getFalcon500(1),
                        kSteerGearRatio,
                        kinematics);
    }

    public void periodic() {
        for (SwerveModule module : swerveMods) {
            module.periodic();
        }

        // Update the odometry of the swerve drive using the wheel encoders and gyro.
        poseEstimator.update(getGyroYaw(), getModulePositions());
    }

    /**
     * Basic drive control. A target field-relative ChassisSpeeds (vx, vy, omega) is converted to
     * specific swerve module states.
     *
     * @param vxMeters X velocity (forwards/backwards)
     * @param vyMeters Y velocity (strafe left/right)
     * @param omegaRadians Angular velocity (rotation CCW+)
     */
    public void drive(double vxMeters, double vyMeters, double omegaRadians) {
        var targetChassisSpeeds =
                ChassisSpeeds.fromFieldRelativeSpeeds(vxMeters, vyMeters, omegaRadians, getHeading());
        setChassisSpeeds(targetChassisSpeeds, true, false);
    }

    /**
     * Command the swerve drive to the desired chassis speeds by converting them to swerve module
     * states and using {@link #setModuleStates(SwerveModuleState[], boolean)}.
     *
     * @param targetChassisSpeeds Target robot-relative chassis speeds (vx, vy, omega).
     * @param openLoop If swerve modules should use feedforward only and ignore velocity feedback
     *     control.
     * @param steerInPlace If modules should steer to the target angle when target velocity is 0.
     */
    public void setChassisSpeeds(
            ChassisSpeeds targetChassisSpeeds, boolean openLoop, boolean steerInPlace) {
        setModuleStates(kinematics.toSwerveModuleStates(targetChassisSpeeds), openLoop, steerInPlace);
        this.targetChassisSpeeds = targetChassisSpeeds;
    }

    /**
     * Command the swerve modules to the desired states. Velocities exceeding the maximum speed will
     * be desaturated (while preserving the ratios between modules).
     *
     * @param openLoop If swerve modules should use feedforward only and ignore velocity feedback
     *     control.
     * @param steerInPlace If modules should steer to the target angle when target velocity is 0.
     */
    public void setModuleStates(
            SwerveModuleState[] desiredStates, boolean openLoop, boolean steerInPlace) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, kMaxLinearSpeed);
        for (int i = 0; i < swerveMods.length; i++) {
            swerveMods[i].setDesiredState(desiredStates[i], openLoop, steerInPlace);
        }
    }

    /** Stop the swerve drive. */
    public void stop() {
        drive(0, 0, 0);
    }

    /** See {@link SwerveDrivePoseEstimator#addVisionMeasurement(Pose2d, double)}. */
    public void addVisionMeasurement(Pose2d visionMeasurement, double timestampSeconds) {
        poseEstimator.addVisionMeasurement(visionMeasurement, timestampSeconds);
    }

    /** See {@link SwerveDrivePoseEstimator#addVisionMeasurement(Pose2d, double, Matrix)}. */
    public void addVisionMeasurement(
            Pose2d visionMeasurement, double timestampSeconds, Matrix<N3, N1> stdDevs) {
        poseEstimator.addVisionMeasurement(visionMeasurement, timestampSeconds, stdDevs);
    }

    /**
     * Reset the estimated pose of the swerve drive on the field.
     *
     * @param pose New robot pose.
     * @param resetSimPose If the simulated robot pose should also be reset. This effectively
     *     teleports the robot and should only be used during the setup of the simulation world.
     */
    public void resetPose(Pose2d pose, boolean resetSimPose) {
        if (resetSimPose) {
            swerveDriveSim.reset(pose, false);
            // we shouldnt realistically be resetting pose after startup, but we will handle it anyway for
            // testing
            for (int i = 0; i < swerveMods.length; i++) {
                swerveMods[i].simulationUpdate(0, 0, 0, 0, 0, 0);
            }
            gyroSim.setAngle(-pose.getRotation().getDegrees());
            gyroSim.setRate(0);
        }

        poseEstimator.resetPosition(getGyroYaw(), getModulePositions(), pose);
    }

    /** Get the estimated pose of the swerve drive on the field. */
    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    /** The heading of the swerve drive's estimated pose on the field. */
    public Rotation2d getHeading() {
        return getPose().getRotation();
    }

    /** Raw gyro yaw (this may not match the field heading!). */
    public Rotation2d getGyroYaw() {
        return gyro.getRotation2d();
    }

    /** Get the chassis speeds of the robot (vx, vy, omega) from the swerve module states. */
    public ChassisSpeeds getChassisSpeeds() {
        return kinematics.toChassisSpeeds(getModuleStates());
    }

    /**
     * Get the SwerveModuleState of each swerve module (speed, angle). The returned array order
     * matches the kinematics module order.
     */
    public SwerveModuleState[] getModuleStates() {
        return new SwerveModuleState[] {
            swerveMods[0].getState(),
            swerveMods[1].getState(),
            swerveMods[2].getState(),
            swerveMods[3].getState()
        };
    }

    /**
     * Get the SwerveModulePosition of each swerve module (position, angle). The returned array order
     * matches the kinematics module order.
     */
    public SwerveModulePosition[] getModulePositions() {
        return new SwerveModulePosition[] {
            swerveMods[0].getPosition(),
            swerveMods[1].getPosition(),
            swerveMods[2].getPosition(),
            swerveMods[3].getPosition()
        };
    }

    /**
     * Get the Pose2d of each swerve module based on kinematics and current robot pose. The returned
     * array order matches the kinematics module order.
     */
    public Pose2d[] getModulePoses() {
        Pose2d[] modulePoses = new Pose2d[swerveMods.length];
        for (int i = 0; i < swerveMods.length; i++) {
            var module = swerveMods[i];
            modulePoses[i] =
                    getPose()
                            .transformBy(
                                    new Transform2d(
                                            module.getModuleConstants().centerOffset, module.getAbsoluteHeading()));
        }
        return modulePoses;
    }

    /** Log various drivetrain values to the dashboard. */
    public void log() {
        String table = "Drive/";
        Pose2d pose = getPose();
        SmartDashboard.putNumber(table + "X", pose.getX());
        SmartDashboard.putNumber(table + "Y", pose.getY());
        SmartDashboard.putNumber(table + "Heading", pose.getRotation().getDegrees());
        ChassisSpeeds chassisSpeeds = getChassisSpeeds();
        SmartDashboard.putNumber(table + "VX", chassisSpeeds.vxMetersPerSecond);
        SmartDashboard.putNumber(table + "VY", chassisSpeeds.vyMetersPerSecond);
        SmartDashboard.putNumber(
                table + "Omega Degrees", Math.toDegrees(chassisSpeeds.omegaRadiansPerSecond));
        SmartDashboard.putNumber(table + "Target VX", targetChassisSpeeds.vxMetersPerSecond);
        SmartDashboard.putNumber(table + "Target VY", targetChassisSpeeds.vyMetersPerSecond);
        SmartDashboard.putNumber(
                table + "Target Omega Degrees", Math.toDegrees(targetChassisSpeeds.omegaRadiansPerSecond));

        for (SwerveModule module : swerveMods) {
            module.log();
        }
    }

    // ----- Simulation

    public void simulationPeriodic() {
        // Pass commanded motor voltages into swerve drive simulation
        double[] driveInputs = new double[swerveMods.length];
        double[] steerInputs = new double[swerveMods.length];
        for (int i = 0; i < swerveMods.length; i++) {
            driveInputs[i] = swerveMods[i].getDriveVoltage();
            steerInputs[i] = swerveMods[i].getSteerVoltage();
        }
        swerveDriveSim.setDriveInputs(driveInputs);
        swerveDriveSim.setSteerInputs(steerInputs);

        // Simulate one timestep
        swerveDriveSim.update(Robot.kDefaultPeriod);

        // Update module and gyro values with simulated values
        var driveStates = swerveDriveSim.getDriveStates();
        var steerStates = swerveDriveSim.getSteerStates();
        totalCurrentDraw = 0;
        double[] driveCurrents = swerveDriveSim.getDriveCurrentDraw();
        for (double current : driveCurrents) totalCurrentDraw += current;
        double[] steerCurrents = swerveDriveSim.getSteerCurrentDraw();
        for (double current : steerCurrents) totalCurrentDraw += current;
        for (int i = 0; i < swerveMods.length; i++) {
            double drivePos = driveStates.get(i).get(0, 0);
            double driveRate = driveStates.get(i).get(1, 0);
            double steerPos = steerStates.get(i).get(0, 0);
            double steerRate = steerStates.get(i).get(1, 0);
            swerveMods[i].simulationUpdate(
                    drivePos, driveRate, driveCurrents[i], steerPos, steerRate, steerCurrents[i]);
        }

        gyroSim.setRate(-swerveDriveSim.getOmegaRadsPerSec());
        gyroSim.setAngle(-swerveDriveSim.getPose().getRotation().getDegrees());
    }

    /**
     * The "actual" pose of the robot on the field used in simulation. This is different from the
     * swerve drive's estimated pose.
     */
    public Pose2d getSimPose() {
        return swerveDriveSim.getPose();
    }

    public double getCurrentDraw() {
        return totalCurrentDraw;
    }
}
