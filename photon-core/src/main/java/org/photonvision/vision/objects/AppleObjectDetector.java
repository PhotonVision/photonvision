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

import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;
import org.swift.swiftkit.ffm.AllocatingSwiftArena;

/**
 * Manages an object detector using Apple's CoreML and Vision framework backend.
 *
 * <p>This detector uses Apple's Vision framework which provides automatic image preprocessing
 * (resizing, cropping) and built-in NMS (Non-Maximum Suppression).
 */
public class AppleObjectDetector implements ObjectDetector {
    private static final Logger logger = new Logger(AppleObjectDetector.class, LogGroup.General);

    /** Cleaner instance to release the detector when it goes out of scope */
    private final Cleaner cleaner = Cleaner.create();

    /** Atomic boolean to ensure that the native object can only be released once. */
    private AtomicBoolean released = new AtomicBoolean(false);

    /** Swift detector arena (must outlive the detector instance) */
    private final AllocatingSwiftArena detectorArena;

    /** Swift ObjectDetector instance */
    private final com.photonvision.apple.ObjectDetector swiftDetector;

    private final AppleModel model;

    /** BGR color for letterboxing (gray) */
    private static final Scalar GRAY_COLOR = new Scalar(114, 114, 114);

    /** Returns the model in use by this detector. */
    @Override
    public AppleModel getModel() {
        return model;
    }

    /**
     * Creates a new AppleObjectDetector from the given model.
     *
     * @param model The model to create the detector from.
     * @param inputSize The required image dimensions for the model (used for letterboxing only,
     *     Vision framework handles actual resizing).
     */
    public AppleObjectDetector(AppleModel model, Size inputSize) {
        this.model = model;

        try {
            // Create arena for the detector (must outlive the detector)
            // Use ofAuto() instead of ofConfined() to allow access from VisionRunner thread
            this.detectorArena = AllocatingSwiftArena.ofAuto();

            // Initialize Swift ObjectDetector
            this.swiftDetector =
                    com.photonvision.apple.ObjectDetector.init(
                            model.modelFile.getAbsolutePath(), detectorArena);

            logger.info("Created AppleObjectDetector for model: " + model.getNickname());
        } catch (Exception e) {
            logger.error("Failed to create AppleObjectDetector for model " + model.getNickname(), e);
            throw new RuntimeException("Failed to create AppleObjectDetector", e);
        }

        // Register cleanup action
        // Note: ofAuto() arenas are automatically cleaned up, no manual close needed
        cleaner.register(
                this,
                () -> {
                    if (released.compareAndSet(false, true)) {
                        logger.debug("Auto arena cleanup for AppleObjectDetector: " + model.getNickname());
                    }
                });
    }

    @Override
    public List<String> getClasses() {
        return model.properties.labels();
    }

    /**
     * Detects objects in the given input image.
     *
     * <p>Note: Vision framework performs its own image preprocessing, so we only need to convert to
     * BGRA format.
     *
     * @param in The input image to perform object detection on.
     * @param nmsThresh The threshold value for NMS (unused - Vision framework handles NMS
     *     internally).
     * @param boxThresh The threshold value for bounding box confidence.
     * @return A list of NeuralNetworkPipeResult objects
     */
    @Override
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThreshold) {
        if (released.get()) {
            throw new IllegalStateException("AppleObjectDetector has been released");
        }

        try (var frameArena = AllocatingSwiftArena.ofConfined()) {
            // Convert to BGRA format (required by Swift side)
            Mat bgra = new Mat();
            if (in.channels() == 3) {
                Imgproc.cvtColor(in, bgra, Imgproc.COLOR_BGR2BGRA);
            } else if (in.channels() == 4) {
                bgra = in;
            } else {
                Imgproc.cvtColor(in, bgra, Imgproc.COLOR_GRAY2BGRA);
            }

            // Get image dimensions
            int width = bgra.width();
            int height = bgra.height();
            int totalBytes = height * width * 4; // BGRA = 4 bytes per pixel

            // Allocate memory and copy image data
            MemorySegment imageData = frameArena.allocate(totalBytes, 1);
            byte[] buffer = new byte[totalBytes];
            bgra.get(0, 0, buffer);
            imageData.copyFrom(MemorySegment.ofArray(buffer));

            // Call Swift detector
            var results =
                    swiftDetector.detect(
                            imageData,
                            (long) width,
                            (long) height,
                            2, // BGRA pixel format
                            boxThreshold,
                            nmsThresh, // Passed but not used by Vision framework
                            frameArena);

            // Convert Swift results to Java
            List<NeuralNetworkPipeResult> detections = new ArrayList<>();
            long count = results.count();

            for (int i = 0; i < count; i++) {
                var detection = results.get(i, frameArena);

                // Swift returns normalized coordinates (0-1), convert to pixel coordinates
                double x = detection.getX() * width;
                double y = detection.getY() * height;
                double w = detection.getWidth() * width;
                double h = detection.getHeight() * height;

                // Create bounding box (x, y are center coordinates in Swift, convert to top-left)
                Rect2d bbox = new Rect2d(x, y, w, h);

                detections.add(
                        new NeuralNetworkPipeResult(
                                bbox, (int) detection.getClassId(), detection.getConfidence()));
            }

            // Clean up BGRA mat if we created it
            if (in.channels() == 3 || in.channels() == 1) {
                bgra.release();
            }

            return detections;

        } catch (Exception e) {
            logger.error("Error during Apple object detection", e);
            return new ArrayList<>();
        }
    }

    /**
     * Releases the detector and cleans up native resources.
     *
     * <p>This method is thread-safe and can be called multiple times.
     */
    @Override
    public void release() {
        if (released.compareAndSet(false, true)) {
            // ofAuto() arenas are automatically cleaned up, no manual close needed
            logger.debug("Released AppleObjectDetector for model: " + model.getNickname());
        }
    }

    /**
     * Checks if the detector is still valid (not released).
     *
     * @return true if the detector is valid, false otherwise
     */
    public boolean isValid() {
        return !released.get();
    }
}
