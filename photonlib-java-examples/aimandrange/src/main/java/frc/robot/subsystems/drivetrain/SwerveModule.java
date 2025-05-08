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

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.simulation.EncoderSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class SwerveModule {
    // --- Module Constants
    private final ModuleConstants moduleConstants;

    // --- Hardware
    private final PWMSparkMax driveMotor;
    private final Encoder driveEncoder;
    private final PWMSparkMax steerMotor;
    private final Encoder steerEncoder;

    // --- Control
    private SwerveModuleState desiredState = new SwerveModuleState();
    private boolean openLoop = false;

    // Simple PID feedback controllers run on the roborio
    private PIDController drivePidController = new PIDController(kDriveKP, kDriveKI, kDriveKD);
    // (A profiled steering PID controller may give better results by utilizing feedforward.)
    private PIDController steerPidController = new PIDController(kSteerKP, kSteerKI, kSteerKD);

    // --- Simulation
    private final EncoderSim driveEncoderSim;
    private double driveCurrentSim = 0;
    private final EncoderSim steerEncoderSim;
    private double steerCurrentSim = 0;

    public SwerveModule(ModuleConstants moduleConstants) {
        this.moduleConstants = moduleConstants;

        driveMotor = new PWMSparkMax(moduleConstants.driveMotorID);
        driveEncoder = new Encoder(moduleConstants.driveEncoderA, moduleConstants.driveEncoderB);
        driveEncoder.setDistancePerPulse(kDriveDistPerPulse);
        steerMotor = new PWMSparkMax(moduleConstants.steerMotorID);
        steerEncoder = new Encoder(moduleConstants.steerEncoderA, moduleConstants.steerEncoderB);
        steerEncoder.setDistancePerPulse(kSteerRadPerPulse);

        steerPidController.enableContinuousInput(-Math.PI, Math.PI);

        // --- Simulation
        driveEncoderSim = new EncoderSim(driveEncoder);
        steerEncoderSim = new EncoderSim(steerEncoder);
    }

    public void periodic() {
        // Perform PID feedback control to steer the module to the target angle
        double steerPid =
                steerPidController.calculate(
                        getAbsoluteHeading().getRadians(), desiredState.angle.getRadians());
        steerMotor.setVoltage(steerPid);

        // Use feedforward model to translate target drive velocities into voltages
        double driveFF = kDriveFF.calculate(desiredState.speedMetersPerSecond);
        double drivePid = 0;
        if (!openLoop) {
            // Perform PID feedback control to compensate for disturbances
            drivePid =
                    drivePidController.calculate(driveEncoder.getRate(), desiredState.speedMetersPerSecond);
        }

        driveMotor.setVoltage(driveFF + drivePid);
    }

    /**
     * Command this swerve module to the desired angle and velocity.
     *
     * @param steerInPlace If modules should steer to target angle when target velocity is 0
     */
    public void setDesiredState(
            SwerveModuleState desiredState, boolean openLoop, boolean steerInPlace) {
        Rotation2d currentRotation = getAbsoluteHeading();
        // Avoid turning more than 90 degrees by inverting speed on large angle changes
        desiredState.optimize(currentRotation);

        this.desiredState = desiredState;
    }

    /** Module heading reported by steering encoder. */
    public Rotation2d getAbsoluteHeading() {
        return Rotation2d.fromRadians(steerEncoder.getDistance());
    }

    /**
     * {@link SwerveModuleState} describing absolute module rotation and velocity in meters per
     * second.
     */
    public SwerveModuleState getState() {
        return new SwerveModuleState(driveEncoder.getRate(), getAbsoluteHeading());
    }

    /** {@link SwerveModulePosition} describing absolute module rotation and position in meters. */
    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(driveEncoder.getDistance(), getAbsoluteHeading());
    }

    /** Voltage of the drive motor */
    public double getDriveVoltage() {
        return driveMotor.get() * RobotController.getBatteryVoltage();
    }

    /** Voltage of the steer motor */
    public double getSteerVoltage() {
        return steerMotor.get() * RobotController.getBatteryVoltage();
    }

    /**
     * Constants about this module, like motor IDs, encoder angle offset, and translation from center.
     */
    public ModuleConstants getModuleConstants() {
        return moduleConstants;
    }

    public void log() {
        var state = getState();

        String table = "Module " + moduleConstants.moduleNum + "/";
        SmartDashboard.putNumber(
                table + "Steer Degrees", Math.toDegrees(MathUtil.angleModulus(state.angle.getRadians())));
        SmartDashboard.putNumber(
                table + "Steer Target Degrees", Math.toDegrees(steerPidController.getSetpoint()));
        SmartDashboard.putNumber(
                table + "Drive Velocity Feet", Units.metersToFeet(state.speedMetersPerSecond));
        SmartDashboard.putNumber(
                table + "Drive Velocity Target Feet",
                Units.metersToFeet(desiredState.speedMetersPerSecond));
        SmartDashboard.putNumber(table + "Drive Current", driveCurrentSim);
        SmartDashboard.putNumber(table + "Steer Current", steerCurrentSim);
    }

    // ----- Simulation

    public void simulationUpdate(
            double driveEncoderDist,
            double driveEncoderRate,
            double driveCurrent,
            double steerEncoderDist,
            double steerEncoderRate,
            double steerCurrent) {
        driveEncoderSim.setDistance(driveEncoderDist);
        driveEncoderSim.setRate(driveEncoderRate);
        this.driveCurrentSim = driveCurrent;
        steerEncoderSim.setDistance(steerEncoderDist);
        steerEncoderSim.setRate(steerEncoderRate);
        this.steerCurrentSim = steerCurrent;
    }

    public double getDriveCurrentSim() {
        return driveCurrentSim;
    }

    public double getSteerCurrentSim() {
        return steerCurrentSim;
    }
}
