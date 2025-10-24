package com.photonvision.apple;

import java.lang.foreign.MemorySegment;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

/**
 * Small stable wrapper around the generated ObjectDetector to validate and log
 * buffer address/size and call arguments before invoking native code.
 */
public final class ObjectDetectorSafe {
    private ObjectDetectorSafe() {}

    public static DetectionResultArray detectChecked(ObjectDetector detector,
            MemorySegment imageData,
            long width,
            long height,
            int pixelFormat,
            double boxThreshold,
            double nmsThreshold,
            AllocatingSwiftArena frameArena) {

        if (imageData == null) {
            throw new IllegalArgumentException("imageData is null");
        }

        long bytesAvailable = imageData.byteSize();

        int bytesPerPixel;
        switch (pixelFormat) {
            case ImageUtils.PIXEL_FORMAT_BGR:
            case ImageUtils.PIXEL_FORMAT_RGB:
                bytesPerPixel = 3;
                break;
            case ImageUtils.PIXEL_FORMAT_BGRA:
            case ImageUtils.PIXEL_FORMAT_RGBA:
                bytesPerPixel = 4;
                break;
            case ImageUtils.PIXEL_FORMAT_GRAY:
            default:
                bytesPerPixel = 1;
                break;
        }

        long expected = width * height * (long) bytesPerPixel;

        // Log details to stderr to correlate with native crash reports.
        try {
            System.err.println("[ObjectDetectorSafe] imageData.address=" + imageData.address() + " byteSize=" + bytesAvailable +
                    " expected=" + expected + " width=" + width + " height=" + height + " pixelFormat=" + pixelFormat);
        } catch (Throwable t) {
            // address() may not be available on some platforms; still continue.
            System.err.println("[ObjectDetectorSafe] imageData.byteSize=" + bytesAvailable + " (address() unavailable)");
        }

        if (bytesAvailable < expected) {
            throw new IllegalArgumentException("imageData byteSize (" + bytesAvailable + ") is smaller than expected (" + expected + ")");
        }

        // Forward to the generated detector
        return detector.detect(imageData, width, height, pixelFormat, boxThreshold, nmsThreshold, frameArena);
    }
}
