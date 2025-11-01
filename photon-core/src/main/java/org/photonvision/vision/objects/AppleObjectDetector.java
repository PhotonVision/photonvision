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

package org.photonvision.vision.objects;

import com.photonvision.apple.AppleVisionLibraryLoader;
import com.photonvision.apple.DetectionResult;
import com.photonvision.apple.DetectionResultArray;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

/**
 * Object detector using Apple's CoreML and Vision frameworks via Swift-Java interop.
 *
 * <p>This detector leverages hardware acceleration on macOS devices. It requires Java 24+ and the
 * photon-apple subproject.
 *
 * <p>This implementation is optimized for sequential frame processing with reused resources.
 */
public class AppleObjectDetector implements ObjectDetector {
    private static final Logger logger = new Logger(AppleObjectDetector.class, LogGroup.General);

    // Load native libraries before any Swift classes are used
    static {
        try {
            AppleVisionLibraryLoader.initialize();
            logger.info("Apple Vision libraries loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Failed to load Apple Vision libraries", e);
            throw e;
        }
    }

    /** Arena for managing the Swift detector object's lifecycle (auto for multi-threaded access) */
    private final AllocatingSwiftArena detectorArena;

    /** The Swift ObjectDetector instance */
    private final com.photonvision.apple.ObjectDetector swiftDetector;

    /** Atomic boolean to ensure that resources can only be released once */
    private final AtomicBoolean released = new AtomicBoolean(false);

    private final AppleModel appleModel;

    /**
     * Creates a new AppleObjectDetector from the given model.
     *
     * @param model The CoreML model to use for detection
     * @throws RuntimeException if initialization fails
     */
    public AppleObjectDetector(AppleModel model) {
        this.appleModel = model;

        try {
            // Create long-lived arena for detector (auto for multi-threaded access)
            detectorArena = AllocatingSwiftArena.ofAuto();

            // Initialize Swift ObjectDetector with model path
            swiftDetector =
                    com.photonvision.apple.ObjectDetector.init(
                            model.modelFile.getAbsolutePath(), detectorArena);

            logger.debug("Created Apple CoreML detector for model " + model.modelFile.getName());

        } catch (Exception e) {
            // Cleanup: confined arenas are auto-closed when no longer referenced
            throw new RuntimeException("Failed to initialize Apple CoreML detector", e);
        }
    }

    /** Returns the model in use by this detector. */
    @Override
    public Model getModel() {
        return appleModel;
    }

    /**
     * Returns the classes that the detector can detect.
     *
     * @return The list of class labels
     */
    @Override
    public List<String> getClasses() {
        return appleModel.properties.labels();
    }

