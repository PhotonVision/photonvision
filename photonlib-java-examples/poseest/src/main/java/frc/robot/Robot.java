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

package frc.robot;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.simulation.BatterySim;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import frc.robot.subsystems.GamepieceLauncher;
import frc.robot.subsystems.drivetrain.SwerveDrive;

public class Robot extends TimedRobot {
    private SwerveDrive drivetrain;
    private Vision vision;

    private GamepieceLauncher gpLauncher;

    private XboxController controller;

    @Override
    public void robotInit() {
        drivetrain = new SwerveDrive();
        vision = new Vision();

        controller = new XboxController(0);

        gpLauncher = new GamepieceLauncher();
    }

    @Override
    public void robotPeriodic() {
        // Update Gamepiece Launcher Subsystem
        gpLauncher.periodic();

        // Update drivetrain subsystem
        drivetrain.periodic();

        // Correct pose estimate with vision measurements
        var visionEst = vision.getEstimatedGlobalPose();
        visionEst.ifPresent(
                est -> {
                    // Change our trust in the measurement based on the tags we can see
                    var estStdDevs = vision.getEstimationStdDevs();

                    drivetrain.addVisionMeasurement(
                            est.estimatedPose.toPose2d(), est.timestampSeconds, estStdDevs);
                });

        // Test/Example only!
        // Apply an offset to pose estimator to test vision correction
        // You probably don't want this on a real robot, just delete it.
        if (controller.getBButtonPressed()) {
            var disturbance =
                    new Transform2d(new Translation2d(1.0, 1.0), new Rotation2d(0.17 * 2 * Math.PI));
            drivetrain.resetPose(drivetrain.getPose().plus(disturbance), false);
        }

        // Log values to the dashboard
        drivetrain.log();
    }

    @Override
    public void disabledPeriodic() {
        drivetrain.stop();
    }

    @Override
    public void teleopInit() {
        resetPose();
    }

    @Override
    public void teleopPeriodic() {
        // Calculate drivetrain commands from Joystick values
        double forward = -controller.getLeftY() * Constants.Swerve.kMaxLinearSpeed;
        double strafe = -controller.getLeftX() * Constants.Swerve.kMaxLinearSpeed;
        double turn = -controller.getRightX() * Constants.Swerve.kMaxAngularSpeed;

        // Command drivetrain motors based on target speeds
        drivetrain.drive(forward, strafe, turn);

        // Calculate whether the gamepiece launcher runs based on our global pose estimate.
        var curPose = drivetrain.getPose();
        var shouldRun = (curPose.getY() > 2.0 && curPose.getX() < 4.0); // Close enough to blue speaker
        gpLauncher.setRunning(shouldRun);
    }

    @Override
    public void simulationPeriodic() {
        // Update drivetrain simulation
        drivetrain.simulationPeriodic();

        // Update camera simulation
        vision.simulationPeriodic(drivetrain.getSimPose());

        var debugField = vision.getSimDebugField();
        debugField.getObject("EstimatedRobot").setPose(drivetrain.getPose());
        debugField.getObject("EstimatedRobotModules").setPoses(drivetrain.getModulePoses());

        // Update gamepiece launcher simulation
        gpLauncher.simulationPeriodic();

        // Calculate battery voltage sag due to current draw
        var batteryVoltage =
                BatterySim.calculateDefaultBatteryLoadedVoltage(drivetrain.getCurrentDraw());

        // Using max(0.1, voltage) here isn't a *physically correct* solution,
        // but it avoids problems with battery voltage measuring 0.
        RoboRioSim.setVInVoltage(Math.max(0.1, batteryVoltage));
    }

    public void resetPose() {
        // Example Only - startPose should be derived from some assumption
        // of where your robot was placed on the field.
        // The first pose in an autonomous path is often a good choice.
        var startPose = new Pose2d(1, 1, new Rotation2d());
        drivetrain.resetPose(startPose, true);
        vision.resetSimPose(startPose);
    }
}
