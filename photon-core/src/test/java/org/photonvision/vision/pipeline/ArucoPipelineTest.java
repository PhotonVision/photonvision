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

import edu.wpi.first.math.geometry.Translation3d;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class ArucoPipelineTest {
    @BeforeEach
    public void setup() {
        TestUtils.loadLibraries();
        ConfigManager.getInstance().load();
    }

    @Test
    public void testApriltagFacingCamera() {
        var pipeline = new ArucoPipeline();

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
        printTestResults(pipelineResult);

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
        Assertions.assertEquals(260, corners.get(0).x, 10);
        Assertions.assertEquals(245, corners.get(0).y, 10);
        Assertions.assertEquals(315, corners.get(1).x, 10);
        Assertions.assertEquals(245, corners.get(1).y, 10);
        Assertions.assertEquals(315, corners.get(2).x, 10);
        Assertions.assertEquals(190, corners.get(2).y, 10);
        Assertions.assertEquals(260, corners.get(3).x, 10);
        Assertions.assertEquals(190, corners.get(3).y, 10);

        var pose = target.getBestCameraToTarget3d();
        // Test pose estimate translation
        Assertions.assertEquals(2, pose.getTranslation().getX(), 0.2);
        Assertions.assertEquals(0.1, pose.getTranslation().getY(), 0.2);
        Assertions.assertEquals(0.0, pose.getTranslation().getZ(), 0.2);

        // Test pose estimate rotation
        // We expect the object axes to be in NWU, with the x-axis coming out of the tag
        // This visible tag is facing the camera almost parallel, so in world space:

        // The object's X axis should be (-1, 0, 0)
        Assertions.assertEquals(
                -1, new Translation3d(1, 0, 0).rotateBy(pose.getRotation()).getX(), 0.1);
        // The object's Y axis should be (0, -1, 0)
        Assertions.assertEquals(
                -1, new Translation3d(0, 1, 0).rotateBy(pose.getRotation()).getY(), 0.1);
        // The object's Z axis should be (0, 0, 1)
        Assertions.assertEquals(1, new Translation3d(0, 0, 1).rotateBy(pose.getRotation()).getZ(), 0.1);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.println(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " " + "fps)");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
        System.out.println(
                "Found targets at "
                        + pipelineResult.targets.stream()
                                .map(TrackedTarget::getBestCameraToTarget3d)
                                .collect(Collectors.toList()));
    }
}
