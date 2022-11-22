/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;

import java.io.IOException;
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
import org.photonvision.targeting.PhotonTrackedTarget;

class SimVisionSystemTest {
    @Test
    public void testEmpty() {
        Assertions.assertDoesNotThrow(
                () -> {
                    var sysUnderTest =
                            new SimVisionSystem("Test", 80.0, new Transform3d(), 99999, 320, 240, 0);
                    sysUnderTest.addSimVisionTarget(new SimVisionTarget(new Pose3d(), 1.0, 1.0, 42));
                    for (int loopIdx = 0; loopIdx < 100; loopIdx++) {
                        sysUnderTest.processFrame(new Pose2d());
                    }
                });
    }

    @BeforeAll
    public static void setUp() {
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        try {
        CombinedRuntimeLoader.loadLibraries(SimVisionSystem.class, "wpiutiljni");
        CombinedRuntimeLoader.loadLibraries(SimVisionSystem.class, "ntcorejni");
        } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        } 


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
        var sysUnderTest = new SimVisionSystem("Test", 80.0, new Transform3d(), 99999, 640, 480, 0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 1.0, 3.0, 3));

        // To the right, to the right
        var robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(-70));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());

        // To the right, to the right
        robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(-95));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());

        // To the left, to the left
        robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(90));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());

        // To the left, to the left
        robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(65));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());

        // now kick, now kick
        robotPose = new Pose2d(new Translation2d(2, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());

        // now kick, now kick
        robotPose = new Pose2d(new Translation2d(2, 0), Rotation2d.fromDegrees(-5));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());

        // now walk it by yourself
        robotPose = new Pose2d(new Translation2d(2, 0), Rotation2d.fromDegrees(-179));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());

        // now walk it by yourself
        sysUnderTest.moveCamera(new Transform3d(new Translation3d(), new Rotation3d(0, 0, Math.PI)));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleVert1() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI));
        var sysUnderTest = new SimVisionSystem("Test", 80.0, new Transform3d(), 99999, 640, 480, 0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 1.0, 3.0, 3));

        var robotPose = new Pose2d(new Translation2d(5, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());

        sysUnderTest.moveCamera(
                new Transform3d(
                        new Translation3d(0, 0, 5000), new Rotation3d(0, 0, Math.PI))); // vooop selfie stick
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleVert2() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 2), new Rotation3d(0, 0, Math.PI));
        var robotToCamera =
                new Transform3d(new Translation3d(0, 0, 1), new Rotation3d(0, Math.PI / 4, 0));
        var sysUnderTest =
                new SimVisionSystem("Test", 80.0, robotToCamera.inverse(), 99999, 1234, 1234, 0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 3.0, 0.5, 1736));

        var robotPose = new Pose2d(new Translation2d(14.98, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());

        // Pitched back camera should mean target goes out of view below the robot as distance increases
        robotPose = new Pose2d(new Translation2d(0, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleTgtSize() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI));
        var sysUnderTest = new SimVisionSystem("Test", 80.0, new Transform3d(), 99999, 640, 480, 20.0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 0.1, 0.025, 24));

        var robotPose = new Pose2d(new Translation2d(12, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());

        robotPose = new Pose2d(new Translation2d(0, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());
    }

    @Test
    public void testNotVisibleTooFarForLEDs() {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, Math.PI));
        var sysUnderTest = new SimVisionSystem("Test", 80.0, new Transform3d(), 10, 640, 480, 1.0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 1.0, 0.25, 78));

        var robotPose = new Pose2d(new Translation2d(10, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertTrue(sysUnderTest.cam.getLatestResult().hasTargets());

        robotPose = new Pose2d(new Translation2d(0, 0), Rotation2d.fromDegrees(5));
        sysUnderTest.processFrame(robotPose);
        assertFalse(sysUnderTest.cam.getLatestResult().hasTargets());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10, -5, -0, -1, -2, 5, 7, 10.23})
    public void testYawAngles(double testYaw) {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 1), new Rotation3d(0, 0, 3 * Math.PI / 4));
        var sysUnderTest = new SimVisionSystem("Test", 80.0, new Transform3d(), 99999, 640, 480, 0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 0.5, 0.5, 3));

        var robotPose = new Pose2d(new Translation2d(10, 0), Rotation2d.fromDegrees(-1.0 * testYaw));
        sysUnderTest.processFrame(robotPose);
        var res = sysUnderTest.cam.getLatestResult();
        assertTrue(res.hasTargets());
        var tgt = res.getBestTarget();
        assertEquals(tgt.getYaw(), testYaw, 0.0001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10, -5, -0, -1, -2, 5, 7, 10.23, 20.21, -19.999})
    public void testCameraPitch(double testPitch) {
        final var targetPose =
                new Pose3d(new Translation3d(15.98, 0, 0), new Rotation3d(0, 0, 3 * Math.PI / 4));
        final var robotPose = new Pose2d(new Translation2d(10, 0), new Rotation2d(0));
        var sysUnderTest = new SimVisionSystem("Test", 120.0, new Transform3d(), 99999, 640, 480, 0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 0.5, 0.5, 23));

        // Here, passing in a positive testPitch points the camera downward (since moveCamera takes the
        // camera->robot transform)
        sysUnderTest.moveCamera(
                new Transform3d(
                        new Translation3d(), new Rotation3d(0, Units.degreesToRadians(testPitch), 0)));
        sysUnderTest.processFrame(robotPose);
        var res = sysUnderTest.cam.getLatestResult();
        assertTrue(res.hasTargets());
        var tgt = res.getBestTarget();

        // Since the camera is level with the target, a downward point will mean the target is in the
        // upper half of the image
        // which should produce positive pitch.
        assertEquals(testPitch, tgt.getPitch(), 0.0001);
    }

    private static Stream<Arguments> distCalCParamProvider() {
        // Arbitrary and fairly random assortment of distances, camera pitches, and heights
        return Stream.of(
                Arguments.of(5, 15.98, 0),
                Arguments.of(6, 15.98, 1),
                Arguments.of(10, 15.98, 0),
                Arguments.of(15, 15.98, 2),
                Arguments.of(19.95, 15.98, 0),
                Arguments.of(20, 15.98, 0),
                Arguments.of(5, 42, 1),
                Arguments.of(6, 42, 0),
                Arguments.of(10, 42, 2),
                Arguments.of(15, 42, 0.5),
                Arguments.of(19.42, 15.98, 0),
                Arguments.of(20, 42, 0),
                Arguments.of(5, 55, 2),
                Arguments.of(6, 55, 0),
                Arguments.of(10, 54, 2.2),
                Arguments.of(15, 53, 0),
                Arguments.of(19.52, 15.98, 1.1));
    }

    @ParameterizedTest
    @MethodSource("distCalCParamProvider")
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

        var sysUnderTest =
                new SimVisionSystem(
                        "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysohowsyourdaygoingihopegoodhaveagreatrestofyourlife!",
                        160.0,
                        robotToCamera.inverse(),
                        99999,
                        640,
                        480,
                        0);
        sysUnderTest.addSimVisionTarget(new SimVisionTarget(targetPose, 0.5, 0.5, 0));

        sysUnderTest.processFrame(robotPose);
        var res = sysUnderTest.cam.getLatestResult();
        assertTrue(res.hasTargets());
        var tgt = res.getBestTarget();
        assertEquals(tgt.getYaw(), 0.0, 0.0001);
        double distMeas =
                PhotonUtils.calculateDistanceToTargetMeters(
                        robotToCamera.getZ(),
                        targetPose.getZ(),
                        Units.degreesToRadians(testPitch),
                        Units.degreesToRadians(tgt.getPitch()));
        assertEquals(Units.feetToMeters(testDist), distMeas, 0.001);
    }

    @Test
    public void testMultipleTargets() {
        final var targetPoseL =
                new Pose3d(new Translation3d(15.98, 2, 0), new Rotation3d(0, 0, Math.PI));
        final var targetPoseC =
                new Pose3d(new Translation3d(15.98, 0, 0), new Rotation3d(0, 0, Math.PI));
        final var targetPoseR =
                new Pose3d(new Translation3d(15.98, -2, 0), new Rotation3d(0, 0, Math.PI));
        var sysUnderTest = new SimVisionSystem("Test", 160.0, new Transform3d(), 99999, 640, 480, 20.0);

        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.00), new Rotation3d())),
                        0.25,
                        0.25,
                        1));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseC.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.00), new Rotation3d())),
                        0.25,
                        0.25,
                        2));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseR.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.00), new Rotation3d())),
                        0.25,
                        0.25,
                        3));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 1.00), new Rotation3d())),
                        0.25,
                        0.25,
                        4));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseC.transformBy(
                                new Transform3d(new Translation3d(0, 0, 1.00), new Rotation3d())),
                        0.25,
                        0.25,
                        5));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseR.transformBy(
                                new Transform3d(new Translation3d(0, 0, 1.00), new Rotation3d())),
                        0.25,
                        0.25,
                        6));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.50), new Rotation3d())),
                        0.25,
                        0.25,
                        7));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseC.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.50), new Rotation3d())),
                        0.25,
                        0.25,
                        8));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.75), new Rotation3d())),
                        0.25,
                        0.25,
                        9));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseR.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.75), new Rotation3d())),
                        0.25,
                        0.25,
                        10));
        sysUnderTest.addSimVisionTarget(
                new SimVisionTarget(
                        targetPoseL.transformBy(
                                new Transform3d(new Translation3d(0, 0, 0.25), new Rotation3d())),
                        0.25,
                        0.25,
                        11));

        var robotPose = new Pose2d(new Translation2d(6.0, 0), Rotation2d.fromDegrees(0.25));
        sysUnderTest.processFrame(robotPose);
        var res = sysUnderTest.cam.getLatestResult();
        assertTrue(res.hasTargets());
        List<PhotonTrackedTarget> tgtList;
        tgtList = res.getTargets();
        assertEquals(tgtList.size(), 11);
    }
}
