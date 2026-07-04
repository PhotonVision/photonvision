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

import java.awt.Color;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.tflite.TFLiteJNI;
import org.photonvision.tflite.TFLiteJNI.TFLiteSource;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/** Manages an object detector using the TFLite backend. */
public class TFLiteObjectDetector implements ObjectDetector {
    private static final Logger logger = new Logger(TFLiteObjectDetector.class, LogGroup.General);

    private static final Cleaner cleaner = Cleaner.create();

    private final Cleanable cleanable;

    private static Runnable cleanupAction(long ptr) {
        return () -> TFLiteJNI.destroy(ptr);
    }

    /** Pointer to the native object */
    private final long ptr;

    private final TFLiteModel model;

    private final Size inputSize;

    /** Returns the model in use by this detector. */
    @Override
    public TFLiteModel getModel() {
        return model;
    }

    /**
     * Creates a new TFLite detector from the given model.
     *
     * @param model The model to create the detector from.
     * @param source The backend to run the detector on.
     */
    public TFLiteObjectDetector(TFLiteModel model, TFLiteSource backend) {
        this.model = model;
        this.inputSize =
                new Size(model.properties.resolutionWidth(), model.properties.resolutionHeight());

        // Create the detector
        try {
            ptr =
                    TFLiteJNI.create(
                            model.modelFile.getPath().toString(),
                            model.properties.version().ordinal(),
                            backend.value());
        } catch (Exception e) {
            logger.error("Failed to create detector from path " + model.modelFile.getPath(), e);
            throw new RuntimeException(
                    "Failed to create detector from path " + model.modelFile.getPath(), e);
        }

        if (!isValid()) {
            logger.error(
                    "Failed to create detector from path "
                            + model.modelFile.getPath()
                            + ". Please ensure the model is valid and compatible with the TFLite backend.");
            throw new RuntimeException(
                    "Failed to create detector from path " + model.modelFile.getPath());
        } else if (!TFLiteJNI.isQuantized(ptr)) {
            throw new UnsupportedOperationException("Model must be quantized.");
        }

        logger.debug("Created detector for model " + model.modelFile.getName());

        // Register the cleaner to release the detector when it goes out of scope
        cleanable = cleaner.register(this, cleanupAction(ptr));
    }

    /**
     * Returns the classes that the detector can detect
     *
     * @return The classes
     */
    @Override
    public List<String> getClasses() {
        return model.properties.labels();
    }

    /**
     * Detects objects in the given input image using the TFLite detector.
     *
     * @param in The input image to perform object detection on.
     * @param nmsThresh The threshold value for non-maximum suppression.
     * @param boxThresh The threshold value for bounding box detection.
     * @return A list of NeuralNetworkPipeResult objects representing the detected objects. Returns an
     *     empty list if the detector is not initialized or if no objects are detected.
     */
    @Override
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh) {
        if (!isValid()) {
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
        var results = TFLiteJNI.detect(ptr, letterboxed.getNativeObjAddr(), boxThresh, nmsThresh);

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
        cleanable.clean();
    }

    private boolean isValid() {
        return ptr != 0;
    }
}
