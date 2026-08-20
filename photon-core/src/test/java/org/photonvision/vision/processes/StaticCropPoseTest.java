/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.StaticCropPipe;
import org.photonvision.vision.pipeline.AprilTagPipeline;
import org.photonvision.vision.target.TargetModel;
import org.wpilib.math.geometry.Transform3d;

/**
 * A static crop shifts the image origin, which the cropped calibration compensates for. Enabling a
 * crop therefore has to leave a stationary tag's estimated pose where it was -- these tests hold
 * that line, since a silent shift in reported pose is the kind of regression nobody notices until
 * it is on a robot.
 */
public class StaticCropPoseTest {
    /**
     * Tolerance on the pose, in meters. Cropping is not bit-exact for a decimated image: the detector
     * rounds the decimated grid differently near the edges, which moves corners by hundredths of a
     * pixel. A millimetre at 2m is a thousand times smaller than the ~2.5cm shift an unaligned crop
     * used to cause.
     */
    private static final double POSE_EPS = 0.002;

    @BeforeEach
    public void setup() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();
    }

    private static AprilTagPipeline pipelineWithDecimate(int decimate) {
        var pipeline = new AprilTagPipeline();
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().targetModel = TargetModel.kAprilTag6p5in_36h11;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;
        pipeline.getSettings().decimate = decimate;
        return pipeline;
    }

    private static FileFrameProvider tagImageProvider(AprilTagPipeline pipeline) {
        var provider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        provider.requestFrameThresholdType(pipeline.getThresholdType());
        return provider;
    }

    /**
     * The pose of the first target found, run through the pipeline as the vision thread would: grab a
     * frame, apply the static crop from the pipeline's settings, process.
     */
    private static Transform3d poseFrom(AprilTagPipeline pipeline, FileFrameProvider provider) {
        var frame =
                VisionRunner.cropFrame(
                        new StaticCropPipe(),
                        provider.get(),
                        VisionRunner.cropRectFromSettings(pipeline.getSettings()));
        var result = pipeline.run(frame, QuirkyCamera.DefaultCamera);
        assertFalse(result.targets.isEmpty(), "The tag should be found");
        return result.targets.get(0).getBestCameraToTarget3d();
    }

    private static void assertSamePose(Transform3d expected, Transform3d actual, String what) {
        assertEquals(expected.getX(), actual.getX(), POSE_EPS, what + " (x)");
        assertEquals(expected.getY(), actual.getY(), POSE_EPS, what + " (y)");
        assertEquals(expected.getZ(), actual.getZ(), POSE_EPS, what + " (z)");
    }

    /**
     * The crop bounds here are deliberately off the detector's tile grid, which is what a user
     * dragging a slider produces. Before the crop origin was aligned this moved the estimate by
     * centimetres.
     */
    @Test
    public void enablingAnUnalignedCropDoesNotMoveThePose() {
        for (int decimate : new int[] {1, 2, 4}) {
            var pipeline = pipelineWithDecimate(decimate);
            var provider = tagImageProvider(pipeline);

            var uncropped = poseFrom(pipeline, provider);

            var settings = pipeline.getSettings();
            settings.staticCropEnabled = true;
            settings.staticCropX.set(201, 501);
            settings.staticCropY.set(151, 401);

            assertSamePose(uncropped, poseFrom(pipeline, provider), "Cropping at decimate " + decimate);
        }
    }

    /** The same, with the frame rotated -- the crop is applied in the rotated image's coordinates. */
    @Test
    public void enablingACropOnARotatedFrameDoesNotMoveThePose() {
        var pipeline = pipelineWithDecimate(2);
        var provider = tagImageProvider(pipeline);
        provider.requestFrameRotation(ImageRotationMode.DEG_90_CCW);
        pipeline.getSettings().inputImageRotationMode = ImageRotationMode.DEG_90_CCW;

        var uncropped = poseFrom(pipeline, provider);

        var settings = pipeline.getSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX.set(101, 401);
        settings.staticCropY.set(103, 503);

        assertSamePose(uncropped, poseFrom(pipeline, provider), "Cropping a rotated frame");
    }

    /** Turning the crop back off has to restore the original estimate too. */
    @Test
    public void disablingTheCropRestoresThePose() {
        var pipeline = pipelineWithDecimate(2);
        var provider = tagImageProvider(pipeline);

        var uncropped = poseFrom(pipeline, provider);

        var settings = pipeline.getSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX.set(201, 501);
        settings.staticCropY.set(151, 401);
        poseFrom(pipeline, provider);

        settings.staticCropEnabled = false;

        assertSamePose(uncropped, poseFrom(pipeline, provider), "Disabling the crop");
    }
}
