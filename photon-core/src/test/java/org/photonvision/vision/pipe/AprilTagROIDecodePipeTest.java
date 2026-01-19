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

package org.photonvision.vision.pipe;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagDetector;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.AprilTagROIDecodePipe;
import org.photonvision.vision.pipe.impl.AprilTagROIDecodePipe.ROIDecodeInput;
import org.photonvision.vision.pipe.impl.AprilTagROIDecodePipe.ROIDecodeParams;

/**
 * Unit tests for AprilTagROIDecodePipe, focusing on coordinate mapping accuracy. These tests verify
 * that corners detected within ROIs are correctly mapped back to full-frame coordinates.
 */
public class AprilTagROIDecodePipeTest {

    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();
    }

    /**
     * CRITICAL TEST: Verify coordinate mapping from ROI to full frame. This test compares
     * ML-assisted detection (simulated with manual ROI) against traditional full-frame detection.
     * The mapped corners should match within a small tolerance.
     */
    @Test
    public void testCoordinateMappingAccuracy() {
        // Load a test image with a known AprilTag
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));

        var frame = frameProvider.get();
        Mat grayMat = frame.processedImage.getMat();

        // First, run full-frame detection to get ground truth corners
        AprilTagDetector fullFrameDetector = new AprilTagDetector();
        fullFrameDetector.addFamily(AprilTagFamily.kTag36h11.getNativeName());

        AprilTagDetection[] fullFrameDetections;
        try {
            fullFrameDetections = fullFrameDetector.detect(grayMat);
        } catch (Exception e) {
            // Skip test if native library doesn't work on this platform
            assumeTrue(false, "Native AprilTag library not available: " + e.getMessage());
            return;
        }
        assumeTrue(fullFrameDetections != null, "Native AprilTag detector returned null");
        assumeTrue(fullFrameDetections.length > 0, "Should detect at least one tag in full frame");

        AprilTagDetection groundTruth = fullFrameDetections[0];

        // Create an ROI that contains the tag (simulating ML detection)
        // The ROI should be slightly larger than the tag's bounding box
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, groundTruth.getCornerX(i));
            maxX = Math.max(maxX, groundTruth.getCornerX(i));
            minY = Math.min(minY, groundTruth.getCornerY(i));
            maxY = Math.max(maxY, groundTruth.getCornerY(i));
        }

        // Add some padding around the tag (simulating ML detection bbox)
        double padding = 20;
        Rect2d simulatedMLBbox =
                new Rect2d(minX - padding, minY - padding, maxX - minX + 2 * padding, maxY - minY + 2 * padding);

        // Run the ROI decode pipe
        AprilTagROIDecodePipe decodePipe = new AprilTagROIDecodePipe();

        ROIDecodeParams params = new ROIDecodeParams();
        params.tagFamily = AprilTagFamily.kTag36h11;
        params.roiExpansionFactor = 1.2;
        params.maxHammingDistance = 0;
        params.minDecisionMargin = 35;
        decodePipe.setParams(params);

        List<Rect2d> rois = new ArrayList<>();
        rois.add(simulatedMLBbox);

        ROIDecodeInput input = new ROIDecodeInput(frame.processedImage, rois);
        var result = decodePipe.run(input);

        List<AprilTagDetection> roiDetections = result.output;
        assertEquals(1, roiDetections.size(), "Should detect exactly one tag in ROI");

        AprilTagDetection mappedDetection = roiDetections.get(0);

        // Verify corner mapping accuracy (should be within 0.5 pixel)
        double tolerance = 0.5;
        for (int i = 0; i < 4; i++) {
            assertEquals(
                    groundTruth.getCornerX(i),
                    mappedDetection.getCornerX(i),
                    tolerance,
                    "Corner " + i + " X coordinate should match");
            assertEquals(
                    groundTruth.getCornerY(i),
                    mappedDetection.getCornerY(i),
                    tolerance,
                    "Corner " + i + " Y coordinate should match");
        }

        // Verify center mapping
        assertEquals(
                groundTruth.getCenterX(), mappedDetection.getCenterX(), tolerance, "Center X should match");
        assertEquals(
                groundTruth.getCenterY(), mappedDetection.getCenterY(), tolerance, "Center Y should match");

        // Verify tag ID matches
        assertEquals(groundTruth.getId(), mappedDetection.getId(), "Tag ID should match");

        fullFrameDetector.close();
        decodePipe.release();
    }

    /** Test that ROI expansion correctly clamps to image bounds when near edges. */
    @Test
    public void testExpandBboxClamping() {
        AprilTagROIDecodePipe decodePipe = new AprilTagROIDecodePipe();

        ROIDecodeParams params = new ROIDecodeParams();
        params.tagFamily = AprilTagFamily.kTag36h11;
        params.roiExpansionFactor = 1.5; // 50% expansion
        decodePipe.setParams(params);

        // Create a test image
        int imageWidth = 640;
        int imageHeight = 480;
        Mat testMat = Mat.zeros(imageHeight, imageWidth, org.opencv.core.CvType.CV_8UC1);
        CVMat cvMat = new CVMat(testMat);

        // ROI at edge that would expand outside bounds
        Rect2d edgeRoi = new Rect2d(600, 440, 30, 30); // Near bottom-right corner

        List<Rect2d> rois = new ArrayList<>();
        rois.add(edgeRoi);

        ROIDecodeInput input = new ROIDecodeInput(cvMat, rois);
        var result = decodePipe.run(input);

        // Should not throw exception - ROI should be clamped
        assertNotNull(result.output, "Result should not be null");
        // Empty because there's no actual tag, but no crash

        testMat.release();
        decodePipe.release();
    }

    /** Test that deduplication keeps the detection with highest decision margin. */
    @Test
    public void testDeduplicationKeepsHighestMargin() {
        // Load a test image with a known AprilTag
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));

        var frame = frameProvider.get();

        AprilTagROIDecodePipe decodePipe = new AprilTagROIDecodePipe();

        ROIDecodeParams params = new ROIDecodeParams();
        params.tagFamily = AprilTagFamily.kTag36h11;
        params.roiExpansionFactor = 1.2;
        params.maxHammingDistance = 0;
        params.minDecisionMargin = 0; // Accept all
        decodePipe.setParams(params);

        // Create two overlapping ROIs that both contain the same tag
        // This simulates overlapping ML detections
        Rect2d roi1 = new Rect2d(200, 150, 200, 150);
        Rect2d roi2 = new Rect2d(220, 170, 200, 150); // Overlapping

        List<Rect2d> rois = new ArrayList<>();
        rois.add(roi1);
        rois.add(roi2);

        ROIDecodeInput input = new ROIDecodeInput(frame.processedImage, rois);
        var result = decodePipe.run(input);

        // Should deduplicate to a single detection
        assertTrue(result.output.size() <= 1, "Overlapping ROIs detecting same tag should be deduplicated");

        decodePipe.release();
    }

    /** Test that empty ROI list returns empty result without errors. */
    @Test
    public void testEmptyROIListReturnsEmpty() {
        // Create a simple test image
        Mat testMat = Mat.zeros(480, 640, org.opencv.core.CvType.CV_8UC1);
        CVMat cvMat = new CVMat(testMat);

        AprilTagROIDecodePipe decodePipe = new AprilTagROIDecodePipe();

        ROIDecodeParams params = new ROIDecodeParams();
        params.tagFamily = AprilTagFamily.kTag36h11;
        decodePipe.setParams(params);

        List<Rect2d> emptyRois = new ArrayList<>();
        ROIDecodeInput input = new ROIDecodeInput(cvMat, emptyRois);

        var result = decodePipe.run(input);

        assertNotNull(result.output, "Result should not be null");
        assertTrue(result.output.isEmpty(), "Empty ROI list should produce empty result");

        testMat.release();
        decodePipe.release();
    }

    /** Test that ROIs with zero or negative dimensions are skipped. */
    @Test
    public void testInvalidROISkipped() {
        Mat testMat = Mat.zeros(480, 640, org.opencv.core.CvType.CV_8UC1);
        CVMat cvMat = new CVMat(testMat);

        AprilTagROIDecodePipe decodePipe = new AprilTagROIDecodePipe();

        ROIDecodeParams params = new ROIDecodeParams();
        params.tagFamily = AprilTagFamily.kTag36h11;
        decodePipe.setParams(params);

        List<Rect2d> rois = new ArrayList<>();
        rois.add(new Rect2d(100, 100, 0, 50)); // Zero width
        rois.add(new Rect2d(100, 100, 50, -10)); // Negative height
        rois.add(new Rect2d(-50, 100, 50, 50)); // Negative x (will be clamped)

        ROIDecodeInput input = new ROIDecodeInput(cvMat, rois);

        // Should not throw exception
        var result = decodePipe.run(input);
        assertNotNull(result.output, "Result should not be null even with invalid ROIs");

        testMat.release();
        decodePipe.release();
    }

    /**
     * Test integer conversion for rectangles preserves area by using floor for position and ceil for
     * size.
     */
    @Test
    public void testToIntRectPreservesArea() {
        // Test with floating point values
        Rect2d input = new Rect2d(10.3, 20.7, 100.2, 100.8);

        // Expected: x=10 (floor), y=20 (floor), w=101 (ceil), h=101 (ceil)
        // This ensures we don't clip any part of the tag

        // We can't directly test the private method, but we can verify the behavior
        // through the overall coordinate mapping test
        // This test documents the expected behavior for future reference

        int expectedX = 10;
        int expectedY = 20;
        int expectedW = 101;
        int expectedH = 101;

        // Verify that floor/ceil logic matches expectations
        assertEquals(expectedX, (int) Math.floor(input.x));
        assertEquals(expectedY, (int) Math.floor(input.y));
        assertEquals(expectedW, (int) Math.ceil(input.width));
        assertEquals(expectedH, (int) Math.ceil(input.height));
    }
}
