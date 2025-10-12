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

package org.photonvision.jni;

import java.awt.Color;
import java.lang.ref.Cleaner;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.rknn.RknnJNI;
import org.photonvision.vision.objects.Letterbox;
import org.photonvision.vision.objects.ObjectDetector;
import org.photonvision.vision.objects.RknnModel;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/** Manages an object detector using the rknn backend. */
public class RknnObjectDetector implements ObjectDetector {
    private static final Logger logger = new Logger(RknnDetectorJNI.class, LogGroup.General);

    /** Cleaner instance to release the detector when it goes out of scope */
    private final Cleaner cleaner = Cleaner.create();

    /** Atomic boolean to ensure that the native object can only be released once. */
    private AtomicBoolean released = new AtomicBoolean(false);

    /** Pointer to the native object */
    private final long objPointer;

    private final RknnModel model;

    private final Size inputSize;

    /** Returns the model in use by this detector. */
    @Override
    public RknnModel getModel() {
        return model;
    }

    /**
     * Creates a new RknnObjectDetector from the given model.
     *
     * @param model The model to create the detector from.
     * @param inputSize The required image dimensions for the model. Images will be {@link
     *     Letterbox}ed to this shape.
     */
    public RknnObjectDetector(RknnModel model, Size inputSize) {
        this.model = model;
        this.inputSize = inputSize;

        // Create the detector
        objPointer =
                RknnJNI.create(model.modelFile.getPath(), model.labels.size(), model.version.ordinal(), -1);
        if (objPointer <= 0) {
            throw new RuntimeException(
                    "Failed to create detector from path " + model.modelFile.getPath());
        }

        logger.debug("Created detector for model " + model.modelFile.getName());

        // Register the cleaner to release the detector when it goes out of scope
        cleaner.register(this, this::release);
    }

    /**
     * Returns the classes that the detector can detect
     *
     * @return The classes
     */
    @Override
    public List<String> getClasses() {
        return model.labels;
    }

    /**
     * Detects objects in the given input image using the RknnDetector.
     *
     * @param in The input image to perform object detection on.
     * @param nmsThresh The threshold value for non-maximum suppression.
     * @param boxThresh The threshold value for bounding box detection.
     * @return A list of NeuralNetworkPipeResult objects representing the detected objects. Returns an
     *     empty list if the detector is not initialized or if no objects are detected.
     */
    @Override
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh) {
        if (objPointer <= 0) {
            // Report error and make sure to include the model name
            logger.error("Detector is not initialized! Model: " + model.modelFile.getName());
            return List.of();
        }

        // Resize the frame to the input size of the model
        Mat letterboxed = new Mat();
        Letterbox scale =
                Letterbox.letterbox(in, letterboxed, this.inputSize, ColorHelper.colorToScalar(Color.GRAY));
        if (!letterboxed.size().equals(this.inputSize)) {
            letterboxed.release();
            throw new RuntimeException("Letterboxed frame is not the right size!");
        }

        // Detect objects in the letterboxed frame
        var results = RknnJNI.detect(objPointer, letterboxed.getNativeObjAddr(), nmsThresh, boxThresh);

        letterboxed.release();

        if (results == null) {
            return List.of();
        }

        return scale.resizeDetections(
                List.of(results).stream()
                        .map(it -> new NeuralNetworkPipeResult(it.rect, it.class_id, it.conf))
                        .toList());
    }

    /** Thread-safe method to release the detector. */
    @Override
    public void release() {
        // Checks if the atomic is 'false', and if so, sets it to 'true'
        if (released.compareAndSet(false, true)) {
            if (objPointer <= 0) {
                logger.error(
                        "Detector is not initialized, and so it can't be released! Model: "
                                + model.modelFile.getName());
                return;
            }

            RknnJNI.destroy(objPointer);
            logger.debug("Released detector for model " + model.modelFile.getName());
        }
    }
}
