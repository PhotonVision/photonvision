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
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.util.List;
import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionSystemSim;
import org.photonvision.simulation.VisionTargetSim;

/**
 * This class handles the simulation of the camera and vision target on the field. Updating this
 * class will update the camera data, reflecting what the simulated camera sees.
 *
 * <p>This class and its methods are only relevant during simulation. While on the real robot, the
 * real camera data is used instead.
 */
public class VisionSim {
  // ----- Simulation specific constants
  // 2020 High goal target shape
  // See
  // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/2020FieldDrawing-SeasonSpecific.pdf
  // page 208
  private static final TargetModel kTargetModel =
      new TargetModel(
          List.of(
              new Translation3d(0, Units.inchesToMeters(-9.819867), Units.inchesToMeters(-8.5)),
              new Translation3d(0, Units.inchesToMeters(9.819867), Units.inchesToMeters(-8.5)),
              new Translation3d(0, Units.inchesToMeters(19.625), Units.inchesToMeters(8.5)),
              new Translation3d(0, Units.inchesToMeters(-19.625), Units.inchesToMeters(8.5))));

  // Simulated camera properties. These can be set to mimic your actual camera.
  private static final int kResolutionWidth = 640; // pixels
  private static final int kResolutionHeight = 480; // pixels
  private static final Rotation2d kFOVDiag = Rotation2d.fromDegrees(100.0); // degrees
  private static final double kAvgErrorPx = 0.2;
  private static final double kErrorStdDevPx = 0.05;
  private static final double kFPS = 25;
  private static final double kAvgLatencyMs = 30;
  private static final double kLatencyStdDevMs = 4;
  private static final double kMinTargetArea = 0.1; // percentage (0 - 100)
  private static final double kMaxLEDRange = 15; // meters
  // -----

  // A simulated vision system which handles simulated cameras and targets.
  private VisionSystemSim visionSim;
  // The simulation of our PhotonCamera, which will simulate camera frames and target info.
  private PhotonCameraSim cameraSim;

  public VisionSim(String name, PhotonCamera camera) {
    visionSim = new VisionSystemSim(name);
    // Make the vision target visible to this simulated field.
    var visionTarget = new VisionTargetSim(TARGET_POSE, kTargetModel);
    visionSim.addVisionTargets(visionTarget);

    // Create simulated camera properties from our defined constants.
    var cameraProp = new SimCameraProperties();
    cameraProp.setCalibration(kResolutionWidth, kResolutionHeight, kFOVDiag);
    cameraProp.setCalibError(kAvgErrorPx, kErrorStdDevPx);
    cameraProp.setFPS(kFPS);
    cameraProp.setAvgLatencyMs(kAvgLatencyMs);
    cameraProp.setLatencyStdDevMs(kLatencyStdDevMs);
    // Create a PhotonCameraSim which will update the linked PhotonCamera's values with visible
    // targets.
    cameraSim = new PhotonCameraSim(camera, cameraProp, kMinTargetArea, kMaxLEDRange);

    // Add the simulated camera to view the targets on this simulated field.
    visionSim.addCamera(
        cameraSim,
        new Transform3d(
            new Translation3d(0, 0, CAMERA_HEIGHT_METERS),
            new Rotation3d(0, -CAMERA_PITCH_RADIANS, 0)));

    cameraSim.enableDrawWireframe(true);
  }

  /**
   * Update the simulated camera data based on its new field pose.
   *
   * @param simRobotPose The pose of the simulated robot
   */
  public void update(Pose2d simRobotPose) {
    visionSim.update(simRobotPose);
  }

  /**
   * Resets the simulation back to a pre-defined pose. Useful to simulate the action of placing the
   * robot onto a specific spot in the field (e.g. at the start of each match).
   *
   * @param pose
   */
  public void resetPose(Pose2d pose) {
    visionSim.resetRobotPose(pose);
  }
}
