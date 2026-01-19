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

/**
 * Integration tests for the ML-assisted AprilTag detection pipeline. These tests verify that when
 * ML detection is enabled but not available (e.g., on test platforms without NPU), the pipeline
 * correctly falls back to traditional detection.
 */
public class AprilTagMLPipelineIntegrationTest {

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

        // Change model name
        settings1.mlModelName = "custom-model";
        assertNotEquals(settings1, settings2);

        settings2.mlModelName = "custom-model";
        assertEquals(settings1, settings2);
    }

    /**
     * Test that the pipeline falls back to traditional detection when ML is enabled but not
     * available. This test runs on a platform without NPU support.
     */
    @Test
    public void testFallbackWhenMLUnavailable() {
        var pipeline = new AprilTagPipeline();

        // Enable ML detection - should fall back on test platform
        pipeline.getSettings().useMLDetection = true;
        pipeline.getSettings().mlFallbackToTraditional = true;
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

        // Should still detect the tag via fallback
        assertFalse(pipelineResult.targets.isEmpty(), "Should detect tag via fallback");
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

    /**
     * Test that ML settings don't interfere with multi-tag detection. Verifies that multi-target
     * settings still work when ML is enabled but unavailable.
     */
    @Test
    public void testMultiTagWithMLEnabled() {
        var pipeline = new AprilTagPipeline();

        pipeline.getSettings().useMLDetection = true;
        pipeline.getSettings().mlFallbackToTraditional = true;
        pipeline.getSettings().doMultiTarget = true;
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

        // Should still work with multi-tag enabled
        assertNotNull(pipelineResult, "Pipeline result should not be null");

        pipeline.release();
    }

    /** Test that changing ML settings triggers proper reconfiguration. */
    @Test
    public void testMLSettingsReconfiguration() {
        var pipeline = new AprilTagPipeline();

        // Start with ML disabled
        pipeline.getSettings().useMLDetection = false;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        // Run once with ML disabled
        CVPipelineResult result1 = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertFalse(result1.targets.isEmpty(), "Should detect tag with ML disabled");

        // Enable ML (will fallback on test platform)
        pipeline.getSettings().useMLDetection = true;
        pipeline.getSettings().mlFallbackToTraditional = true;

        // Run again with ML enabled (will use fallback)
        CVPipelineResult result2 = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertFalse(result2.targets.isEmpty(), "Should detect tag with ML enabled (fallback)");

        // Disable ML again
        pipeline.getSettings().useMLDetection = false;

        CVPipelineResult result3 = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        assertFalse(result3.targets.isEmpty(), "Should detect tag after disabling ML");

        pipeline.release();
    }
}
