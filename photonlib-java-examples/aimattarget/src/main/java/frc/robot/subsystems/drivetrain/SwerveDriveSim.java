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

import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.Discretization;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.RobotController;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class attempts to simulate the dynamics of a swerve drive. In simulationPeriodic, users
 * should first set inputs from motors with {@link #setDriveInputs(double...)} and {@link
 * #setSteerInputs(double...)}, call {@link #update(double)} to update the simulation, and then set
 * swerve module's encoder values and the drivetrain's gyro values with the results from this class.
 *
 * <p>In this class, distances are expressed with meters, angles with radians, and inputs with
 * voltages.
 *
 * <p>Teams can use {@link edu.wpi.first.wpilibj.smartdashboard.Field2d} to visualize their robot on
 * the Sim GUI's field.
 */
public class SwerveDriveSim {
    private final LinearSystem<N2, N1, N2> drivePlant;
    private final double driveKs;
    private final DCMotor driveMotor;
    private final double driveGearing;
    private final double driveWheelRadius;
    private final LinearSystem<N2, N1, N2> steerPlant;
    private final double steerKs;
    private final DCMotor steerMotor;
    private final double steerGearing;

    private final SwerveDriveKinematics kinematics;
    private final int numModules;

    private final double[] driveInputs;
    private final List<Matrix<N2, N1>> driveStates;
    private final double[] steerInputs;
    private final List<Matrix<N2, N1>> steerStates;

    private final Random rand = new Random();

    // noiseless "actual" pose of the robot on the field
    private Pose2d pose = new Pose2d();
    private double omegaRadsPerSec = 0;

    /**
     * Creates a swerve drive simulation.
     *
     * @param driveFF The feedforward for the drive motors of this swerve drive. This should be in
     *     units of meters.
     * @param driveMotor The DCMotor model for the drive motor(s) of this swerve drive's modules. This
     *     should not have any gearing applied.
     * @param driveGearing The gear ratio of the drive system. Positive values indicate a reduction
     *     where one rotation of the drive wheel equals driveGearing rotations of the drive motor.
     * @param driveWheelRadius The radius of the module's driving wheel.
     * @param steerFF The feedforward for the steer motors of this swerve drive. This should be in
     *     units of radians.
     * @param steerMotor The DCMotor model for the steer motor(s) of this swerve drive's modules. This
     *     should not have any gearing applied.
     * @param steerGearing The gear ratio of the steer system. Positive values indicate a reduction
     *     where one rotation of the module heading/azimuth equals steerGearing rotations of the steer
     *     motor.
     * @param kinematics The kinematics for this swerve drive. All swerve module information used in
     *     this class should match the order of the modules this kinematics object was constructed
     *     with.
     */
    public SwerveDriveSim(
            SimpleMotorFeedforward driveFF,
            DCMotor driveMotor,
            double driveGearing,
            double driveWheelRadius,
            SimpleMotorFeedforward steerFF,
            DCMotor steerMotor,
            double steerGearing,
            SwerveDriveKinematics kinematics) {
        this(
                new LinearSystem<N2, N1, N2>(
                        MatBuilder.fill(Nat.N2(), Nat.N2(), 0.0, 1.0, 0.0, -driveFF.getKv() / driveFF.getKa()),
                        VecBuilder.fill(0.0, 1.0 / driveFF.getKa()),
                        MatBuilder.fill(Nat.N2(), Nat.N2(), 1.0, 0.0, 0.0, 1.0),
                        VecBuilder.fill(0.0, 0.0)),
                driveFF.getKs(),
                driveMotor,
                driveGearing,
                driveWheelRadius,
                new LinearSystem<N2, N1, N2>(
                        MatBuilder.fill(Nat.N2(), Nat.N2(), 0.0, 1.0, 0.0, -steerFF.getKv() / steerFF.getKa()),
                        VecBuilder.fill(0.0, 1.0 / steerFF.getKa()),
                        MatBuilder.fill(Nat.N2(), Nat.N2(), 1.0, 0.0, 0.0, 1.0),
                        VecBuilder.fill(0.0, 0.0)),
                steerFF.getKs(),
                steerMotor,
                steerGearing,
                kinematics);
    }

    /**
     * Creates a swerve drive simulation.
     *
     * @param drivePlant The {@link LinearSystem} representing a swerve module's drive system. The
     *     state should be in units of meters and input in volts.
     * @param driveKs The static gain in volts of the drive system's feedforward, or the minimum
     *     voltage to cause motion. Set this to 0 to ignore static friction.
     * @param driveMotor The DCMotor model for the drive motor(s) of this swerve drive's modules. This
     *     should not have any gearing applied.
     * @param driveGearing The gear ratio of the drive system. Positive values indicate a reduction
     *     where one rotation of the drive wheel equals driveGearing rotations of the drive motor.
     * @param driveWheelRadius The radius of the module's driving wheel.
     * @param steerPlant The {@link LinearSystem} representing a swerve module's steer system. The
     *     state should be in units of radians and input in volts.
     * @param steerKs The static gain in volts of the steer system's feedforward, or the minimum
     *     voltage to cause motion. Set this to 0 to ignore static friction.
     * @param steerMotor The DCMotor model for the steer motor(s) of this swerve drive's modules. This
     *     should not have any gearing applied.
     * @param steerGearing The gear ratio of the steer system. Positive values indicate a reduction
     *     where one rotation of the module heading/azimuth equals steerGearing rotations of the steer
     *     motor.
     * @param kinematics The kinematics for this swerve drive. All swerve module information used in
     *     this class should match the order of the modules this kinematics object was constructed
     *     with.
     */
    public SwerveDriveSim(
            LinearSystem<N2, N1, N2> drivePlant,
            double driveKs,
            DCMotor driveMotor,
            double driveGearing,
            double driveWheelRadius,
            LinearSystem<N2, N1, N2> steerPlant,
            double steerKs,
            DCMotor steerMotor,
            double steerGearing,
            SwerveDriveKinematics kinematics) {
        this.drivePlant = drivePlant;
        this.driveKs = driveKs;
        this.driveMotor = driveMotor;
        this.driveGearing = driveGearing;
        this.driveWheelRadius = driveWheelRadius;
        this.steerPlant = steerPlant;
        this.steerKs = steerKs;
        this.steerMotor = steerMotor;
        this.steerGearing = steerGearing;

        this.kinematics = kinematics;
        numModules = kinematics.toSwerveModuleStates(new ChassisSpeeds()).length;
        driveInputs = new double[numModules];
        driveStates = new ArrayList<>(numModules);
        steerInputs = new double[numModules];
        steerStates = new ArrayList<>(numModules);
        for (int i = 0; i < numModules; i++) {
            driveStates.add(VecBuilder.fill(0, 0));
            steerStates.add(VecBuilder.fill(0, 0));
        }
    }

    /**
     * Sets the input voltages of the drive motors.
     *
     * @param inputs Input voltages. These should match the module order used in the kinematics.
     */
    public void setDriveInputs(double... inputs) {
        final double battVoltage = RobotController.getBatteryVoltage();
        for (int i = 0; i < driveInputs.length; i++) {
            double input = inputs[i];
            driveInputs[i] = MathUtil.clamp(input, -battVoltage, battVoltage);
        }
    }

    /**
     * Sets the input voltages of the steer motors.
     *
     * @param inputs Input voltages. These should match the module order used in the kinematics.
     */
    public void setSteerInputs(double... inputs) {
        final double battVoltage = RobotController.getBatteryVoltage();
        for (int i = 0; i < steerInputs.length; i++) {
            double input = inputs[i];
            steerInputs[i] = MathUtil.clamp(input, -battVoltage, battVoltage);
        }
    }

    /**
     * Computes the new x given the old x and the control input. Includes the effect of static
     * friction.
     *
     * @param discA The discretized system matrix.
     * @param discB The discretized input matrix.
     * @param x The position/velocity state of the drive/steer system.
     * @param input The input voltage.
     * @param ks The kS value of the feedforward model.
     * @return The updated x, including the effect of static friction.
     */
    protected static Matrix<N2, N1> calculateX(
            Matrix<N2, N2> discA, Matrix<N2, N1> discB, Matrix<N2, N1> x, double input, double ks) {
        var Ax = discA.times(x);
        double nextStateVel = Ax.get(1, 0);
        // input required to make next state vel == 0
        double inputToStop = nextStateVel / -discB.get(1, 0);
        // ks effect on system velocity
        double ksSystemEffect = MathUtil.clamp(inputToStop, -ks, ks);

        // after ks system effect:
        nextStateVel += discB.get(1, 0) * ksSystemEffect;
        inputToStop = nextStateVel / -discB.get(1, 0);
        double signToStop = Math.signum(inputToStop);
        double inputSign = Math.signum(input);
        double ksInputEffect = 0;

        // system velocity was reduced to 0, resist any input
        if (Math.abs(ksSystemEffect) < ks) {
            double absInput = Math.abs(input);
            ksInputEffect = -MathUtil.clamp(ks * inputSign, -absInput, absInput);
        }
        // non-zero system velocity, but input causes velocity sign change. Resist input after sign
        // change
        else if ((input * signToStop) > (inputToStop * signToStop)) {
            double absInput = Math.abs(input - inputToStop);
            ksInputEffect = -MathUtil.clamp(ks * inputSign, -absInput, absInput);
        }

        // calculate next x including static friction
        var Bu = discB.times(VecBuilder.fill(input + ksSystemEffect + ksInputEffect));
        return Ax.plus(Bu);
    }

    /**
     * Update the drivetrain states with the given timestep.
     *
     * @param dtSeconds The timestep in seconds. This should be the robot loop period.
     */
    public void update(double dtSeconds) {
        var driveDiscAB = Discretization.discretizeAB(drivePlant.getA(), drivePlant.getB(), dtSeconds);
        var steerDiscAB = Discretization.discretizeAB(steerPlant.getA(), steerPlant.getB(), dtSeconds);

        var moduleDeltas = new SwerveModulePosition[numModules];
        for (int i = 0; i < numModules; i++) {
            double prevDriveStatePos = driveStates.get(i).get(0, 0);
            driveStates.set(
                    i,
                    calculateX(
                            driveDiscAB.getFirst(),
                            driveDiscAB.getSecond(),
                            driveStates.get(i),
                            driveInputs[i],
                            driveKs));
            double currDriveStatePos = driveStates.get(i).get(0, 0);
            steerStates.set(
                    i,
                    calculateX(
                            steerDiscAB.getFirst(),
                            steerDiscAB.getSecond(),
                            steerStates.get(i),
                            steerInputs[i],
                            steerKs));
            double currSteerStatePos = steerStates.get(i).get(0, 0);
            moduleDeltas[i] =
                    new SwerveModulePosition(
                            currDriveStatePos - prevDriveStatePos, new Rotation2d(currSteerStatePos));
        }

        var twist = kinematics.toTwist2d(moduleDeltas);
        pose = pose.exp(twist);
        omegaRadsPerSec = twist.dtheta / dtSeconds;
    }

    /**
     * Reset the simulated swerve drive state. This effectively teleports the robot and should only be
     * used during the setup of the simulation world.
     *
     * @param pose The new pose of the simulated swerve drive.
     * @param preserveMotion If true, the current module states will be preserved. Otherwise, they
     *     will be reset to zeros.
     */
    public void reset(Pose2d pose, boolean preserveMotion) {
        this.pose = pose;
        if (!preserveMotion) {
            for (int i = 0; i < numModules; i++) {
                driveStates.set(i, VecBuilder.fill(0, 0));
                steerStates.set(i, VecBuilder.fill(0, 0));
            }
            omegaRadsPerSec = 0;
        }
    }

    /**
     * Reset the simulated swerve drive state. This effectively teleports the robot and should only be
     * used during the setup of the simulation world.
     *
     * @param pose The new pose of the simulated swerve drive.
     * @param moduleDriveStates The new states of the modules' drive systems in [meters, meters/sec].
     *     These should match the module order used in the kinematics.
     * @param moduleSteerStates The new states of the modules' steer systems in [radians,
     *     radians/sec]. These should match the module order used in the kinematics.
     */
    public void reset(
            Pose2d pose, List<Matrix<N2, N1>> moduleDriveStates, List<Matrix<N2, N1>> moduleSteerStates) {
        if (moduleDriveStates.size() != driveStates.size()
                || moduleSteerStates.size() != steerStates.size())
            throw new IllegalArgumentException("Provided module states do not match number of modules!");
        this.pose = pose;
        for (int i = 0; i < numModules; i++) {
            driveStates.set(i, moduleDriveStates.get(i).copy());
            steerStates.set(i, moduleSteerStates.get(i).copy());
        }
        omegaRadsPerSec = kinematics.toChassisSpeeds(getModuleStates()).omegaRadiansPerSecond;
    }

    /**
     * Get the pose of the simulated swerve drive. Note that this is the "actual" pose of the robot in
     * the simulation world, without any noise. If you are simulating a pose estimator, this pose
     * should only be used for visualization or camera simulation. This should be called after {@link
     * #update(double)}.
     */
    public Pose2d getPose() {
        return pose;
    }

    /**
     * Get the {@link SwerveModulePosition} of each module. The returned array order matches the
     * kinematics module order. This should be called after {@link #update(double)}.
     */
    public SwerveModulePosition[] getModulePositions() {
        var positions = new SwerveModulePosition[numModules];
        for (int i = 0; i < numModules; i++) {
            positions[i] =
                    new SwerveModulePosition(
                            driveStates.get(i).get(0, 0), new Rotation2d(steerStates.get(i).get(0, 0)));
        }
        return positions;
    }

    /**
     * Get the {@link SwerveModulePosition} of each module with rudimentary noise simulation. The
     * returned array order matches the kinematics module order. This should be called after {@link
     * #update(double)}.
     *
     * @param driveStdDev The standard deviation in meters of the drive wheel position.
     * @param steerStdDev The standard deviation in radians of the steer angle.
     */
    public SwerveModulePosition[] getNoisyModulePositions(double driveStdDev, double steerStdDev) {
        var positions = new SwerveModulePosition[numModules];
        for (int i = 0; i < numModules; i++) {
            positions[i] =
                    new SwerveModulePosition(
                            driveStates.get(i).get(0, 0) + rand.nextGaussian() * driveStdDev,
                            new Rotation2d(steerStates.get(i).get(0, 0) + rand.nextGaussian() * steerStdDev));
        }
        return positions;
    }

    /**
     * Get the {@link SwerveModuleState} of each module. The returned array order matches the
     * kinematics module order. This should be called after {@link #update(double)}.
     */
    public SwerveModuleState[] getModuleStates() {
        var positions = new SwerveModuleState[numModules];
        for (int i = 0; i < numModules; i++) {
            positions[i] =
                    new SwerveModuleState(
                            driveStates.get(i).get(1, 0), new Rotation2d(steerStates.get(i).get(0, 0)));
        }
        return positions;
    }

    /**
     * Get the state of each module's drive system in [meters, meters/sec]. The returned list order
     * matches the kinematics module order. This should be called after {@link #update(double)}.
     */
    public List<Matrix<N2, N1>> getDriveStates() {
        List<Matrix<N2, N1>> states = new ArrayList<>();
        for (int i = 0; i < driveStates.size(); i++) {
            states.add(driveStates.get(i).copy());
        }
        return states;
    }

    /**
     * Get the state of each module's steer system in [radians, radians/sec]. The returned list order
     * matches the kinematics module order. This should be called after {@link #update(double)}.
     */
    public List<Matrix<N2, N1>> getSteerStates() {
        List<Matrix<N2, N1>> states = new ArrayList<>();
        for (int i = 0; i < steerStates.size(); i++) {
            states.add(steerStates.get(i).copy());
        }
        return states;
    }

    /**
     * Get the angular velocity of the robot, which can be useful for gyro simulation. CCW positive.
     * This should be called after {@link #update(double)}.
     */
    public double getOmegaRadsPerSec() {
        return omegaRadsPerSec;
    }

    /**
     * Calculates the current drawn from the battery by the motor(s). Ignores regenerative current
     * from back-emf.
     *
     * @param motor The motor(s) used.
     * @param radiansPerSec The velocity of the motor in radians per second.
     * @param inputVolts The voltage commanded by the motor controller (battery voltage * duty cycle).
     * @param battVolts The voltage of the battery.
     */
    protected static double getCurrentDraw(
            DCMotor motor, double radiansPerSec, double inputVolts, double battVolts) {
        double effVolts = inputVolts - radiansPerSec / motor.KvRadPerSecPerVolt;
        // ignore regeneration
        if (inputVolts >= 0) effVolts = MathUtil.clamp(effVolts, 0, inputVolts);
        else effVolts = MathUtil.clamp(effVolts, inputVolts, 0);
        // calculate battery current
        return (inputVolts / battVolts) * (effVolts / motor.rOhms);
    }

    /**
     * Get the current draw in amps for each module's drive motor(s). This should be called after
     * {@link #update(double)}. The returned array order matches the kinematics module order.
     */
    public double[] getDriveCurrentDraw() {
        double[] currents = new double[numModules];
        for (int i = 0; i < numModules; i++) {
            double radiansPerSec = driveStates.get(i).get(1, 0) * driveGearing / driveWheelRadius;
            currents[i] =
                    getCurrentDraw(
                            driveMotor, radiansPerSec, driveInputs[i], RobotController.getBatteryVoltage());
        }
        return currents;
    }

    /**
     * Get the current draw in amps for each module's steer motor(s). This should be called after
     * {@link #update(double)}. The returned array order matches the kinematics module order.
     */
    public double[] getSteerCurrentDraw() {
        double[] currents = new double[numModules];
        for (int i = 0; i < numModules; i++) {
            double radiansPerSec = steerStates.get(i).get(1, 0) * steerGearing;
            currents[i] =
                    getCurrentDraw(
                            steerMotor, radiansPerSec, steerInputs[i], RobotController.getBatteryVoltage());
        }
        return currents;
    }

    /**
     * Get the total current draw in amps of all swerve motors. This should be called after {@link
     * #update(double)}.
     */
    public double getTotalCurrentDraw() {
        double sum = 0;
        for (double val : getDriveCurrentDraw()) sum += val;
        for (double val : getSteerCurrentDraw()) sum += val;
        return sum;
    }
}