    /**
     * Detects objects in the given input image using Apple CoreML.
     *
     * @param in The input image (OpenCV Mat, BGR format)
     * @param nmsThresh The NMS (non-maximum suppression) IoU threshold
     * @param boxThresh The confidence threshold for detections
     * @return List of detected objects with bounding boxes and class information
     */
    @Override
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh) {
        long detectStartNs = System.nanoTime();

        if (released.get()) {
            logger.warn("Attempted to use released AppleObjectDetector");
            return List.of();
        }

        if (in.empty()) {
            logger.warn("Input image is empty");
            return List.of();
        }

        logger.debug(
                String.format(
                        "Detection called with image: %dx%d (channels=%d), nmsThresh=%s, boxThresh=%s",
                        in.cols(), in.rows(), in.channels(), nmsThresh, boxThresh));

        // Create confined arena on current thread for this frame's data
        try (var frameArena = AllocatingSwiftArena.ofConfined()) {
            // Convert to BGRA (creates new Mat if conversion needed)
            long bgraStartNs = System.nanoTime();
            Mat bgraMat = convertToBGRA(in);
            long bgraEndNs = System.nanoTime();
            System.err.printf(
                    "[TIMING-JAVA] BGRA conversion: %.3f ms%n", (bgraEndNs - bgraStartNs) / 1_000_000.0);

            logger.debug(
                    String.format(
                            "Converted to BGRA: %dx%d (channels=%d)",
                            bgraMat.cols(), bgraMat.rows(), bgraMat.channels()));

            try {
                int width = bgraMat.cols();
                int height = bgraMat.rows();
                int channels = bgraMat.channels();
                int elemSize = (int) bgraMat.elemSize();
                long totalElements = bgraMat.total();
                int totalBytes = Math.toIntExact(totalElements * elemSize);

                // Allocate memory
                long allocStartNs = System.nanoTime();
                MemorySegment imageData = frameArena.allocate(totalBytes, 1);
                long allocEndNs = System.nanoTime();
                System.err.printf(
                        "[TIMING-JAVA] Memory allocation: %.3f ms%n",
                        (allocEndNs - allocStartNs) / 1_000_000.0);

                // Copy data - handle both continuous and non-continuous Mats
                long copyStartNs = System.nanoTime();
                byte[] buffer;
                if (bgraMat.isContinuous()) {
                    // Fast path: continuous memory, single copy
                    logger.debug("Mat is continuous");
                    long matGetStartNs = System.nanoTime();
                    buffer = new byte[totalBytes];
                    bgraMat.get(0, 0, buffer);
                    long matGetEndNs = System.nanoTime();
                    System.err.printf(
                            "[TIMING-JAVA] Mat.get() to byte[]: %.3f ms%n",
                            (matGetEndNs - matGetStartNs) / 1_000_000.0);

                    long memcpyStartNs = System.nanoTime();
                    MemorySegment.copy(
                            buffer, 0, imageData, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, totalBytes);
                    long memcpyEndNs = System.nanoTime();
                    System.err.printf(
                            "[TIMING-JAVA] MemorySegment.copy(): %.3f ms%n",
                            (memcpyEndNs - memcpyStartNs) / 1_000_000.0);
                } else {
                    // Slow path: non-continuous memory, copy row by row
                    logger.debug("Mat is non-continuous, copying row-by-row");
                    int rowBytes = width * elemSize;
                    byte[] rowBuf = new byte[rowBytes];
                    buffer = new byte[totalBytes];

                    for (int r = 0; r < height; r++) {
                        int read = bgraMat.get(r, 0, rowBuf);
                        if (read != rowBytes) {
                            logger.error(
                                    String.format(
                                            "Unexpected row byte count: expected %d but got %d", rowBytes, read));
                            return List.of();
                        }
                        System.arraycopy(rowBuf, 0, buffer, r * rowBytes, rowBytes);
                    }

                    MemorySegment.copy(
                            buffer, 0, imageData, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, totalBytes);
                }
                long copyEndNs = System.nanoTime();
                System.err.printf(
                        "[TIMING-JAVA] Total data copy: %.3f ms%n", (copyEndNs - copyStartNs) / 1_000_000.0);

                // Debug: log first few bytes to verify data is valid
                if (buffer.length >= 16) {
                    logger.debug(
                            String.format(
                                    "First 16 bytes: %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
                                    buffer[0] & 0xFF,
                                    buffer[1] & 0xFF,
                                    buffer[2] & 0xFF,
                                    buffer[3] & 0xFF,
                                    buffer[4] & 0xFF,
                                    buffer[5] & 0xFF,
                                    buffer[6] & 0xFF,
                                    buffer[7] & 0xFF,
                                    buffer[8] & 0xFF,
                                    buffer[9] & 0xFF,
                                    buffer[10] & 0xFF,
                                    buffer[11] & 0xFF,
                                    buffer[12] & 0xFF,
                                    buffer[13] & 0xFF,
                                    buffer[14] & 0xFF,
                                    buffer[15] & 0xFF));
                }

                logger.debug(String.format("Calling Swift detector with %dx%d BGRA image", width, height));

                long swiftCallStartNs = System.nanoTime();
                DetectionResultArray results =
                        swiftDetector.detectRaw(
                                imageData,
                                (long) width,
                                (long) height,
                                2, // BGRA pixel format
                                boxThresh,
                                nmsThresh,
                                frameArena);
                long swiftCallEndNs = System.nanoTime();
                System.err.printf(
                        "[TIMING-JAVA] Swift detect() call (total): %.3f ms%n",
                        (swiftCallEndNs - swiftCallStartNs) / 1_000_000.0);

                logger.debug(String.format("Swift detector returned %d results", results.count()));

                long convertStartNs = System.nanoTime();
                List<NeuralNetworkPipeResult> detections =
                        convertResults(results, width, height, frameArena);
                long convertEndNs = System.nanoTime();
                System.err.printf(
                        "[TIMING-JAVA] Result conversion: %.3f ms%n",
                        (convertEndNs - convertStartNs) / 1_000_000.0);

                long detectEndNs = System.nanoTime();
                System.err.printf(
                        "[TIMING-JAVA] ===== TOTAL JAVA DETECT: %.3f ms =====%n",
                        (detectEndNs - detectStartNs) / 1_000_000.0);

                logger.info(
                        String.format(
                                "AppleObjectDetector detected %d objects (nms=%s, box=%s)",
                                detections.size(), nmsThresh, boxThresh));
                return detections;
            } finally {
                // Release the converted mat if we created a new one
                if (bgraMat != in) {
                    bgraMat.release();
                }
            }

        } catch (Exception e) {
            logger.error("Detection failed", e);
            return List.of();
        }
    }

    /**
     * Convert any OpenCV Mat to BGRA format
     *
     * @param mat Input Mat (any format)
     * @return Mat in BGRA format (may be the same object if already BGRA)
     */
    private Mat convertToBGRA(Mat mat) {
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
     * Convert Swift DetectionResultArray to List of NeuralNetworkPipeResult.
     *
     * @param resultsArray Swift DetectionResultArray object
     * @param imageWidth Original image width for denormalizing coordinates
     * @param imageHeight Original image height for denormalizing coordinates
     * @param frameArena Arena for accessing detection results
     * @return List of PhotonVision detection results
     */
    private List<NeuralNetworkPipeResult> convertResults(
            DetectionResultArray resultsArray,
            int imageWidth,
            int imageHeight,
            AllocatingSwiftArena frameArena) {
        List<NeuralNetworkPipeResult> results = new ArrayList<>();

        long count = resultsArray.count();

        for (int i = 0; i < count; i++) {
            DetectionResult detection = resultsArray.get((long) i, frameArena);

            // Denormalize coordinates (Swift returns 0-1 normalized, top-left origin)
            double x = detection.getX() * imageWidth;
            double y = detection.getY() * imageHeight;
            double width = detection.getWidth() * imageWidth;
            double height = detection.getHeight() * imageHeight;

            // Create PhotonVision result (Rect2d uses x,y,width,height)
            Rect2d bbox = new Rect2d(x, y, width, height);
            NeuralNetworkPipeResult result =
                    new NeuralNetworkPipeResult(bbox, detection.getClassId(), detection.getConfidence());

            logger.debug(
                    String.format(
                            "Detection %d: class=%d, confidence=%.3f, bbox=[x=%.1f, y=%.1f, w=%.1f, h=%.1f]",
                            i, detection.getClassId(), detection.getConfidence(), x, y, width, height));
            results.add(result);
        }

        return results;
    }

    /**
     * Release resources associated with this detector. Safe to call multiple times.
     *
     * <p>Note: Auto arena is automatically closed when no longer referenced.
     */
    @Override
    public void release() {
        if (released.compareAndSet(false, true)) {
            logger.debug("Released Apple CoreML detector");
        }
    }
}
