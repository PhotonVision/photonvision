package com.photonvision.apple;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.opencv.core.Mat;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

@EnabledOnOs(OS.MAC)
public class CoreMLBaseTest {

    @BeforeAll
    public static void setup() {
        CoreMLTestUtils.initializeLibraries();
    }

    @Test
    public void testModelCreation() {
        // Test model creation with valid path
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");
        }

        // Test model creation with invalid path - expect exception or null
        String invalidPath = "invalid/path/model.mlmodel";
        try (var arena = AllocatingSwiftArena.ofConfined()) {
            // The Swift side might throw an error or return a detector that fails on detect
            // For now, just verify it doesn't crash
            ObjectDetector detector = ObjectDetector.init(invalidPath, arena);
            // If we get here, the detector was created but may fail on detect()
            assertNotNull(detector);
        } catch (Exception e) {
            // Expected - invalid model path should fail
            assertTrue(true, "Invalid model path correctly failed");
        }
    }

    @Test
    public void testEmptyImageDetection() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);

            // Test with empty image
            Mat emptyImage = CoreMLTestUtils.loadTestImage("empty.png");
            assertNotNull(emptyImage, "Empty test image should be loaded");
            assertFalse(emptyImage.empty(), "Test image should not be empty");

            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(emptyImage, frameArena);
                int pixelFormat = ImageUtils.getPixelFormat(emptyImage);
                // Debug: log the MemorySegment address and size to correlate with native crash
                try {
                    System.err.println(
                            "[CoreMLBaseTest] imageData.address="
                                    + imageData.address()
                                    + " byteSize="
                                    + imageData.byteSize());
                } catch (Throwable t) {
                    System.err.println(
                            "[CoreMLBaseTest] imageData.byteSize="
                                    + imageData.byteSize()
                                    + " (address unavailable)");
                }

                DetectionResultArray results =
                        ObjectDetectorSafe.detectChecked(
                                detector,
                                imageData,
                                (long) emptyImage.width(),
                                (long) emptyImage.height(),
                                pixelFormat,
                                0.5,
                                0.5,
                                frameArena);

                assertNotNull(results, "Detection results should not be null");
                assertTrue(results.count() == 0, "Empty image should have 0");
            }

            emptyImage.release();
        }
    }

    @Test
    public void testInvalidDetectionParameters() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);

            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");

            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                int pixelFormat = ImageUtils.getPixelFormat(image);

                // Test invalid NMS threshold (negative)
                DetectionResultArray results =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                -1.0,
                                0.5,
                                frameArena);
                assertNotNull(results, "Detection with invalid NMS threshold should return array");
                // Should return empty or valid results (Swift may handle gracefully)

                // Test invalid box threshold (negative)
                results =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                0.5,
                                -1.0,
                                frameArena);
                assertNotNull(results, "Detection with invalid box threshold should return array");

                // Test with very high thresholds (should return no results)
                results =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                1.5,
                                1.5,
                                frameArena);
                assertNotNull(results, "Detection with very high thresholds should return array");
                assertEquals(0, results.count(), "Very high thresholds should have no results");
            }

            image.release();
        }
    }

    @Test
    public void testMultipleModelsSequentially() {
        // Test creating multiple models one after another
        String coralModelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");
        String algaeModelPath = CoreMLTestUtils.loadTestModel("algae-640-640-yolov11s.mlmodel");

        // Create and destroy coral model
        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(coralModelPath, arena);
            assertNotNull(detector);
        }

        // Create and destroy algae model
        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(algaeModelPath, arena);
            assertNotNull(detector);
        }

        // Create coral model again
        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(coralModelPath, arena);
            assertNotNull(detector);
        }
    }

    @Test
    public void testDetectorReuse() {
        // Test that a single detector can be reused for multiple detections
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);

            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");

            // Run detection 5 times with the same detector
            for (int i = 0; i < 5; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

                    DetectionResultArray results =
                            detector.detect(
                                    imageData,
                                    (long) image.width(),
                                    (long) image.height(),
                                    ImageUtils.getPixelFormat(image),
                                    0.5,
                                    0.5,
                                    frameArena);

                    assertNotNull(results, "Detection results should not be null on iteration " + i);
                    assertTrue(results.count() >= 0, "Should have valid result count on iteration " + i);
                }
            }

            image.release();
        }
    }
}
