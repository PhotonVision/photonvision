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

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Translation3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;

public class AprilTagTest {
    @BeforeEach
    public void setup() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();
    }

    @Test
    public void testApriltagFacingCamera() {
        var pipeline = new AprilTagPipeline();

        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().targetModel = TargetModel.kAprilTag6p5in_36h11;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult;
        pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        TestUtils.printTestResultsWithLocation(pipelineResult);

        // Draw on input
        var outputPipe = new OutputStreamPipeline();
        var ret =
                outputPipe.process(
                        pipelineResult.inputAndOutputFrame, pipeline.getSettings(), pipelineResult.targets);

        TestUtils.showImage(ret.inputAndOutputFrame.processedImage.getMat(), "Pipeline output", 999999);

        // these numbers are not *accurate*, but they are known and expected
        var target = pipelineResult.targets.get(0);

        // Test corner order
        var corners = target.getTargetCorners();
        assertEquals(260, corners.get(0).x, 10);
        assertEquals(245, corners.get(0).y, 10);
        assertEquals(315, corners.get(1).x, 10);
        assertEquals(245, corners.get(1).y, 10);
        assertEquals(315, corners.get(2).x, 10);
        assertEquals(190, corners.get(2).y, 10);
        assertEquals(260, corners.get(3).x, 10);
        assertEquals(190, corners.get(3).y, 10);

        var pose = target.getBestCameraToTarget3d();
        // Test pose estimate translation
        assertEquals(2, pose.getTranslation().getX(), 0.2);
        assertEquals(0.1, pose.getTranslation().getY(), 0.2);
        assertEquals(0.0, pose.getTranslation().getZ(), 0.2);

        // Test pose estimate rotation
        // We expect the object axes to be in NWU, with the x-axis coming out of the tag
        // This visible tag is facing the camera almost parallel, so in world space:

        // The object's X axis should be (-1, 0, 0)
        assertEquals(-1, new Translation3d(1, 0, 0).rotateBy(pose.getRotation()).getX(), 0.1);
        // The object's Y axis should be (0, -1, 0)
        assertEquals(-1, new Translation3d(0, 1, 0).rotateBy(pose.getRotation()).getY(), 0.1);
        // The object's Z axis should be (0, 0, 1)
        assertEquals(1, new Translation3d(0, 0, 1).rotateBy(pose.getRotation()).getZ(), 0.1);
    }

    @Test
    public void testApriltagDistorted() {
        var pipeline = new AprilTagPipeline();

        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag16h5;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag_corner_1280, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.getCoeffs(TestUtils.LIMELIGHT_480P_CAL_FILE, false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult;
        pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        TestUtils.printTestResultsWithLocation(pipelineResult);

        // Draw on input
        var outputPipe = new OutputStreamPipeline();
        var ret =
                outputPipe.process(
                        pipelineResult.inputAndOutputFrame, pipeline.getSettings(), pipelineResult.targets);

        TestUtils.showImage(ret.inputAndOutputFrame.processedImage.getMat(), "Pipeline output", 999999);

        // these numbers are not *accurate*, but they are known and expected
        var pose = pipelineResult.targets.get(0).getBestCameraToTarget3d();
        assertEquals(4.14, pose.getTranslation().getX(), 0.2);
        assertEquals(2, pose.getTranslation().getY(), 0.2);
        assertEquals(0.0, pose.getTranslation().getZ(), 0.2);
    }
}
