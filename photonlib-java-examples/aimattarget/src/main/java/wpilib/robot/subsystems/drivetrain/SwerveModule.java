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

package wpilib.robot.subsystems.drivetrain;

import static wpilib.robot.Constants.Swerve.*;

import org.wpilib.hardware.motor.PWMMotorController;
import org.wpilib.hardware.rotation.Encoder;
import org.wpilib.math.controller.PIDController;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.kinematics.SwerveModulePosition;
import org.wpilib.math.kinematics.SwerveModuleVelocity;
import org.wpilib.math.util.MathUtil;
import org.wpilib.math.util.Units;
import org.wpilib.simulation.EncoderSim;

public class SwerveModule {
    private static class SimulatedPWMMotorController extends PWMMotorController {
        SimulatedPWMMotorController(int channel) {
            super(channel);
            setBoundsMicroseconds(2500, 2000, 1500, 1000, 500);
        }
    }

    // --- Module Constants
    private final ModuleConstants moduleConstants;

    // --- Hardware
    private final PWMMotorController driveMotor;
    private final Encoder driveEncoder;
    private final PWMMotorController steerMotor;
    private final Encoder steerEncoder;

    // --- Control
    private SwerveModuleVelocity desiredState = new SwerveModuleVelocity();
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

        driveMotor = new SimulatedPWMMotorController(moduleConstants.driveMotorID);
        driveEncoder = new Encoder(moduleConstants.driveEncoderA, moduleConstants.driveEncoderB);
        driveEncoder.setDistancePerPulse(kDriveDistPerPulse);
        steerMotor = new SimulatedPWMMotorController(moduleConstants.steerMotorID);
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
        double driveFF = kDriveFF.calculate(desiredState.velocity);
        double drivePid = 0;
        if (!openLoop) {
            // Perform PID feedback control to compensate for disturbances
            drivePid = drivePidController.calculate(driveEncoder.getRate(), desiredState.velocity);
        }

        driveMotor.setVoltage(driveFF + drivePid);
    }

    /**
     * Command this swerve module to the desired angle and velocity.
     *
     * @param steerInPlace If modules should steer to target angle when target velocity is 0
     */
    public void setDesiredState(
            SwerveModuleVelocity desiredState, boolean openLoop, boolean steerInPlace) {
        Rotation2d currentRotation = getAbsoluteHeading();
        // Avoid turning more than 90 degrees by inverting speed on large angle changes
        this.desiredState = desiredState.optimize(currentRotation);
    }

    /** Module heading reported by steering encoder. */
    public Rotation2d getAbsoluteHeading() {
        return Rotation2d.fromRadians(steerEncoder.getDistance());
    }

    /**
     * {@link SwerveModuleVelocity} describing absolute module rotation and velocity in meters per
     * second.
     */
    public SwerveModuleVelocity getState() {
        return new SwerveModuleVelocity(driveEncoder.getRate(), getAbsoluteHeading());
    }

    /** {@link SwerveModulePosition} describing absolute module rotation and position in meters. */
    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(driveEncoder.getDistance(), getAbsoluteHeading());
    }

    /** Voltage of the drive motor */
    public double getDriveVoltage() {
        return driveMotor.getVoltage();
    }

    /** Voltage of the steer motor */
    public double getSteerVoltage() {
        return steerMotor.getVoltage();
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
        System.out.println(
                table + "Steer Degrees=" + Math.toDegrees(MathUtil.angleModulus(state.angle.getRadians())));
        System.out.println(
                table + "Steer Target Degrees=" + Math.toDegrees(steerPidController.getSetpoint()));
        System.out.println(table + "Drive Velocity Feet=" + Units.metersToFeet(state.velocity));
        System.out.println(
                table + "Drive Velocity Target Feet=" + Units.metersToFeet(desiredState.velocity));
        System.out.println(table + "Drive Current=" + driveCurrentSim);
        System.out.println(table + "Steer Current=" + steerCurrentSim);
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
