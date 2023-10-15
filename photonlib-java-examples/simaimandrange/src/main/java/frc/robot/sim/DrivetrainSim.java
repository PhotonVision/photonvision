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

package frc.robot.sim;

import static frc.robot.Constants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.motorcontrol.PWMMotorController;
import edu.wpi.first.wpilibj.simulation.DifferentialDrivetrainSim;
import edu.wpi.first.wpilibj.simulation.PWMSim;
import frc.robot.Robot;

/**
 * This class handles the simulation of robot physics, sensors, and motor controllers.
 *
 * <p>This class and its methods are only relevant during simulation. While on the real robot, the
 * real motors/sensors/physics are used instead.
 */
public class DrivetrainSim {
    // ----- Simulation specific constants
    // Drivetrain plant and simulation. This will calculate how the robot pose changes over time as
    // motor voltages are applied.
    private static final LinearSystem<N2, N2, N2> drivetrainSystem =
            LinearSystemId.identifyDrivetrainSystem(
                    LINEAR_KV, LINEAR_KA, ANGULAR_KV, ANGULAR_KA, TRACKWIDTH_METERS);
    private static final DifferentialDrivetrainSim drivetrainSimulator =
            new DifferentialDrivetrainSim(
                    drivetrainSystem,
                    DRIVE_MOTORS,
                    GEARING,
                    TRACKWIDTH_METERS,
                    WHEEL_DIAMETER_METERS / 2.0,
                    null);
    // -----

    // PWM handles for getting commanded motor controller speeds
    private final PWMSim leftLeader;
    private final PWMSim rightLeader;

    public DrivetrainSim(PWMMotorController leftController, PWMMotorController rightController) {
        leftLeader = new PWMSim(leftController);
        rightLeader = new PWMSim(rightController);
    }

    /**
     * Perform all periodic drivetrain simulation related tasks to advance our simulation of robot
     * physics forward by a single 20ms step.
     */
    public void update() {
        double leftMotorCmd = 0;
        double rightMotorCmd = 0;

        if (DriverStation.isEnabled() && !RobotController.isBrownedOut()) {
            leftMotorCmd = leftLeader.getSpeed();
            rightMotorCmd = rightLeader.getSpeed();
        }

        drivetrainSimulator.setInputs(
                leftMotorCmd * RobotController.getBatteryVoltage(),
                -rightMotorCmd * RobotController.getBatteryVoltage());

        drivetrainSimulator.update(Robot.kDefaultPeriod);
    }

    public Pose2d getPose() {
        return drivetrainSimulator.getPose();
    }

    /**
     * Resets the simulation back to a pre-defined pose. Useful to simulate the action of placing the
     * robot onto a specific spot in the field (e.g. at the start of each match).
     *
     * @param pose
     */
    public void resetPose(Pose2d pose) {
        drivetrainSimulator.setPose(pose);
    }
}
