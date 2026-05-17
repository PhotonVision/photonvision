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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;
import org.photonvision.common.configuration.NeuralNetworkModelsSettings.ModelProperties;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;

/**
 * Safety tests for the ML-assisted AprilTag pipeline's flag handling. None of these tests actually
 * exercise the ML code path — they run on hosts without an NPU, so {@code useMLDetection = true}
 * falls through to traditional detection. The tests verify that toggling ML-related flags does not
 * crash the pipeline and that ML-related settings serialise correctly.
 */
public class AprilTagPipelineMLFlagSafetyTest {
    @BeforeEach
    public void setup() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();
    }

    /**
     * Test that ML settings are properly handled in pipeline settings. Verifies equals works
     * correctly with new ML fields.
     */
    @Test
    public void testMLSettingsEquality() {
        var settings1 = new AprilTagPipelineSettings();
        var settings2 = new AprilTagPipelineSettings();

        // Both should be equal initially
        assertEquals(settings1, settings2);

        // Change ML setting
        settings1.useMLDetection = true;
        assertNotEquals(settings1, settings2);

        settings2.useMLDetection = true;
        assertEquals(settings1, settings2);

        // Change threshold
        settings1.mlConfidenceThreshold = 0.7;
        assertNotEquals(settings1, settings2);

        settings2.mlConfidenceThreshold = 0.7;
        assertEquals(settings1, settings2);

        // Change model.
        var customModel =
                new ModelProperties(
                        Path.of("test", "custom-model.rknn").toAbsolutePath(),
                        "custom-model",
                        List.of(),
                        640,
                        640,
                        Family.RKNN,
                        Version.YOLOV11);
        settings1.model = customModel;
        assertNotEquals(settings1, settings2);

        settings2.model =
                new ModelProperties(
                        Path.of("test", "custom-model.rknn").toAbsolutePath(),
                        "custom-model",
                        List.of(),
                        640,
                        640,
                        Family.RKNN,
                        Version.YOLOV11);
        assertEquals(settings1, settings2);
    }

    /**
     * Test that the pipeline still detects tags when ML detection is enabled on a platform without ML
     * support (e.g., no NPU). In that case the pipeline uses traditional detection.
     */
    @Test
    public void testMLEnabledOnNonNpuPlatform() {
        var pipeline = new AprilTagPipeline();

        pipeline.getSettings().useMLDetection = true;
        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().targetModel = TargetModel.kAprilTag6p5in_36h11;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);

        assertFalse(pipelineResult.targets.isEmpty(), "Should detect tag");
        assertEquals(1, pipelineResult.targets.size(), "Should detect exactly one tag");

        // Verify tag ID
        var target = pipelineResult.targets.get(0);
        assertEquals(1, target.getFiducialId(), "Should detect tag ID 1");

        pipeline.release();
    }

    /**
     * Test that traditional detection still works when ML is disabled. This ensures the existing
     * pipeline behavior is unchanged.
     */
    @Test
    public void testTraditionalDetectionWhenMLDisabled() {
        var pipeline = new AprilTagPipeline();

        // Explicitly disable ML detection
        pipeline.getSettings().useMLDetection = false;
        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().targetModel = TargetModel.kAprilTag6p5in_36h11;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);

        // Should detect the tag
        assertFalse(pipelineResult.targets.isEmpty(), "Should detect tag with traditional detection");
        assertEquals(1, pipelineResult.targets.size(), "Should detect exactly one tag");

        // Verify pose estimation works
        var target = pipelineResult.targets.get(0);
        var pose = target.getBestCameraToTarget3d();
        assertNotNull(pose, "Pose should be estimated");

        // Known approximate values from existing tests
        assertEquals(2, pose.getTranslation().getX(), 0.3, "X translation should be approximately 2m");

        pipeline.release();
    }
}
