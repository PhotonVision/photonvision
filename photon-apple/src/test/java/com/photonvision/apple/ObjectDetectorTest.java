package com.photonvision.apple;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.Test;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

/** Tests for ObjectDetector functionality */
class ObjectDetectorTest {

    @Test
    void test_ImageUtils_getPixelFormatFromChannels() {
        assertEquals(0, ImageUtils.getPixelFormatFromChannels(3)); // BGR
        assertEquals(2, ImageUtils.getPixelFormatFromChannels(4)); // BGRA
        assertEquals(4, ImageUtils.getPixelFormatFromChannels(1)); // GRAY
    }

    @Test
    void test_ImageUtils_pixelFormatToString() {
        assertEquals("BGR", ImageUtils.pixelFormatToString(0));
        assertEquals("RGB", ImageUtils.pixelFormatToString(1));
        assertEquals("BGRA", ImageUtils.pixelFormatToString(2));
        assertEquals("RGBA", ImageUtils.pixelFormatToString(3));
        assertEquals("GRAY", ImageUtils.pixelFormatToString(4));
        assertEquals("UNKNOWN", ImageUtils.pixelFormatToString(99));
    }

    @Test
    void test_ObjectDetector_detectFake_returnsSyntheticResults() {
        // Test that we can successfully receive DetectionResult data from Swift
        // This validates the Swift→Java data passing without requiring a CoreML model

        try (var arena = AllocatingSwiftArena.ofConfined()) {
            // Create detector (doesn't need a real model for detectFake)
            ObjectDetector detector = ObjectDetector.init("/fake/path/model.mlmodel", arena);
            assertNotNull(detector);

            // Create a dummy BGRA image
            try (var frameArena = AllocatingSwiftArena.ofConfined()) {
                int width = 640;
                int height = 480;
                int totalBytes = height * width * 4;
                MemorySegment imageData = frameArena.allocate(totalBytes, 1);

                // Call detectFake which returns synthetic test data
                DetectionResultArray results =
                        detector.detectFake(
                                imageData,
                                640L,
                                480L,
                                2, // BGRA format
                                0.5,
                                0.4,
                                frameArena);

                // Validate we got the expected synthetic results
                assertNotNull(results);
                assertEquals(3, results.count(), "Should return 3 fake detection results");

                // Validate first detection result
                DetectionResult det0 = results.get(0, frameArena);
                assertNotNull(det0);

                assertEquals(0.1, det0.getX(), 0.001, "First detection X coordinate");
                assertEquals(0.2, det0.getY(), 0.001, "First detection Y coordinate");
                assertEquals(0.3, det0.getWidth(), 0.001, "First detection width");
                assertEquals(0.4, det0.getHeight(), 0.001, "First detection height");
                assertEquals(1, det0.getClassId(), "First detection class ID");
                assertEquals(0.95, det0.getConfidence(), 0.001, "First detection confidence");
            }
        }
    }
}
