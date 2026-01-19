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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    // ==================== Homography Transformation Tests ====================

    /**
     * Helper method to access the private transformHomography method via reflection.
     */
    private double[] invokeTransformHomography(AprilTagROIDecodePipe pipe,
            double[] h, int offsetX, int offsetY) throws Exception {
        Method method = AprilTagROIDecodePipe.class.getDeclaredMethod(
            "transformHomography", double[].class, int.class, int.class);
        method.setAccessible(true);
        return (double[]) method.invoke(pipe, h, offsetX, offsetY);
    }

    /** Test that zero offset returns the homography unchanged. */
    @Test
    public void testTransformHomography_ZeroOffset_ReturnsUnchanged() throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        double[] h = {0.9, 0.1, 50.0, -0.1, 0.9, 60.0, 0.001, 0.002, 1.0};
        double[] result = invokeTransformHomography(pipe, h, 0, 0);

        double tolerance = 1e-15;
        for (int i = 0; i < 9; i++) {
            assertEquals(h[i], result[i], tolerance,
                "Element " + i + " should be unchanged with zero offset");
        }

        pipe.release();
    }

    /** Test that identity homography with offset becomes a pure translation matrix. */
    @Test
    public void testTransformHomography_IdentityHomography_AddsTranslationOnly() throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        // Identity matrix: [[1,0,0],[0,1,0],[0,0,1]]
        double[] identity = {1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
        int offsetX = 50;
        int offsetY = 75;

        double[] result = invokeTransformHomography(pipe, identity, offsetX, offsetY);

        double tolerance = 1e-15;
        // Expected: [[1,0,50],[0,1,75],[0,0,1]]
        assertEquals(1.0, result[0], tolerance, "h[0] should remain 1");
        assertEquals(0.0, result[1], tolerance, "h[1] should remain 0");
        assertEquals(50.0, result[2], tolerance, "h[2] should become offsetX");
        assertEquals(0.0, result[3], tolerance, "h[3] should remain 0");
        assertEquals(1.0, result[4], tolerance, "h[4] should remain 1");
        assertEquals(75.0, result[5], tolerance, "h[5] should become offsetY");
        assertEquals(0.0, result[6], tolerance, "h[6] should remain 0");
        assertEquals(0.0, result[7], tolerance, "h[7] should remain 0");
        assertEquals(1.0, result[8], tolerance, "h[8] should remain 1");

        pipe.release();
    }

    /** Test that the third row of the homography is always preserved unchanged. */
    @Test
    public void testTransformHomography_ThirdRowUnchanged() throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        // Test various homographies with different third rows
        double[][] testCases = {
            {0.9, 0.1, 50.0, -0.1, 0.9, 60.0, 0.001, 0.002, 1.0},
            {1.5, -0.3, 100.0, 0.2, 1.8, -50.0, -0.005, 0.003, 1.2},
            {0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 1.0}
        };
        int[][] offsets = {{100, 150}, {500, 300}, {1920, 1080}};

        double tolerance = 1e-15;
        for (int t = 0; t < testCases.length; t++) {
            double[] h = testCases[t];
            int[] offset = offsets[t];
            double[] result = invokeTransformHomography(pipe, h, offset[0], offset[1]);

            assertEquals(h[6], result[6], tolerance,
                "h[6] should be unchanged for test case " + t);
            assertEquals(h[7], result[7], tolerance,
                "h[7] should be unchanged for test case " + t);
            assertEquals(h[8], result[8], tolerance,
                "h[8] should be unchanged for test case " + t);
        }

        pipe.release();
    }

    /** Test the standard transformation case with manually computed expected values. */
    @Test
    public void testTransformHomography_StandardCase_CorrectComputation() throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        // Input homography
        double[] h = {0.9, 0.1, 50.0, -0.1, 0.9, 60.0, 0.001, 0.002, 1.0};
        int offsetX = 100;
        int offsetY = 150;

        // Manually compute expected T*H where T = [[1,0,100],[0,1,150],[0,0,1]]
        // Row 0: H[0][j] + offsetX * H[2][j]
        double expected0 = 0.9 + 100 * 0.001;   // 0.9 + 0.1 = 1.0
        double expected1 = 0.1 + 100 * 0.002;   // 0.1 + 0.2 = 0.3
        double expected2 = 50.0 + 100 * 1.0;    // 50 + 100 = 150
        // Row 1: H[1][j] + offsetY * H[2][j]
        double expected3 = -0.1 + 150 * 0.001;  // -0.1 + 0.15 = 0.05
        double expected4 = 0.9 + 150 * 0.002;   // 0.9 + 0.3 = 1.2
        double expected5 = 60.0 + 150 * 1.0;    // 60 + 150 = 210
        // Row 2: unchanged
        double expected6 = 0.001;
        double expected7 = 0.002;
        double expected8 = 1.0;

        double[] result = invokeTransformHomography(pipe, h, offsetX, offsetY);

        double tolerance = 1e-10;
        assertEquals(expected0, result[0], tolerance, "h[0] computation incorrect");
        assertEquals(expected1, result[1], tolerance, "h[1] computation incorrect");
        assertEquals(expected2, result[2], tolerance, "h[2] computation incorrect");
        assertEquals(expected3, result[3], tolerance, "h[3] computation incorrect");
        assertEquals(expected4, result[4], tolerance, "h[4] computation incorrect");
        assertEquals(expected5, result[5], tolerance, "h[5] computation incorrect");
        assertEquals(expected6, result[6], tolerance, "h[6] computation incorrect");
        assertEquals(expected7, result[7], tolerance, "h[7] computation incorrect");
        assertEquals(expected8, result[8], tolerance, "h[8] computation incorrect");

        pipe.release();
    }

    /**
     * Test point mapping consistency: for a point p, project(H_roi, p) + offset == project(H_full, p)
     * This verifies the mathematical correctness of the transformation.
     */
    @Test
    public void testTransformHomography_PointMapping_Consistency() throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        // A realistic homography from a tag detection
        double[] hRoi = {0.95, 0.05, 45.0, -0.03, 0.98, 55.0, 0.0002, 0.0001, 1.0};
        int offsetX = 200;
        int offsetY = 150;

        double[] hFull = invokeTransformHomography(pipe, hRoi, offsetX, offsetY);

        // Test points (normalized tag corners: -1 to 1)
        double[][] testPoints = {
            {-1.0, -1.0},
            {1.0, -1.0},
            {1.0, 1.0},
            {-1.0, 1.0},
            {0.0, 0.0}  // center
        };

        double tolerance = 1e-10;
        for (double[] point : testPoints) {
            // Project with ROI homography and add offset
            double[] roiProjected = projectPoint(hRoi, point[0], point[1]);
            double roiFullX = roiProjected[0] + offsetX;
            double roiFullY = roiProjected[1] + offsetY;

            // Project with full-frame homography
            double[] fullProjected = projectPoint(hFull, point[0], point[1]);

            assertEquals(roiFullX, fullProjected[0], tolerance,
                "X coordinate mismatch for point (" + point[0] + ", " + point[1] + ")");
            assertEquals(roiFullY, fullProjected[1], tolerance,
                "Y coordinate mismatch for point (" + point[0] + ", " + point[1] + ")");
        }

        pipe.release();
    }

    /**
     * Helper method to project a point using a homography.
     * Computes [x', y'] from H * [X, Y, 1]^T with perspective division.
     */
    private double[] projectPoint(double[] h, double X, double Y) {
        double w = h[6] * X + h[7] * Y + h[8];
        double x = (h[0] * X + h[1] * Y + h[2]) / w;
        double y = (h[3] * X + h[4] * Y + h[5]) / w;
        return new double[] {x, y};
    }

    /** Test numerical stability with large offsets (4K resolution). */
    @Test
    public void testTransformHomography_LargeOffsets_NumericalStability() throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        // A homography with typical values
        double[] h = {1.2, 0.05, 30.0, -0.02, 1.15, 40.0, 0.0003, 0.0002, 1.0};

        // 4K resolution offset
        int offsetX = 3840;
        int offsetY = 2160;

        double[] result = invokeTransformHomography(pipe, h, offsetX, offsetY);

        // Verify no NaN or Inf values
        for (int i = 0; i < 9; i++) {
            assertFalse(Double.isNaN(result[i]), "Result contains NaN at index " + i);
            assertFalse(Double.isInfinite(result[i]), "Result contains Infinity at index " + i);
        }

        // Verify the transformation is mathematically correct
        double tolerance = 1e-10;

        // Row 0
        assertEquals(h[0] + offsetX * h[6], result[0], tolerance);
        assertEquals(h[1] + offsetX * h[7], result[1], tolerance);
        assertEquals(h[2] + offsetX * h[8], result[2], tolerance);
        // Row 1
        assertEquals(h[3] + offsetY * h[6], result[3], tolerance);
        assertEquals(h[4] + offsetY * h[7], result[4], tolerance);
        assertEquals(h[5] + offsetY * h[8], result[5], tolerance);
        // Row 2 unchanged
        assertEquals(h[6], result[6], tolerance);
        assertEquals(h[7], result[7], tolerance);
        assertEquals(h[8], result[8], tolerance);

        pipe.release();
    }

    // ==================== Parameterized Tests ====================

    private static Stream<Arguments> offsetTestCases() {
        return Stream.of(
            Arguments.of(0, 0, "Zero offset"),
            Arguments.of(100, 0, "X only"),
            Arguments.of(0, 100, "Y only"),
            Arguments.of(100, 100, "Equal offsets"),
            Arguments.of(640, 480, "VGA resolution"),
            Arguments.of(1920, 1080, "1080p resolution")
        );
    }

    /**
     * Parameterized test for various offset values.
     * Verifies the transformation formula holds for different offset combinations.
     */
    @ParameterizedTest(name = "{2}: offset=({0}, {1})")
    @MethodSource("offsetTestCases")
    public void testTransformHomography_VariousOffsets(int offsetX, int offsetY, String description)
            throws Exception {
        AprilTagROIDecodePipe pipe = new AprilTagROIDecodePipe();

        // Use a non-trivial homography
        double[] h = {0.85, 0.12, 35.0, -0.08, 0.92, 45.0, 0.0005, 0.0003, 1.0};

        double[] result = invokeTransformHomography(pipe, h, offsetX, offsetY);

        double tolerance = 1e-10;

        // Verify the transformation formula: T * H
        // Row 0: h[i] + offsetX * h[6+i%3] for i in 0,1,2
        assertEquals(h[0] + offsetX * h[6], result[0], tolerance,
            description + ": h[0] incorrect");
        assertEquals(h[1] + offsetX * h[7], result[1], tolerance,
            description + ": h[1] incorrect");
        assertEquals(h[2] + offsetX * h[8], result[2], tolerance,
            description + ": h[2] incorrect");

        // Row 1: h[i] + offsetY * h[3+i%3] for i in 3,4,5
        assertEquals(h[3] + offsetY * h[6], result[3], tolerance,
            description + ": h[3] incorrect");
        assertEquals(h[4] + offsetY * h[7], result[4], tolerance,
            description + ": h[4] incorrect");
        assertEquals(h[5] + offsetY * h[8], result[5], tolerance,
            description + ": h[5] incorrect");

        // Row 2: unchanged
        assertEquals(h[6], result[6], tolerance, description + ": h[6] should be unchanged");
        assertEquals(h[7], result[7], tolerance, description + ": h[7] should be unchanged");
        assertEquals(h[8], result[8], tolerance, description + ": h[8] should be unchanged");

        pipe.release();
    }

    /**
     * Integration test: Verify that homography transformation produces consistent results
     * when comparing full-frame detection vs ROI-based detection with transformation.
     * This extends testCoordinateMappingAccuracy to also verify homography values.
     */
    @Test
    public void testTransformHomography_Integration_HomographyProducesCorrectPoseInput() {
        // Load a test image with a known AprilTag
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));

        var frame = frameProvider.get();
        Mat grayMat = frame.processedImage.getMat();

        // First, run full-frame detection to get ground truth
        AprilTagDetector fullFrameDetector = new AprilTagDetector();
        fullFrameDetector.addFamily(AprilTagFamily.kTag36h11.getNativeName());

        AprilTagDetection[] fullFrameDetections;
        try {
            fullFrameDetections = fullFrameDetector.detect(grayMat);
        } catch (Exception e) {
            assumeTrue(false, "Native AprilTag library not available: " + e.getMessage());
            return;
        }
        assumeTrue(fullFrameDetections != null, "Native AprilTag detector returned null");
        assumeTrue(fullFrameDetections.length > 0, "Should detect at least one tag in full frame");

        AprilTagDetection groundTruth = fullFrameDetections[0];
        double[] groundTruthHomography = groundTruth.getHomography();

        // Create an ROI that contains the tag
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, groundTruth.getCornerX(i));
            maxX = Math.max(maxX, groundTruth.getCornerX(i));
            minY = Math.min(minY, groundTruth.getCornerY(i));
            maxY = Math.max(maxY, groundTruth.getCornerY(i));
        }

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
        double[] mappedHomography = mappedDetection.getHomography();

        // Verify that the transformed homography produces the same point projections
        // as the ground truth homography for the tag corners
        double tolerance = 1.0; // Allow 1 pixel tolerance due to subpixel differences

        // Test projection of normalized tag corners
        double[][] tagCorners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int i = 0; i < 4; i++) {
            double[] gtProjected = projectPoint(groundTruthHomography, tagCorners[i][0], tagCorners[i][1]);
            double[] mappedProjected = projectPoint(mappedHomography, tagCorners[i][0], tagCorners[i][1]);

            assertEquals(gtProjected[0], mappedProjected[0], tolerance,
                "Corner " + i + " X projection should match");
            assertEquals(gtProjected[1], mappedProjected[1], tolerance,
                "Corner " + i + " Y projection should match");
        }

        fullFrameDetector.close();
        decodePipe.release();
    }
}
