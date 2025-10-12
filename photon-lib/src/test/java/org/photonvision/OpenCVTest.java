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

import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.CombinedRuntimeLoader;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.photonvision.estimation.CameraTargetRelation;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.estimation.RotTrlTransform3d;
import org.photonvision.estimation.TargetModel;
import org.photonvision.simulation.SimCameraProperties;
import org.photonvision.simulation.VisionTargetSim;

public class OpenCVTest {
    private static final double kTrlDelta = 0.005;
    private static final double kRotDeltaDeg = 0.25;

    public static void assertSame(Translation3d trl1, Translation3d trl2) {
        assertEquals(0, trl1.getX() - trl2.getX(), kTrlDelta, "Trl X Diff");
        assertEquals(0, trl1.getY() - trl2.getY(), kTrlDelta, "Trl Y Diff");
        assertEquals(0, trl1.getZ() - trl2.getZ(), kTrlDelta, "Trl Z Diff");
    }

    public static void assertSame(Rotation3d rot1, Rotation3d rot2) {
        assertEquals(0, MathUtil.angleModulus(rot1.getX() - rot2.getX()), kRotDeltaDeg, "Rot X Diff");
        assertEquals(0, MathUtil.angleModulus(rot1.getY() - rot2.getY()), kRotDeltaDeg, "Rot Y Diff");
        assertEquals(0, MathUtil.angleModulus(rot1.getZ() - rot2.getZ()), kRotDeltaDeg, "Rot Z Diff");
        assertEquals(
                0, MathUtil.angleModulus(rot1.getAngle() - rot2.getAngle()), kRotDeltaDeg, "Rot W Diff");
    }

    public static void assertSame(Pose3d pose1, Pose3d pose2) {
        assertSame(pose1.getTranslation(), pose2.getTranslation());
        assertSame(pose1.getRotation(), pose2.getRotation());
    }

    public static void assertSame(Transform3d trf1, Transform3d trf2) {
        assertSame(trf1.getTranslation(), trf2.getTranslation());
        assertSame(trf1.getRotation(), trf2.getRotation());
    }

    private static final SimCameraProperties prop = new SimCameraProperties();

    @BeforeAll
    public static void setUp() throws IOException {
        CombinedRuntimeLoader.loadLibraries(OpenCVTest.class, Core.NATIVE_LIBRARY_NAME);

        // NT live for debug purposes
        NetworkTableInstance.getDefault().startServer();

        // No version check for testing
        PhotonCamera.setVersionCheckEnabled(false);
    }

    @Test
    public void testTrlConvert() {
        var trl = new Translation3d(0.75, -0.4, 0.1);
        var tvec = OpenCVHelp.translationToTvec(trl);
        var result = OpenCVHelp.tvecToTranslation(tvec);
        tvec.release();
        assertSame(trl, result);
    }

    @Test
    public void testRotConvert() {
        var rot = new Rotation3d(0.5, 1, -1);
        var rvec = OpenCVHelp.rotationToRvec(rot);
        var result = OpenCVHelp.rvecToRotation(rvec);
        rvec.release();
        var diff = result.minus(rot);
        assertSame(new Rotation3d(), diff);
    }

