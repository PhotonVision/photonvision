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

import java.io.File;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;

/**
 * Model implementation for Apple CoreML-based object detection.
 *
 * <p>This model uses the Swift-Java interop layer to access Apple's CoreML and Vision frameworks
 * for hardware-accelerated object detection on macOS and iOS devices.
 */
public class AppleModel implements Model {
    public final File modelFile;
    public final ModelProperties properties;

    /**
     * Creates a new AppleModel.
     *
     * @param properties The properties of the model. Must specify a CoreML model file (.mlmodel or
     *     .mlmodelc).
     * @throws IllegalArgumentException if model file doesn't exist, labels are missing, or family is
     *     not APPLE
     */
    public AppleModel(ModelProperties properties) throws IllegalArgumentException {
        modelFile = new File(properties.modelPath().toString());
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + modelFile);
        }

        if (properties.labels() == null || properties.labels().isEmpty()) {
            throw new IllegalArgumentException("Labels must be provided");
        }

        if (properties.family() != Family.APPLE) {
            throw new IllegalArgumentException("Model family must be APPLE");
        }

        // CoreML models can be YOLO or other architectures
        if (properties.version() != Version.YOLOV5
                && properties.version() != Version.YOLOV8
                && properties.version() != Version.YOLOV11) {
            throw new IllegalArgumentException("Model version must be YOLOV5, YOLOV8, or YOLOV11");
        }

        this.properties = properties;
    }

    /**
     * Return the unique identifier for the model. In this case, it's the model's path.
     *
     * @return The model's absolute path as a unique identifier
     */
    @Override
    public String getUID() {
        return properties.modelPath().toString();
    }

    @Override
    public String getNickname() {
        return properties.nickname();
    }

    @Override
    public Family getFamily() {
        return properties.family();
    }

    @Override
    public ModelProperties getProperties() {
        return properties;
    }

    /**
     * Load the CoreML model and create an AppleObjectDetector instance.
     *
     * @return A new AppleObjectDetector instance for this model
     * @throws RuntimeException if on a non-Apple platform
     */
    @Override
    public ObjectDetector load() {
        return new AppleObjectDetector(this);
    }

    @Override
    public String toString() {
        return "AppleModel{" + "modelFile=" + modelFile + ", properties=" + properties + '}';
    }
}
