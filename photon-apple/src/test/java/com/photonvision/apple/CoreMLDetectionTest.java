package com.photonvision.apple;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

@EnabledOnOs(OS.MAC)
public class CoreMLDetectionTest {

    @BeforeAll
    public static void setup() {
        CoreMLTestUtils.initializeLibraries();
    }

    @Test
    public void testCoralDetection() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");

            // Test coral detection
            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");
            assertNotNull(image, "Test image should be loaded");
            assertFalse(image.empty(), "Test image should not be empty");

            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                int pixelFormat = ImageUtils.getPixelFormat(image);

                DetectionResultArray results =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                0.5,
                                0.5,
                                frameArena);

                assertNotNull(results, "Detection results should not be null");
                assertTrue(results.count() > 0, "Should detect coral in the image");

                // Verify detection results
                for (int i = 0; i < results.count(); i++) {
                    DetectionResult result = results.get(i, frameArena);

                    assertTrue(
                            result.getConfidence() > 0 && result.getConfidence() <= 1.0,
                            "Confidence should be between 0 and 1");
                    // Class ID can be -1 for single-class models or background
                    assertTrue(result.getClassId() >= -1, "Class ID should be >= -1");
                    assertTrue(
                            result.getWidth() > 0 && result.getWidth() <= 1.0,
                            "Detection box should have positive width and normalized dimensions");
                    assertTrue(
                            result.getHeight() > 0 && result.getHeight() <= 1.0,
                            "Detection box should have positive height and normalized dimensions");

                    // Convert normalized coordinates to pixel coordinates
                    int x = (int) (result.getX() * image.width());
                    int y = (int) (result.getY() * image.height());
                    int width = (int) (result.getWidth() * image.width());
                    int height = (int) (result.getHeight() * image.height());

                    // Draw detection results for debugging
                    Scalar boxColor = new Scalar(255, 0, 0);
                    Point pt1 = new Point(x, y);
                    Point pt2 = new Point(x + width, y + height);
                    Imgproc.rectangle(image, pt1, pt2, boxColor, 2);

                    String label = String.format("Coral: %.2f", result.getConfidence());
                    Point labelOrg = new Point(x, y - 10);
                    Imgproc.putText(image, label, labelOrg, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, boxColor, 2);
                }

                CoreMLTestUtils.saveDebugImage(image, "coral_detection_result.jpg");
            }

            image.release();
        }
    }

    @Test
    public void testAlgaeDetection() {
        String modelPath = CoreMLTestUtils.loadTestModel("algae-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");

            // Test algae detection
            Mat image = CoreMLTestUtils.loadTestImage("algae.jpeg");
            assertNotNull(image, "Test image should be loaded");
            assertFalse(image.empty(), "Test image should not be empty");

            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                int pixelFormat = ImageUtils.getPixelFormat(image);

                DetectionResultArray results =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                0.5,
                                0.5,
                                frameArena);

                assertNotNull(results, "Detection results should not be null");
                // Note: algae model may not detect anything in this image depending on training data
                // Just verify it runs without error
                System.out.println("Algae detection count: " + results.count());

                // Verify detection results if any
                for (int i = 0; i < results.count(); i++) {
                    DetectionResult result = results.get(i, frameArena);

                    assertTrue(
                            result.getConfidence() > 0 && result.getConfidence() <= 1.0,
                            "Confidence should be between 0 and 1");
                    // Class ID can be -1 for single-class models or background
                    assertTrue(result.getClassId() >= -1, "Class ID should be >= -1");
                    assertTrue(
                            result.getWidth() > 0 && result.getWidth() <= 1.0,
                            "Detection box should have positive width and normalized dimensions");
                    assertTrue(
                            result.getHeight() > 0 && result.getHeight() <= 1.0,
                            "Detection box should have positive height and normalized dimensions");

                    // Convert normalized coordinates to pixel coordinates
                    int x = (int) (result.getX() * image.width());
                    int y = (int) (result.getY() * image.height());
                    int width = (int) (result.getWidth() * image.width());
                    int height = (int) (result.getHeight() * image.height());

                    // Draw detection results for debugging
                    Scalar boxColor = new Scalar(255, 0, 0);
                    Point pt1 = new Point(x, y);
                    Point pt2 = new Point(x + width, y + height);
                    Imgproc.rectangle(image, pt1, pt2, boxColor, 2);

                    String label = String.format("Algae: %.2f", result.getConfidence());
                    Point labelOrg = new Point(x, y - 10);
                    Imgproc.putText(image, label, labelOrg, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, boxColor, 2);
                }

                CoreMLTestUtils.saveDebugImage(image, "algae_detection_result.jpg");
            }

            image.release();
        }
    }

    @Test
    public void testDetectionPerformance() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");

            // Warm up
            try (var warmupArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(image, warmupArena);
                detector.detect(
                        imageData,
                        (long) image.width(),
                        (long) image.height(),
                        ImageUtils.getPixelFormat(image),
                        0.5,
                        0.5,
                        warmupArena);
            }

            // Test detection performance
            int numIterations = 10;
            long startTime = System.nanoTime();

            for (int i = 0; i < numIterations; i++) {
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

                    assertNotNull(results, "Detection results should not be null");
                }
            }

            long endTime = System.nanoTime();
            double avgTimeMs = (endTime - startTime) / (numIterations * 1_000_000.0);

            System.out.println("Average detection time: " + avgTimeMs + " ms");
            assertTrue(avgTimeMs < 1000, "Average detection time should be less than 1000ms");

            image.release();
        }
    }

    @Test
    public void testDifferentConfidenceThresholds() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");

            double[] confidenceThresholds = {0.1, 0.3, 0.5, 0.7, 0.9};
            int[] detectionCounts = new int[confidenceThresholds.length];

            for (int i = 0; i < confidenceThresholds.length; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

                    DetectionResultArray results =
                            detector.detect(
                                    imageData,
                                    (long) image.width(),
                                    (long) image.height(),
                                    ImageUtils.getPixelFormat(image),
                                    0.5,
                                    confidenceThresholds[i],
                                    frameArena);

                    detectionCounts[i] = (int) results.count();

                    // Higher confidence threshold should result in fewer detections
                    if (i > 0) {
                        assertTrue(
                                detectionCounts[i] <= detectionCounts[i - 1],
                                "Higher confidence threshold should result in fewer or equal detections");
                    }
                }
            }

            image.release();
        }
    }

    @Test
    public void testDifferentNMSThresholds() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");

            double[] nmsThresholds = {0.1, 0.3, 0.5, 0.7, 0.9};
            int[] detectionCounts = new int[nmsThresholds.length];

            for (int i = 0; i < nmsThresholds.length; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

                    DetectionResultArray results =
                            detector.detect(
                                    imageData,
                                    (long) image.width(),
                                    (long) image.height(),
                                    ImageUtils.getPixelFormat(image),
                                    nmsThresholds[i],
                                    0.5,
                                    frameArena);

                    detectionCounts[i] = (int) results.count();
                    System.out.println(
                            "NMS threshold " + nmsThresholds[i] + ": " + detectionCounts[i] + " detections");
                }
            }

            image.release();
        }
    }

    @Test
    public void testAlgae2Detection() {
        String modelPath = CoreMLTestUtils.loadTestModel("algae-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");

            // Load algae2.jpg test image
            Mat image = CoreMLTestUtils.loadTestImage("algae2.jpg");
            assertNotNull(image, "Test image should be loaded");
            assertFalse(image.empty(), "Test image should not be empty");

            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                int pixelFormat = ImageUtils.getPixelFormat(image);

                DetectionResultArray results =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                0.5,
                                0.5,
                                frameArena);

                assertNotNull(results, "Detection results should not be null");
                assertEquals(1, results.count(), "Should detect exactly 1 algae object in the image");

                // Verify the detection result
                DetectionResult result = results.get(0, frameArena);

                // For single-class model, class ID can be -1 or 0 (algae)
                assertTrue(
                        result.getClassId() == -1 || result.getClassId() == 0,
                        "Detected object should be algae (class ID -1 or 0 for single-class model)");

                assertTrue(
                        result.getConfidence() > 0 && result.getConfidence() <= 1.0,
                        "Confidence should be between 0 and 1");
                assertTrue(
                        result.getWidth() > 0 && result.getWidth() <= 1.0,
                        "Detection box should have positive width and normalized dimensions");
                assertTrue(
                        result.getHeight() > 0 && result.getHeight() <= 1.0,
                        "Detection box should have positive height and normalized dimensions");

                // Convert normalized coordinates to pixel coordinates for debugging
                int x = (int) (result.getX() * image.width());
                int y = (int) (result.getY() * image.height());
                int width = (int) (result.getWidth() * image.width());
                int height = (int) (result.getHeight() * image.height());

                // Draw detection results for debugging
                Scalar boxColor = new Scalar(0, 255, 0); // Green box for algae
                Point pt1 = new Point(x, y);
                Point pt2 = new Point(x + width, y + height);
                Imgproc.rectangle(image, pt1, pt2, boxColor, 2);

                String label = String.format("Algae: %.2f", result.getConfidence());
                Point labelOrg = new Point(x, y - 10);
                Imgproc.putText(
                        image, label, labelOrg, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, boxColor, 2);

                System.out.println(
                        String.format(
                                "Detected algae at (%.2f, %.2f) with confidence %.2f",
                                result.getX(), result.getY(), result.getConfidence()));

                CoreMLTestUtils.saveDebugImage(image, "algae2_detection_result.jpg");
            }

            image.release();
        }
    }
}
