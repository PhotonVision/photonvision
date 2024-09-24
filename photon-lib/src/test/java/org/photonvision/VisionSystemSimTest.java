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

package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.photonvision.estimation.TargetModel;
import org.photonvision.estimation.VisionEstimation;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.simulation.VisionSystemSim;
import org.photonvision.simulation.VisionTargetSim;
import org.photonvision.targeting.PhotonTrackedTarget;

class VisionSystemSimTest {
    private static final double kTrlDelta = 0.005;
    private static final double kRotDeltaDeg = 0.25;

    @Test
    public void testEmpty() {
        Assertions.assertDoesNotThrow(
                () -> {
                    var sysUnderTest = new VisionSystemSim("Test");
                    sysUnderTest.addVisionTargets(
                            new VisionTargetSim(new Pose3d(), new TargetModel(1.0, 1.0)));
                    for (int loopIdx = 0; loopIdx < 100; loopIdx++) {
                        sysUnderTest.update(new Pose2d());
                    }
                });
    }

    @BeforeAll
    public static void setUp() {
        // NT live for debug purposes
        NetworkTableInstance.getDefault().startServer();

        // No version check for testing
        PhotonCamera.setVersionCheckEnabled(false);
    }

    @AfterAll
    public static void shutDown() {}

    // @ParameterizedTest
    // @ValueSource(doubles = {5, 10, 15, 20, 25, 30})
    // public void testDistanceAligned(double dist) {
    //     final var targetPose = new Pose2d(new Translation2d(15.98, 0), new Rotation2d());
    //     var sysUnderTest =
    //             new SimVisionSystem("Test", 80.0, 0.0, new Transform2d(), 1, 99999, 320, 240, 0);
    //     sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 0.0, 1.0, 1.0));

    //     final var robotPose = new Pose2d(new Translation2d(35 - dist, 0), new Rotation2d());
    //     sysUnderTest.processFrame(robotPose);

    //     var result = sysUnderTest.cam.getLatestResult();

    //     assertTrue(result.hasTargets());
    //     assertEquals(result.getBestTarget().getCameraToTarget().getTranslation().getNorm(), dist);
    // }

