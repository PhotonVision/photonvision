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
                Imgproc.putText(image, label, labelOrg, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, boxColor, 2);

                System.out.println(
                        String.format(
                                "Detected algae at (%.2f, %.2f) with confidence %.2f",
                                result.getX(), result.getY(), result.getConfidence()));

                CoreMLTestUtils.saveDebugImage(image, "algae2_detection_result.jpg");
            }

            image.release();
        }
    }

    @Test
    public void testDetectRawCorrectness() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");

            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");
            assertNotNull(image, "Test image should be loaded");
            assertFalse(image.empty(), "Test image should not be empty");

            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                int pixelFormat = ImageUtils.getPixelFormat(image);
                double boxThreshold = 0.5;
                double nmsThreshold = 0.5;

                // Test both detect() and detectRaw() methods
                DetectionResultArray visionResults =
                        detector.detect(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                boxThreshold,
                                nmsThreshold,
                                frameArena);

                DetectionResultArray rawResults =
                        detector.detectRaw(
                                imageData,
                                (long) image.width(),
                                (long) image.height(),
                                pixelFormat,
                                boxThreshold,
                                nmsThreshold,
                                frameArena);

                assertNotNull(visionResults, "Vision detection results should not be null");
                assertNotNull(rawResults, "Raw detection results should not be null");

                System.out.println("Vision framework results: " + visionResults.count() + " detections");
                System.out.println("Raw MLModel results: " + rawResults.count() + " detections");

                // Both methods should detect objects (though counts may differ slightly due to
                // implementation)
                assertTrue(visionResults.count() > 0, "Vision framework should detect objects");
                assertTrue(rawResults.count() > 0, "Raw MLModel should detect objects");

                // Verify all raw results are valid
                for (int i = 0; i < rawResults.count(); i++) {
                    DetectionResult result = rawResults.get(i, frameArena);

                    assertTrue(
                            result.getConfidence() > 0 && result.getConfidence() <= 1.0,
                            "Confidence should be between 0 and 1");
                    assertTrue(result.getClassId() >= -1, "Class ID should be >= -1");
                    assertTrue(
                            result.getWidth() > 0 && result.getWidth() <= 1.0,
                            "Detection box should have positive width and normalized dimensions");
                    assertTrue(
                            result.getHeight() > 0 && result.getHeight() <= 1.0,
                            "Detection box should have positive height and normalized dimensions");
                }
            }

            image.release();
        }
    }

    @Test
    public void testDetectRawVsVisionPerformance() {
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        System.out.println("=".repeat(80));
        System.out.println("PERFORMANCE COMPARISON: Vision Framework vs Raw MLModel");
        System.out.println("=".repeat(80));

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");

            System.out.println(
                    "Test image: "
                            + image.width()
                            + "x"
                            + image.height()
                            + " ("
                            + image.channels()
                            + " channels)");
            System.out.println();

            int pixelFormat = ImageUtils.getPixelFormat(image);
            double boxThreshold = 0.5;
            double nmsThreshold = 0.5;
            int warmupIterations = 20;
            int benchmarkIterations = 50;

            // ===== VISION FRAMEWORK WARMUP =====
            System.out.println("Vision Framework - Warmup (" + warmupIterations + " iterations)...");
            for (int i = 0; i < warmupIterations; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                    detector.detect(
                            imageData,
                            (long) image.width(),
                            (long) image.height(),
                            pixelFormat,
                            boxThreshold,
                            nmsThreshold,
                            frameArena);
                }
            }

            // ===== VISION FRAMEWORK BENCHMARK =====
            System.out.println(
                    "Vision Framework - Benchmark (" + benchmarkIterations + " iterations)...");
            long[] visionTimes = new long[benchmarkIterations];
            for (int i = 0; i < benchmarkIterations; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    long startTime = System.nanoTime();
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                    detector.detect(
                            imageData,
                            (long) image.width(),
                            (long) image.height(),
                            pixelFormat,
                            boxThreshold,
                            nmsThreshold,
                            frameArena);
                    visionTimes[i] = System.nanoTime() - startTime;
                }
            }

            // ===== RAW MLMODEL WARMUP =====
            System.out.println();
            System.out.println("Raw MLModel - Warmup (" + warmupIterations + " iterations)...");
            for (int i = 0; i < warmupIterations; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                    detector.detectRaw(
                            imageData,
                            (long) image.width(),
                            (long) image.height(),
                            pixelFormat,
                            boxThreshold,
                            nmsThreshold,
                            frameArena);
                }
            }

            // ===== RAW MLMODEL BENCHMARK =====
            System.out.println("Raw MLModel - Benchmark (" + benchmarkIterations + " iterations)...");
            long[] rawTimes = new long[benchmarkIterations];
            for (int i = 0; i < benchmarkIterations; i++) {
                try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                    long startTime = System.nanoTime();
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);
                    detector.detectRaw(
                            imageData,
                            (long) image.width(),
                            (long) image.height(),
                            pixelFormat,
                            boxThreshold,
                            nmsThreshold,
                            frameArena);
                    rawTimes[i] = System.nanoTime() - startTime;
                }
            }

            // ===== CALCULATE STATISTICS =====
            double visionAvg = calculateAverage(visionTimes);
            double visionMin = calculateMin(visionTimes);
            double visionMax = calculateMax(visionTimes);
            double visionStdDev = calculateStdDev(visionTimes, visionAvg);

            double rawAvg = calculateAverage(rawTimes);
            double rawMin = calculateMin(rawTimes);
            double rawMax = calculateMax(rawTimes);
            double rawStdDev = calculateStdDev(rawTimes, rawAvg);

            double speedup = visionAvg / rawAvg;

            // ===== PRINT RESULTS =====
            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("BENCHMARK RESULTS");
            System.out.println("=".repeat(80));
            System.out.println();
            System.out.println("Vision Framework (VNCoreMLRequest):");
            System.out.println(String.format("  Average:   %.3f ms", visionAvg));
            System.out.println(String.format("  Minimum:   %.3f ms", visionMin));
            System.out.println(String.format("  Maximum:   %.3f ms", visionMax));
            System.out.println(String.format("  Std Dev:   %.3f ms", visionStdDev));
            System.out.println(String.format("  Avg FPS:   %.1f", 1000.0 / visionAvg));
            System.out.println();
            System.out.println("Raw MLModel (direct prediction):");
            System.out.println(String.format("  Average:   %.3f ms", rawAvg));
            System.out.println(String.format("  Minimum:   %.3f ms", rawMin));
            System.out.println(String.format("  Maximum:   %.3f ms", rawMax));
            System.out.println(String.format("  Std Dev:   %.3f ms", rawStdDev));
            System.out.println(String.format("  Avg FPS:   %.1f", 1000.0 / rawAvg));
            System.out.println();
            System.out.println(String.format("Speedup: %.2fx", speedup));
            if (speedup > 1.0) {
                System.out.println(
                        String.format(
                                "Raw MLModel is %.1f%% faster than Vision Framework", (speedup - 1.0) * 100));
            } else {
                System.out.println(
                        String.format(
                                "Vision Framework is %.1f%% faster than Raw MLModel", (1.0 / speedup - 1.0) * 100));
            }
            System.out.println("=".repeat(80));

            image.release();
        }
    }

    private double calculateAverage(long[] times) {
        long sum = 0;
        for (long time : times) {
            sum += time;
        }
        return sum / (times.length * 1_000_000.0);
    }

    private double calculateMin(long[] times) {
        long min = Long.MAX_VALUE;
        for (long time : times) {
            min = Math.min(min, time);
        }
        return min / 1_000_000.0;
    }

    private double calculateMax(long[] times) {
        long max = Long.MIN_VALUE;
        for (long time : times) {
            max = Math.max(max, time);
        }
        return max / 1_000_000.0;
    }

    private double calculateStdDev(long[] times, double avg) {
        double sumSquaredDiff = 0;
        for (long time : times) {
            double timeMs = time / 1_000_000.0;
            double diff = timeMs - avg;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / times.length);
    }

    @Test
    public void testYOLO11BenchmarkWith100WarmupIterations() {
        return;
        //     String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        //     System.out.println("=".repeat(80));
        //     System.out.println("YOLO11 BENCHMARK TEST - 100 Warmup Iterations");
        //     System.out.println("=".repeat(80));

        //     try (var arena = AllocatingSwiftArena.ofConfined()) {
        //         ObjectDetector detector = ObjectDetector.init(modelPath, arena);
        //         assertNotNull(detector, "Model creation should return valid detector");

        //         // Load coral test image
        //         Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");
        //         assertNotNull(image, "Test image should be loaded");
        //         assertFalse(image.empty(), "Test image should not be empty");

        //         System.out.println(
        //                 "Loaded test image: "
        //                         + image.width()
        //                         + "x"
        //                         + image.height()
        //                         + " ("
        //                         + image.channels()
        //                         + " channels)");
        //         System.out.println();

        //         int pixelFormat = ImageUtils.getPixelFormat(image);
        //         double boxThreshold = 0.5;
        //         double nmsThreshold = 0.5;

        //         // Warmup phase - 100 iterations
        //         System.out.println("Starting warmup phase: 100 iterations...");
        //         long warmupStartTime = System.nanoTime();

        //         for (int i = 0; i < 100; i++) {
        //             try (var frameArena = AllocatingSwiftArena.ofConfined()) {
        //                 MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

        //                 DetectionResultArray results =
        //                         detector.detect(
        //                                 imageData,
        //                                 (long) image.width(),
        //                                 (long) image.height(),
        //                                 pixelFormat,
        //                                 boxThreshold,
        //                                 nmsThreshold,
        //                                 frameArena);

        //                 assertNotNull(results, "Detection results should not be null");

        //                 // Print progress every 10 iterations
        //                 if ((i + 1) % 10 == 0) {
        //                     System.out.println("  Completed " + (i + 1) + "/100 warmup iterations");
        //                 }
        //             }
        //         }

        //         long warmupEndTime = System.nanoTime();
        //         double warmupTotalMs = (warmupEndTime - warmupStartTime) / 1_000_000.0;
        //         double warmupAvgMs = warmupTotalMs / 100.0;

        //         System.out.println();
        //         System.out.println("Warmup phase completed!");
        //         System.out.println(
        //                 String.format(
        //                         "  Total warmup time: %.2f ms (%.2f seconds)",
        //                         warmupTotalMs, warmupTotalMs / 1000.0));
        //         System.out.println(String.format("  Average per iteration: %.3f ms", warmupAvgMs));
        //         System.out.println(String.format("  Estimated FPS: %.1f", 1000.0 / warmupAvgMs));
        //         System.out.println();

        //         // Benchmark phase - 50 measured iterations after warmup
        //         System.out.println("Starting benchmark phase: 50 measured iterations...");
        //         long[] iterationTimes = new long[50];

        //         for (int i = 0; i < 50; i++) {
        //             try (var frameArena = AllocatingSwiftArena.ofConfined()) {
        //                 long iterStartTime = System.nanoTime();

        //                 MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

        //                 DetectionResultArray results =
        //                         detector.detect(
        //                                 imageData,
        //                                 (long) image.width(),
        //                                 (long) image.height(),
        //                                 pixelFormat,
        //                                 boxThreshold,
        //                                 nmsThreshold,
        //                                 frameArena);

        //                 long iterEndTime = System.nanoTime();
        //                 iterationTimes[i] = iterEndTime - iterStartTime;

        //                 assertNotNull(results, "Detection results should not be null");
        //             }
        //         }

        //         // Calculate statistics
        //         long totalTime = 0;
        //         long minTime = Long.MAX_VALUE;
        //         long maxTime = Long.MIN_VALUE;

        //         for (long time : iterationTimes) {
        //             totalTime += time;
        //             minTime = Math.min(minTime, time);
        //             maxTime = Math.max(maxTime, time);
        //         }

        //         double avgTimeMs = totalTime / (50.0 * 1_000_000.0);
        //         double minTimeMs = minTime / 1_000_000.0;
        //         double maxTimeMs = maxTime / 1_000_000.0;

        //         // Calculate standard deviation
        //         double sumSquaredDiff = 0;
        //         for (long time : iterationTimes) {
        //             double timeMs = time / 1_000_000.0;
        //             double diff = timeMs - avgTimeMs;
        //             sumSquaredDiff += diff * diff;
        //         }
        //         double stdDevMs = Math.sqrt(sumSquaredDiff / 50.0);

        //         System.out.println();
        //         System.out.println("Benchmark phase completed!");
        //         System.out.println("=".repeat(80));
        //         System.out.println("BENCHMARK RESULTS");
        //         System.out.println("=".repeat(80));
        //         System.out.println(String.format("  Average time:       %.3f ms", avgTimeMs));
        //         System.out.println(String.format("  Minimum time:       %.3f ms", minTimeMs));
        //         System.out.println(String.format("  Maximum time:       %.3f ms", maxTimeMs));
        //         System.out.println(String.format("  Standard deviation: %.3f ms", stdDevMs));
        //         System.out.println(String.format("  Average FPS:        %.1f", 1000.0 / avgTimeMs));
        //         System.out.println(String.format("  Peak FPS:           %.1f", 1000.0 / minTimeMs));
        //         System.out.println("=".repeat(80));
        //         System.out.println();

        //         // Verify performance is reasonable (should be faster than 1000ms per detection)
        //         assertTrue(
        //                 avgTimeMs < 1000,
        //                 "Average detection time should be less than 1000ms, was: " + avgTimeMs +
        // "ms");

        //         // Run one final detection to get actual results for verification
        //         try (var frameArena = AllocatingSwiftArena.ofConfined()) {
        //             MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

        //             DetectionResultArray results =
        //                     detector.detect(
        //                             imageData,
        //                             (long) image.width(),
        //                             (long) image.height(),
        //                             pixelFormat,
        //                             boxThreshold,
        //                             nmsThreshold,
        //                             frameArena);

        //             System.out.println("Final detection result: " + results.count() + " objects
        // detected");
        //         }

        //         image.release();
        //     }

        //     System.out.println("Benchmark test completed successfully!");
        //     System.out.println("=".repeat(80));
    }
}