    @Test
    public void testProjection() {
        var target =
                new VisionTargetSim(
                        new Pose3d(1, 0, 0, new Rotation3d(0, 0, Math.PI)), TargetModel.kAprilTag16h5, 0);
        var cameraPose = new Pose3d(0, 0, 0, new Rotation3d());
        var camRt = RotTrlTransform3d.makeRelativeTo(cameraPose);
        var imagePoints =
                OpenCVHelp.projectPoints(
                        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices());

        // find circulation (counter/clockwise-ness)
        double circulation = 0;
        for (int i = 0; i < imagePoints.length; i++) {
            double xDiff = imagePoints[(i + 1) % 4].x - imagePoints[i].x;
            double ySum = imagePoints[(i + 1) % 4].y + imagePoints[i].y;
            circulation += xDiff * ySum;
        }
        assertTrue(circulation > 0, "2d fiducial points aren't counter-clockwise");

        // undo projection distortion
        imagePoints =
                OpenCVHelp.undistortPoints(prop.getIntrinsics(), prop.getDistCoeffs(), imagePoints);

        // test projection results after moving camera
        var avgCenterRot1 = prop.getPixelRot(OpenCVHelp.avgPoint(imagePoints));
        cameraPose =
                cameraPose.plus(new Transform3d(new Translation3d(), new Rotation3d(0, 0.25, 0.25)));
        camRt = RotTrlTransform3d.makeRelativeTo(cameraPose);
        imagePoints =
                OpenCVHelp.projectPoints(
                        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices());
        var avgCenterRot2 = prop.getPixelRot(OpenCVHelp.avgPoint(imagePoints));

        var yaw2d = new Rotation2d(avgCenterRot2.getZ());
        var pitch2d = new Rotation2d(avgCenterRot2.getY());
        var yawDiff = yaw2d.minus(new Rotation2d(avgCenterRot1.getZ()));
        var pitchDiff = pitch2d.minus(new Rotation2d(avgCenterRot1.getY()));
        assertTrue(yawDiff.getRadians() < 0, "2d points don't follow yaw");
        assertTrue(pitchDiff.getRadians() < 0, "2d points don't follow pitch");

        var actualRelation = new CameraTargetRelation(cameraPose, target.getPose());
        assertEquals(
                actualRelation.camToTargPitch.getDegrees(),
                pitchDiff.getDegrees()
                        * Math.cos(yaw2d.getRadians()), // adjust for unaccounted perspective distortion
                kRotDeltaDeg,
                "2d pitch doesn't match 3d");
        assertEquals(
                actualRelation.camToTargYaw.getDegrees(),
                yawDiff.getDegrees(),
                kRotDeltaDeg,
                "2d yaw doesn't match 3d");
    }

    @Test
    public void testSolvePNP_SQUARE() {
        // square AprilTag target
        var target =
                new VisionTargetSim(
                        new Pose3d(5, 0.5, 1, new Rotation3d(0, 0, Math.PI)), TargetModel.kAprilTag16h5, 0);
        var cameraPose = new Pose3d(0, 0, 0, new Rotation3d());
        var camRt = RotTrlTransform3d.makeRelativeTo(cameraPose);
        // target relative to camera
        var relTarget = camRt.apply(target.getPose());

        // simulate solvePNP estimation
        var targetCorners =
                OpenCVHelp.projectPoints(
                        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices());
        var pnpSim =
                OpenCVHelp.solvePNP_SQUARE(
                                prop.getIntrinsics(),
                                prop.getDistCoeffs(),
                                target.getModel().vertices,
                                targetCorners)
                        .get();

        // check solvePNP estimation accuracy
        assertSame(relTarget.getRotation(), pnpSim.best.getRotation());
        assertSame(relTarget.getTranslation(), pnpSim.best.getTranslation());
    }

    @Test
    public void testSolvePNP_SQPNP() {
        // (for targets with arbitrary number of non-colinear points > 2)
        var target =
                new VisionTargetSim(
                        new Pose3d(5, 0.5, 1, new Rotation3d(0, 0, Math.PI)),
                        new TargetModel(
                                List.of(
                                        new Translation3d(0, 0, 0),
                                        new Translation3d(1, 0, 0),
                                        new Translation3d(0, 1, 0),
                                        new Translation3d(0, 0, 1),
                                        new Translation3d(0.125, 0.25, 0.5),
                                        new Translation3d(0, 0, -1),
                                        new Translation3d(0, -1, 0),
                                        new Translation3d(-1, 0, 0))),
                        0);
        var cameraPose = new Pose3d(0, 0, 0, new Rotation3d());
        var camRt = RotTrlTransform3d.makeRelativeTo(cameraPose);
        // target relative to camera
        var relTarget = camRt.apply(target.getPose());

        // simulate solvePNP estimation
        var targetCorners =
                OpenCVHelp.projectPoints(
                        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices());
        var pnpSim =
                OpenCVHelp.solvePNP_SQPNP(
                                prop.getIntrinsics(),
                                prop.getDistCoeffs(),
                                target.getModel().vertices,
                                targetCorners)
                        .get();

        // check solvePNP estimation accuracy
        assertSame(relTarget.getRotation(), pnpSim.best.getRotation());
        assertSame(relTarget.getTranslation(), pnpSim.best.getTranslation());
    }
}
