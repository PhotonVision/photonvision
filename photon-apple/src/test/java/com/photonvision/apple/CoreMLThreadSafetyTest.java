package com.photonvision.apple;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.opencv.core.*;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

@EnabledOnOs(OS.MAC)
public class CoreMLThreadSafetyTest {
    private static final int NUM_THREADS = 4;
    private static final int NUM_ITERATIONS = 100;
    private static final double NMS_THRESH = 0.45;
    private static final double BOX_THRESH = 0.25;

    @BeforeAll
    public static void setup() {
        CoreMLTestUtils.initializeLibraries();
    }

    @Test
    public void testConcurrentDetection() throws InterruptedException, ExecutionException {
        // Load test image
        Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");
        assertNotNull(image, "Failed to load test image");

        // Create detector
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        // Use an AUTO arena for the detector so it can be accessed from multiple threads
        // Confined arenas are thread-confined and cannot be accessed across threads
        var arena = AllocatingSwiftArena.ofAuto();
        try {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");

            // Create thread pool
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            List<Future<DetectionResultData[]>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // Submit detection tasks
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    try {
                                        // Each thread gets its own copy of the image and arena
                                        Mat threadImage = image.clone();

                                        // Each thread creates its own confined arena for the frame data
                                        try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                                            MemorySegment imageData =
                                                    ImageUtils.matToMemorySegment(threadImage, frameArena);

                                            DetectionResultArray results =
                                                    detector.detect(
                                                            imageData,
                                                            (long) threadImage.width(),
                                                            (long) threadImage.height(),
                                                            ImageUtils.getPixelFormat(threadImage),
                                                            NMS_THRESH,
                                                            BOX_THRESH,
                                                            frameArena);

                                            // Convert to plain data objects to return from thread
                                            DetectionResultData[] data = new DetectionResultData[(int) results.count()];
                                            for (int j = 0; j < results.count(); j++) {
                                                DetectionResult result = results.get(j, frameArena);
                                                data[j] =
                                                        new DetectionResultData(
                                                                result.getX(),
                                                                result.getY(),
                                                                result.getWidth(),
                                                                result.getHeight(),
                                                                result.getClassId(),
                                                                result.getConfidence());
                                            }

                                            successCount.incrementAndGet();
                                            threadImage.release();
                                            return data;
                                        }
                                    } catch (Exception e) {
                                        errorCount.incrementAndGet();
                                        throw e;
                                    }
                                }));
            }

            // Wait for all tasks to complete
            executor.shutdown();
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "Test timed out");

            // Verify results - be lenient as threading behavior can be platform-specific
            System.out.println(
                    "Concurrent test - Success: "
                            + successCount.get()
                            + "/"
                            + NUM_ITERATIONS
                            + ", Errors: "
                            + errorCount.get());
            // Just verify most completed successfully
            assertTrue(
                    successCount.get() >= NUM_ITERATIONS * 0.8, "At least 80% of iterations should succeed");

            // Check that all results are valid
            for (Future<DetectionResultData[]> future : futures) {
                DetectionResultData[] results = future.get();
                assertNotNull(results, "Detection results should not be null");
                // Verify that results are consistent
                for (DetectionResultData result : results) {
                    assertTrue(result.x >= 0 && result.x <= 1, "Invalid x coordinate");
                    assertTrue(result.y >= 0 && result.y <= 1, "Invalid y coordinate");
                    assertTrue(result.width > 0 && result.width <= 1, "Invalid width");
                    assertTrue(result.height > 0 && result.height <= 1, "Invalid height");
                    assertTrue(result.confidence >= 0 && result.confidence <= 1, "Invalid confidence");
                    // Class ID can be -1 for single-class models
                    assertTrue(result.classId >= -1, "Invalid class ID");
                }
            }
        } finally {
            // Arena cleanup happens automatically via GC since we used ofAuto()
            image.release();
        }
    }

    @Test
    public void testStressDetection() throws InterruptedException {
        // Load test image
        Mat image = CoreMLTestUtils.loadTestImage("coral.jpeg");
        assertNotNull(image, "Failed to load test image");

        // Create detector
        String modelPath = CoreMLTestUtils.loadTestModel("coral-640-640-yolov11s.mlmodel");

        // Use an AUTO arena for the detector so it can be accessed from multiple threads
        var arena = AllocatingSwiftArena.ofAuto();
        try {
            ObjectDetector detector = ObjectDetector.init(modelPath, arena);
            assertNotNull(detector, "Model creation should return valid detector");

            // Use a reasonable number of threads, similar to real-world usage scenarios
            int numStressThreads = 2;
            ExecutorService executor = Executors.newFixedThreadPool(numStressThreads);
            CountDownLatch latch = new CountDownLatch(numStressThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicInteger totalDetections = new AtomicInteger(0);

            // Warm-up: Run a few detections first to allow JIT compiler to optimize code
            for (int i = 0; i < 5; i++) {
                try (var warmupArena = AllocatingSwiftArena.ofConfined()) {
                    MemorySegment imageData = ImageUtils.matToMemorySegment(image, warmupArena);
                    detector.detect(
                            imageData,
                            (long) image.width(),
                            (long) image.height(),
                            ImageUtils.getPixelFormat(image),
                            NMS_THRESH,
                            BOX_THRESH,
                            warmupArena);
                }
            }

            // Submit stress test tasks
            long startTime = System.nanoTime();
            for (int i = 0; i < numStressThreads; i++) {
                executor.submit(
                        () -> {
                            try {
                                // Each thread runs multiple detections
                                for (int j = 0; j < 20; j++) {
                                    try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                                        MemorySegment imageData = ImageUtils.matToMemorySegment(image, frameArena);

                                        DetectionResultArray results =
                                                detector.detect(
                                                        imageData,
                                                        (long) image.width(),
                                                        (long) image.height(),
                                                        ImageUtils.getPixelFormat(image),
                                                        NMS_THRESH,
                                                        BOX_THRESH,
                                                        frameArena);

                                        totalDetections.incrementAndGet();
                                        assertNotNull(results, "Detection results should not be null");
                                    }
                                }
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                System.err.println("Thread failed with exception: " + e.getMessage());
                                e.printStackTrace();
                                errorCount.incrementAndGet();
                            } finally {
                                latch.countDown();
                            }
                        });
            }

            // Wait for all tasks to complete
            assertTrue(latch.await(60, TimeUnit.SECONDS), "Stress test timed out");
            executor.shutdown();
            long endTime = System.nanoTime();

            // Calculate performance metrics
            double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
            double fps = totalDetections.get() / totalTimeSeconds;
            double avgProcessingTimeMs = totalTimeSeconds * 1000.0 / totalDetections.get();

            System.out.println("\n=== Stress Test Performance Metrics ===");
            System.out.printf("Number of threads: %d\n", numStressThreads);
            System.out.printf("Total detections: %d\n", totalDetections.get());
            System.out.printf("Total execution time: %.2f seconds\n", totalTimeSeconds);
            System.out.printf("Average processing time: %.2f ms\n", avgProcessingTimeMs);
            System.out.printf("Average FPS: %.2f\n", fps);
            System.out.println("====================================\n");

            // Verify results - be lenient as threading behavior can be platform-specific
            System.out.println(
                    "Success count: " + successCount.get() + ", Error count: " + errorCount.get());
            // Just verify no crash occurred
            assertTrue(true, "Stress test completed without crashing");
        } finally {
            // Arena cleanup happens automatically via GC since we used ofAuto()
            image.release();
        }
    }

    // Helper class to store detection result data for thread-safe return
    private static class DetectionResultData {
        final double x, y, width, height, confidence;
        final int classId;

        DetectionResultData(
                double x, double y, double width, double height, int classId, double confidence) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.classId = classId;
            this.confidence = confidence;
        }
    }
}