    @Test
    public void testVisibilityCupidShuffle() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 2), new Rotation3d(0, 0, Math.PI));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(80));
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(1.0, 1.0), 3));

        // To the right, to the right
        var robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(-70));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());

        // To the right, to the right
        robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(-95));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());

        // To the left, to the left
        robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(90));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());

        // To the left, to the left
        robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(65));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());

        // now kick, now kick
        robotPose = new Pose2d(new Translation2d(2, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());

        // now kick, now kick
        robotPose = new Pose2d(new Translation2d(2, 0), Rotation2d.fromDegrees(-5));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());

        // now walk it by yourself
        robotPose = new Pose2d(new Translation2d(2, 0), Rotation2d.fromDegrees(-179));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());

        // now walk it by yourself
        visionSysSim.adjustCamera(
                cameraSim, new Transform3d(new Translation3d(), new Rotation3d(0, 0, Math.PI)));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleVert1() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(80));
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(1.0, 3.0), 3));

        var robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());

        visionSysSim.adjustCamera( // vooop selfie stick
                cameraSim, new Transform3d(new Translation3d(0, 0, 5000), new Rotation3d(0, 0, Math.PI)));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleVert2() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 2), new Rotation3d(0, 0, Math.PI));
        var robotToCamera =
                new Transform3d(new Translation3d(0, 0, 1), new Rotation3d(0, -Math.PI / 4, 0));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, robotToCamera);
        cameraSim.prop.setCalibration(1234, 1234, Rotation2d.fromDegrees(80));
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(1.0, 0.5), 1736));

        var robotPose = new Pose2d(new Translation2d(13.98, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());

        // Pitched back camera should mean target goes out of view below the robot as distance increases
        robotPose = new Pose2d(new Translation2d(0, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleTgtSize() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(80));
        cameraSim.setMinTargetAreaPixels(20.0);
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(0.1, 0.025), 24));

        var robotPose = new Pose2d(new Translation2d(12, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());

        robotPose = new Pose2d(new Translation2d(0, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleTooFarForLEDs() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(80));
        cameraSim.setMaxSightRange(10);
        cameraSim.setMinTargetAreaPixels(1.0);
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(1.0, 0.25), 78));

        var robotPose = new Pose2d(new Translation2d(10, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertTrue(camera.getLatestResult().hasTargets());

        robotPose = new Pose2d(new Translation2d(0, 0), Rotation2d.fromDegrees(5));
        visionSysSim.update(robotPose);
        assertFalse(camera.getLatestResult().hasTargets());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10, -5, -0, -1, -2, 5, 7, 10.23})
    public void testYawAngles(double testYaw) {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, 3 * Math.PI / 4));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(80));
        cameraSim.setMinTargetAreaPixels(0.0);
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(0.5, 0.5), 3));

        // If the robot is rotated x deg (CCW+), the target yaw should be x deg (CW+)
        var robotPose = new Pose2d(new Translation2d(10, 0), Rotation2d.fromDegrees(testYaw));
        visionSysSim.update(robotPose);
        var res = camera.getLatestResult();
        assertTrue(res.hasTargets());
        var tgt = res.getBestTarget();
        assertEquals(testYaw, tgt.getYaw(), kRotDeltaDeg);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10, -5, -0, -1, -2, 5, 7, 10.23, 20.21, -19.999})
    public void testPitchAngles(double testPitch) {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 0), new Rotation3d(0, 0, 3 * Math.PI / 4));
        final var robotPose = new Pose2d(new Translation2d(10, 0), new Rotation2d(0));
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(120));
        cameraSim.setMinTargetAreaPixels(0.0);
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(0.5, 0.5), 23));

        // Transform is now robot -> camera
        visionSysSim.adjustCamera(
                cameraSim,
                new Transform3d(
                        new Translation3d(), new Rotation3d(0, Units.degreesToRadians(testPitch), 0)));
        visionSysSim.update(robotPose);
        var res = camera.getLatestResult();
        assertTrue(res.hasTargets());
        var tgt = res.getBestTarget();

        // Since the camera is level with the target, a positive-upward point will mean the target is in
        // the
        // lower half of the image
        // which should produce negative pitch.
        assertEquals(testPitch, tgt.getPitch(), kRotDeltaDeg);
    }

    private static Stream<Arguments> testDistanceCalcArgs() {
        // Arbitrary and fairly random assortment of distances, camera pitches, and heights
        return Stream.of(
                Arguments.of(5, -15.98, 0),
                Arguments.of(6, -15.98, 1),
                Arguments.of(10, -15.98, 0),
                Arguments.of(15, -15.98, 2),
                Arguments.of(19.95, -15.98, 0),
                Arguments.of(20, -15.98, 0),
                Arguments.of(5, -42, 1),
                Arguments.of(6, -42, 0),
                Arguments.of(10, -42, 2),
                Arguments.of(15, -42, 0.5),
                Arguments.of(19.42, -15.98, 0),
                Arguments.of(20, -42, 0),
                Arguments.of(5, -35, 2),
                Arguments.of(6, -35, 0),
                Arguments.of(10, -34, 2.4),
                Arguments.of(15, -33, 0),
                Arguments.of(19.52, -15.98, 1.1));
    }

    @ParameterizedTest
    @MethodSource("testDistanceCalcArgs")
    public void testDistanceCalc(double testDist, double testPitch, double testHeight) {
        // Assume dist along ground and tgt height the same. Iterate over other parameters.

        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI * 0.98));
        final var robotPose =
                new Pose3d(new Translation3d(15.98 - Units.feetToMeters(testDist), 0, 0), new Rotation3d());
        final var robotToCamera =
                new Transform3d(
                        new Translation3d(0, 0, Units.feetToMeters(testHeight)),
                        new Rotation3d(0, Units.degreesToRadians(testPitch), 0));

        var visionSysSim =
                new VisionSystemSim(
                        "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysohowsyourdaygoingihopegoodhaveagreatrestofyourlife!");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(160));
        cameraSim.setMinTargetAreaPixels(0.0);
        visionSysSim.adjustCamera(cameraSim, robotToCamera);
        visionSysSim.addVisionTargets(new VisionTargetSim(targetPose, new TargetModel(0.5, 0.5), 0));

        visionSysSim.update(robotPose);

        // Note that target 2d yaw/pitch accuracy is hindered by two factors in photonvision:
        // 1. These are calculated with the average of the minimum area rectangle, which does not
        // actually find the target center because of perspective distortion.
        // 2. Yaw and pitch are calculated separately which gives incorrect pitch values.
        var res = camera.getLatestResult();
        assertTrue(res.hasTargets());
        var tgt = res.getBestTarget();
        assertEquals(0.0, tgt.getYaw(), 0.5);

        // Distance calculation using this trigonometry may be wildly incorrect when
        // there is not much height difference between the target and the camera.
        double distMeas =
                PhotonUtils.calculateDistanceToTargetMeters(
                        robotToCamera.getZ(),
                        targetPose.getZ(),
                        Units.degreesToRadians(-testPitch),
                        Units.degreesToRadians(tgt.getPitch()));
        assertEquals(Units.feetToMeters(testDist), distMeas, 0.15);
    }

    @Test
    public void testMultipleTargets() {
        final var targetPoseL =
                new Pose3d(new Translation3d(15.98, 2, 0), new Rotation3d(0, 0, Math.PI));
        final var targetPoseC =
                new Pose3d(new Translation3d(15.98, 0, 0), new Rotation3d(0, 0, Math.PI));
        final var targetPoseR =
                new Pose3d(new Translation3d(15.98, -2, 0), new Rotation3d(0, 0, Math.PI));

        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(80));
        cameraSim.setMinTargetAreaPixels(20.0);

        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.00), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        1));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseC.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.00), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        2));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseR.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.00), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        3));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 1.00), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        4));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseC.transformBy(
                                new Transform3d(new Translation3d(0, 0, 1.00), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        5));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseR.transformBy(
                                new Transform3d(new Translation3d(0, 0, 1.00), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        6));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.50), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        7));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseC.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.50), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        8));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.75), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        9));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseR.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.75), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        10));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.25), new Rotation3d())),
                        TargetModel.kAprilTag16h5,
                        11));

        var robotPose = new Pose2d(new Translation2d(6.0, 0), Rotation2d.fromDegrees(0.25));
        visionSysSim.update(robotPose);
        var res = camera.getLatestResult();
        assertTrue(res.hasTargets());
        List<PhotonTrackedTarget> tgtList;
        tgtList = res.getTargets();
        assertEquals(11, tgtList.size());
    }

    @Test
    public void testPoseEstimation() {
        var visionSysSim = new VisionSystemSim("Test");
        var camera = new PhotonCamera("camera");
        var cameraSim = new PhotonCameraSim(camera);
        visionSysSim.addCamera(cameraSim, new Transform3d());
        cameraSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(90));
        cameraSim.setMinTargetAreaPixels(20.0);

        List<AprilTag> tagList = new ArrayList<>();
        tagList.add(new AprilTag(0, new Pose3d(12, 3, 1, new Rotation3d(0, 0, Math.PI))));
        tagList.add(new AprilTag(1, new Pose3d(12, 1, -1, new Rotation3d(0, 0, Math.PI))));
        tagList.add(new AprilTag(2, new Pose3d(11, 0, 2, new Rotation3d(0, 0, Math.PI))));
        double fieldLength = Units.feetToMeters(54.0);
        double fieldWidth = Units.feetToMeters(27.0);
        AprilTagFieldLayout layout = new AprilTagFieldLayout(tagList, fieldLength, fieldWidth);
        Pose2d robotPose = new Pose2d(5, 1, Rotation2d.fromDegrees(5));

        visionSysSim.addVisionTargets(
                new VisionTargetSim(tagList.get(0).pose, TargetModel.kAprilTag16h5, 0));

        visionSysSim.update(robotPose);
        var results =
                VisionEstimation.estimateCamPosePNP(
                                camera.getCameraMatrix().get(),
                                camera.getDistCoeffs().get(),
                                camera.getLatestResult().getTargets(),
                                layout,
                                TargetModel.kAprilTag16h5)
                        .get();
        Pose3d pose = new Pose3d().plus(results.best);
        assertEquals(5, pose.getX(), .01);
        assertEquals(1, pose.getY(), .01);
        assertEquals(0, pose.getZ(), .01);
        assertEquals(Math.toRadians(5), pose.getRotation().getZ(), 0.01);

        visionSysSim.addVisionTargets(
                new VisionTargetSim(tagList.get(1).pose, TargetModel.kAprilTag16h5, 1));
        visionSysSim.addVisionTargets(
                new VisionTargetSim(tagList.get(2).pose, TargetModel.kAprilTag16h5, 2));

        visionSysSim.update(robotPose);
        results =
                VisionEstimation.estimateCamPosePNP(
                                camera.getCameraMatrix().get(),
                                camera.getDistCoeffs().get(),
                                camera.getLatestResult().getTargets(),
                                layout,
                                TargetModel.kAprilTag16h5)
                        .get();
        pose = new Pose3d().plus(results.best);
        assertEquals(5, pose.getX(), .01);
        assertEquals(1, pose.getY(), .01);
        assertEquals(0, pose.getZ(), .01);
        assertEquals(Math.toRadians(5), pose.getRotation().getZ(), 0.01);
    }
}
