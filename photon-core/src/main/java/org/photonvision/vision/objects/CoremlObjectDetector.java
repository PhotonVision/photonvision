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

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.apple.MemorySegmentCompat;
import org.photonvision.apple.SwiftArena;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/**
 * Manages an object detector using Apple's CoreML and Vision framework backend.
 *
 * <p>This detector uses Apple's Vision framework which provides automatic image preprocessing
 * (resizing, cropping) and built-in NMS (Non-Maximum Suppression).
 */
public class CoremlObjectDetector implements ObjectDetector {
    private static final Logger logger = new Logger(CoremlObjectDetector.class, LogGroup.General);

    private final SwiftArena arena;

    /** Swift ObjectDetector instance */
    private final org.photonvision.apple.ObjectDetector swiftDetector;

    private final CoremlModel model;

    /** Returns the model in use by this detector. */
    @Override
    public CoremlModel getModel() {
        return model;
    }

    /**
     * Creates a new AppleObjectDetector from the given model.
     *
     * @param model The model to create the detector from.
     * @param inputSize unused - The required image dimensions for the model
     */
    public CoremlObjectDetector(CoremlModel model, Size inputSize) {
        this.model = model;

        // ofAuto() instead of ofConfined() to allow access from VisionRunner thread
        this.arena = SwiftArena.ofAuto();

        // Initialize Swift ObjectDetector
        this.swiftDetector =
                org.photonvision.apple.ObjectDetector.init(
                        model.modelFile.getAbsolutePath(), arena.unwrap());

        logger.info("Created AppleObjectDetector for model: " + model.getNickname());
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
        try (var frameArena = SwiftArena.ofConfined()) {
            // Convert to BGRA format (required by Swift side)
            Mat bgra = new Mat();
            var matFilled = false;

            if (in.channels() == 1) {
                Imgproc.cvtColor(in, bgra, Imgproc.COLOR_GRAY2BGRA);
                matFilled = true;
            } else if (in.channels() == 3) {
                Imgproc.cvtColor(in, bgra, Imgproc.COLOR_BGR2BGRA);
                matFilled = true;
            } else if (in.channels() == 4) {
                bgra = in;
            } else {
                logger.error(
                        "AppleObjectDetector: Input image has unsupported number of channels: "
                                + in.channels());
                return new ArrayList<>();
            }

            var mem = MemorySegmentCompat.ofAddress(bgra.dataAddr());
            var width = in.width();
            var height = in.height();

            var results = swiftDetector.detect(mem, width, height, boxThreshold, frameArena.unwrap());

            List<NeuralNetworkPipeResult> detections = new ArrayList<>();
            long count = results.count();

            for (int i = 0; i < count; i++) {
                var detection = results.get(i, frameArena.unwrap());

                double w = detection.getWidth() * width;
                double h = detection.getHeight() * height;
                double x = detection.getX() * width - w / 2.0;
                double y = detection.getY() * height - h / 2.0;

                Rect2d bbox = new Rect2d(x, y, w, h);

                detections.add(
                        new NeuralNetworkPipeResult(
                                bbox, (int) detection.getClassId(), detection.getConfidence()));
            }

            if (matFilled) {
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
        // ofConfined arenas are already closed via try-with-resources
        // ofAuto is cleaned up by the GC
    }
}
