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
import java.io.IOException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class ArucoPipelineTest {
    @BeforeEach
    public void Init() throws IOException {
        TestUtils.loadLibraries();
    }

    @Test
    public void testApriltagFacingCameraAruco() {
        var pipeline = new ArucoPipeline();

        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().targetModel = TargetModel.k200mmAprilTag;

        // pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_16h5_1280, false),
                        106,
                        TestUtils.getCoeffs("laptop_1280.json", false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult;
        try {
            pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
            printTestResults(pipelineResult);
        } catch (RuntimeException e) {
            // For now, will throw coz rotation3d ctor
            return;
        }

        // Draw on input
        var outputPipe = new OutputStreamPipeline();
        outputPipe.process(
                pipelineResult.inputAndOutputFrame, pipeline.getSettings(), pipelineResult.targets);

        TestUtils.showImage(
                pipelineResult.inputAndOutputFrame.processedImage.getMat(), "Pipeline output", 999999);

        // these numbers are not *accurate*, but they are known and expected
        var pose = pipelineResult.targets.get(0).getBestCameraToTarget3d();
        Assertions.assertEquals(2, pose.getTranslation().getX(), 0.2);
        Assertions.assertEquals(0.0, pose.getTranslation().getY(), 0.2);
        Assertions.assertEquals(0.0, pose.getTranslation().getY(), 0.2);

        var objX = new Translation3d(1, 0, 0).rotateBy(pose.getRotation()).getY();
        var objY = new Translation3d(0, 1, 0).rotateBy(pose.getRotation()).getZ();
        var objZ = new Translation3d(0, 0, 1).rotateBy(pose.getRotation()).getX();
        System.out.printf("Object x %.2f y %.2f z %.2f\n", objX, objY, objZ);

        // We expect the object X to be forward, or -X in world space
        Assertions.assertEquals(
                -1, new Translation3d(1, 0, 0).rotateBy(pose.getRotation()).getX(), 0.1);
        // We expect the object Y axis to be right, or negative-Y in world space
        Assertions.assertEquals(
                -1, new Translation3d(0, 1, 0).rotateBy(pose.getRotation()).getY(), 0.1);
        // We expect the object Z axis to be up, or +Z in world space
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
