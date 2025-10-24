// ===----------------------------------------------------------------------===//
//
// This source file is part of the Swift.org open source project
//
// Copyright (c) 2024 Apple Inc. and the Swift.org project authors
// Licensed under Apache License v2.0
//
// See LICENSE.txt for license information
// See CONTRIBUTORS.txt for the list of Swift.org project authors
//
// SPDX-License-Identifier: Apache-2.0
//
// ===----------------------------------------------------------------------===//

package com.photonvision.apple;

import java.lang.foreign.MemorySegment;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

/**
 * Utility class for image format information
 *
 * <p>Note: Image conversion is now handled directly in AppleObjectDetector for better performance.
 * This class remains for pixel format constants and test utilities.
 */
public class ImageUtils {

    /**
     * Determine the PixelFormat enum value from channel count
     *
     * @param channels Number of channels in the image
     * @return PixelFormat value (0=BGR, 1=RGB, 2=BGRA, 3=RGBA, 4=GRAY)
     */
    public static int getPixelFormatFromChannels(int channels) {
        return switch (channels) {
            case 1 -> 4; // GRAY
            case 3 -> 0; // BGR (OpenCV default)
            case 4 -> 2; // BGRA (OpenCV convention)
            default -> throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        };
    }

    /** Get a human-readable string for the pixel format */
    public static String pixelFormatToString(int format) {
        return switch (format) {
            case 0 -> "BGR";
            case 1 -> "RGB";
            case 2 -> "BGRA";
            case 3 -> "RGBA";
            case 4 -> "GRAY";
            default -> "UNKNOWN";
        };
    }

    // Pixel format constants
    public static final int PIXEL_FORMAT_BGR = 0;
    public static final int PIXEL_FORMAT_RGB = 1;
    public static final int PIXEL_FORMAT_BGRA = 2;
    public static final int PIXEL_FORMAT_RGBA = 3;
    public static final int PIXEL_FORMAT_GRAY = 4;

    /**
     * Convert OpenCV Mat to MemorySegment for Swift interop (test utility)
     *
     * <p>This method converts the Mat to BGRA format (required by Swift/CoreML), then allocates new
     * memory and copies the data to ensure proper alignment and lifetime management.
     *
     * @param mat OpenCV Mat containing image data (any format)
     * @param arena Arena for memory management
     * @return MemorySegment containing BGRA image data
     */
    public static MemorySegment matToMemorySegment(Mat mat, AllocatingSwiftArena arena) {
        if (mat == null || mat.empty()) {
            throw new IllegalArgumentException("Mat cannot be null or empty");
        }

        // Convert to BGRA if needed (Swift expects BGRA format)
        Mat bgraMat = convertToBGRA(mat);

        try {
            // Use OpenCV's total() and elemSize() to compute exact byte count
            long totalElements = bgraMat.total();
            int elemSize = (int) bgraMat.elemSize(); // bytes per element (includes channels)

            if (totalElements <= 0 || elemSize <= 0) {
                throw new IllegalArgumentException("Mat has invalid size/elemSize");
            }

            int totalBytes = Math.toIntExact(totalElements * elemSize);

            // Allocate new memory in the arena (critical for proper alignment and lifetime)
            var segment = arena.allocate(totalBytes, 1);

            // Copy Mat data to the allocated segment. Handle non-contiguous Mats by copying
            // row-by-row to guarantee we copy the bytes the native side expects.
            if (bgraMat.isContinuous()) {
                byte[] buffer = new byte[totalBytes];
                bgraMat.get(0, 0, buffer);
                java.lang.foreign.MemorySegment.copy(
                        buffer, 0, segment, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, totalBytes);
            } else {
                int rows = bgraMat.rows();
                int cols = bgraMat.cols();
                int rowBytes = cols * elemSize;
                byte[] rowBuf = new byte[rowBytes];
                byte[] outBuf = new byte[totalBytes];

                for (int r = 0; r < rows; r++) {
                    int read = bgraMat.get(r, 0, rowBuf);
                    if (read != rowBytes) {
                        // Defensive: if OpenCV returns fewer bytes than expected, fail fast.
                        throw new IllegalStateException(
                                "Unexpected row byte count while copying Mat: expected "
                                        + rowBytes
                                        + " but got "
                                        + read);
                    }
                    System.arraycopy(rowBuf, 0, outBuf, r * rowBytes, rowBytes);
                }

                java.lang.foreign.MemorySegment.copy(
                        outBuf, 0, segment, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, totalBytes);
            }

            return segment;
        } finally {
            // Release the converted mat if we created a new one
            if (bgraMat != mat) {
                bgraMat.release();
            }
        }
    }

    /**
     * Convert any OpenCV Mat to BGRA format using OpenCV's conversion functions
     *
     * @param mat Input Mat (any format)
     * @return Mat in BGRA format (may be the same object if already BGRA)
     */
    private static Mat convertToBGRA(Mat mat) {
        if (mat == null || mat.empty()) {
            throw new IllegalArgumentException("Mat cannot be null or empty");
        }

        int channels = mat.channels();

        // Already BGRA - return as-is
        if (channels == 4) {
            return mat;
        }

        Mat bgraMat = new Mat();

        if (channels == 3) {
            // BGR -> BGRA (add alpha channel)
            Imgproc.cvtColor(mat, bgraMat, Imgproc.COLOR_BGR2BGRA);
        } else if (channels == 1) {
            // GRAY -> BGRA
            Imgproc.cvtColor(mat, bgraMat, Imgproc.COLOR_GRAY2BGRA);
        } else {
            throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }

        return bgraMat;
    }

    /**
     * Get pixel format from OpenCV Mat (test utility)
     *
     * @param mat OpenCV Mat
     * @return Pixel format constant
     */
    public static int getPixelFormat(Mat mat) {
        return getPixelFormatFromChannels(mat.channels());
    }
}
