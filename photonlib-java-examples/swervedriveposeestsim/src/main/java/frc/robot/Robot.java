// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.simulation.BatterySim;
import edu.wpi.first.wpilibj.simulation.RoboRioSim;
import frc.robot.subsystems.drivetrain.SwerveDrive;
import java.util.Random;

public class Robot extends TimedRobot {
    private SwerveDrive drivetrain;
    private Vision vision;

    private XboxController controller;
    // Limit max speed
    private final double kDriveSpeed = 0.6;
    // Rudimentary limiting of drivetrain acceleration
    private SlewRateLimiter forwardLimiter = new SlewRateLimiter(1.0 / 0.6); // 1 / x seconds to 100%
    private SlewRateLimiter strafeLimiter = new SlewRateLimiter(1.0 / 0.6);
    private SlewRateLimiter turnLimiter = new SlewRateLimiter(1.0 / 0.33);

    private Timer autoTimer = new Timer();
    private Random rand = new Random(4512);

    @Override
    public void robotInit() {
        drivetrain = new SwerveDrive();
        vision = new Vision();

        controller = new XboxController(0);
    }

    @Override
    public void robotPeriodic() {
        drivetrain.periodic();

        // Correct pose estimate with vision measurements
        var visionEst = vision.getEstimatedGlobalPose();
        visionEst.ifPresent(
                est -> {
                    var estPose = est.estimatedPose.toPose2d();
                    // Change our trust in the measurement based on the tags we can see
                    var estStdDevs = vision.getEstimationStdDevs(estPose);

                    drivetrain.addVisionMeasurement(
                            est.estimatedPose.toPose2d(), est.timestampSeconds, estStdDevs);
                });

        // Apply a random offset to pose estimator to test vision correction
        if (controller.getBButtonPressed()) {
            var trf =
                    new Transform2d(
                            new Translation2d(rand.nextDouble() * 4 - 2, rand.nextDouble() * 4 - 2),
                            new Rotation2d(rand.nextDouble() * 2 * Math.PI));
            drivetrain.resetPose(drivetrain.getPose().plus(trf), false);
        }

        // Log values to the dashboard
        drivetrain.log();
    }

    @Override
    public void disabledPeriodic() {
        drivetrain.stop();
    }

    @Override
    public void autonomousInit() {
        autoTimer.restart();
        var pose = new Pose2d(1, 1, new Rotation2d());
        drivetrain.resetPose(pose, true);
        vision.resetSimPose(pose);
    }

    @Override
    public void autonomousPeriodic() {
        // translate diagonally while spinning
        if (autoTimer.get() < 10) {
            drivetrain.drive(0.5, 0.5, 0.5, false);
        } else {
            autoTimer.stop();
            drivetrain.stop();
        }
    }

    @Override
    public void teleopPeriodic() {
        // We will use an "arcade drive" scheme to turn joystick values into target robot speeds
        // We want to get joystick values where pushing forward/left is positive
        double forward = -controller.getLeftY() * kDriveSpeed;
        if (Math.abs(forward) < 0.1) forward = 0; // deadband small values
        forward = forwardLimiter.calculate(forward); // limit acceleration
        double strafe = -controller.getLeftX() * kDriveSpeed;
        if (Math.abs(strafe) < 0.1) strafe = 0;
        strafe = strafeLimiter.calculate(strafe);
        double turn = -controller.getRightX() * kDriveSpeed;
        if (Math.abs(turn) < 0.1) turn = 0;
        turn = turnLimiter.calculate(turn);

        // Convert from joystick values to real target speeds
        forward *= Constants.Swerve.kMaxLinearSpeed;
        strafe *= Constants.Swerve.kMaxLinearSpeed;
        turn *= Constants.Swerve.kMaxLinearSpeed;

        // Command drivetrain motors based on target speeds
        drivetrain.drive(forward, strafe, turn, true);
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

        // Calculate battery voltage sag due to current draw
        RoboRioSim.setVInVoltage(
                BatterySim.calculateDefaultBatteryLoadedVoltage(drivetrain.getCurrentDraw()));
    }
}
