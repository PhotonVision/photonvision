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

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.simulation.DifferentialDrivetrainSim;
import edu.wpi.first.wpilibj.simulation.PWMSim;
import frc.robot.Robot;

import java.util.List;

import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionSystemSim;
import org.photonvision.simulation.VisionTargetSim;

/**
 * Implementation of a simulation of robot physics, sensors, motor controllers Includes a Simulated
 * PhotonVision system and one vision target.
 *
 * <p>This class and its methods are only relevant during simulation. While on the real robot, the
 * real motors/sensors/physics are used instead.
 */
public class DrivetrainSim {
    // Simulated Motor Controllers
    PWMSim leftLeader;
    PWMSim rightLeader;

    // Simulation Physics
    // Configure these to match your drivetrain's physical dimensions
    // and characterization results.
    double trackwidthMeters = Units.feetToMeters(2.0);
    LinearSystem<N2, N2, N2> drivetrainSystem =
            LinearSystemId.identifyDrivetrainSystem(2.0, 0.5, 2.25, 0.3, trackwidthMeters);
    DifferentialDrivetrainSim drivetrainSimulator =
            new DifferentialDrivetrainSim(
                    drivetrainSystem,
                    DCMotor.getCIM(2),
                    8,
                    trackwidthMeters,
                    Units.inchesToMeters(6.0 / 2.0),
                    null);

    // Simulated Vision System.
    // Configure these to match your PhotonVision Camera,
    // pipeline, and LED setup.
    double camDiagFOV = 100.0; // degrees
    double camPitch = Robot.CAMERA_PITCH_RADIANS; // degrees
    double camHeightOffGround = Robot.CAMERA_HEIGHT_METERS; // meters
    double minTargetArea = 0.1; // percentage (0 - 100)
    double maxLEDRange = 20; // meters
    int camResolutionWidth = 640; // pixels
    int camResolutionHeight = 480; // pixels
    PhotonCameraSim cameraSim;

    VisionSystemSim visionSim = new VisionSystemSim("main");

    // See
    // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/2020FieldDrawing-SeasonSpecific.pdf
    // page 208
    TargetModel targetModel = new TargetModel(List.of(
        new Translation3d(0, Units.inchesToMeters(-9.819867), Units.inchesToMeters(-8.5)),
        new Translation3d(0, Units.inchesToMeters(9.819867), Units.inchesToMeters(-8.5)),
        new Translation3d(0, Units.inchesToMeters(19.625), Units.inchesToMeters(8.5)),
        new Translation3d(0, Units.inchesToMeters(-19.625), Units.inchesToMeters(8.5))
    ));
    // See https://firstfrc.blob.core.windows.net/frc2020/PlayingField/LayoutandMarkingDiagram.pdf
    // pages 4 and 5
    double tgtXPos = Units.feetToMeters(54);
    double tgtYPos =
            Units.feetToMeters(27 / 2) - Units.inchesToMeters(43.75) - Units.inchesToMeters(48.0 / 2.0);
    Pose3d farTargetPose =
            new Pose3d(
                    new Translation3d(tgtXPos, tgtYPos, Robot.TARGET_HEIGHT_METERS),
                    new Rotation3d(0.0, 0.0, Math.PI));

    public DrivetrainSim(int leftMotorPort, int rightMotorPort, PhotonCamera camera) {
        leftLeader = new PWMSim(leftMotorPort);
        rightLeader = new PWMSim(rightMotorPort);

        // Make the vision target visible to this simulated field.
        var visionTarget = new VisionTargetSim(farTargetPose, targetModel);
        visionSim.addVisionTargets(visionTarget);

        // Create simulated camera properties. These can be set to mimic your actual camera.
        var cameraProp = new SimCameraProperties();
        cameraProp.setCalibration(camResolutionWidth, camResolutionHeight, Rotation2d.fromDegrees(camDiagFOV));
        cameraProp.setCalibError(0.2, 0.05);
        cameraProp.setFPS(25);
        cameraProp.setAvgLatencyMs(30);
        cameraProp.setLatencyStdDevMs(4);
        // Create a PhotonCameraSim which will update the linked PhotonCamera's values with visible targets.
        cameraSim = new PhotonCameraSim(camera, cameraProp, minTargetArea, maxLEDRange);

        // Add the simulated camera to view the targets on this simulated field.
        visionSim.addCamera(cameraSim, new Transform3d(new Translation3d(0.25, 0, Robot.CAMERA_HEIGHT_METERS), new Rotation3d(0, -Robot.CAMERA_PITCH_RADIANS, 0)));
    
        cameraSim.enableDrawWireframe(true);
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
        drivetrainSimulator.update(0.02);

        // Update PhotonVision based on our new robot position.
        visionSim.update(drivetrainSimulator.getPose());
    }

    /**
     * Resets the simulation back to a pre-defined pose Useful to simulate the action of placing the
     * robot onto a specific spot in the field (IE, at the start of each match).
     *
     * @param pose
     */
    public void resetPose(Pose2d pose) {
        drivetrainSimulator.setPose(pose);
        visionSim.resetRobotPose(pose);
    }
}
