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
import org.opencv.core.Size;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;

public class RubikModel implements Model {
    public final File modelFile;
    public final ModelProperties properties;

    /**
     * Rubik model constructor.
     *
     * @param properties The properties of the model.
     * @throws IllegalArgumentException
     */
    public RubikModel(ModelProperties properties) throws IllegalArgumentException {
        modelFile = new File(properties.modelPath().toString());
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + modelFile);
        }

        if (properties.labels() == null || properties.labels().isEmpty()) {
            throw new IllegalArgumentException("Labels must be provided");
        }

        if (properties.resolutionWidth() <= 0 || properties.resolutionHeight() <= 0) {
            throw new IllegalArgumentException("Resolution must be greater than 0");
        }

        if (properties.family() != Family.RUBIK) {
            throw new IllegalArgumentException("Model family must be RUBIK");
        }

        if (properties.version() != Version.YOLOV8 && properties.version() != Version.YOLOV11) {
            throw new IllegalArgumentException("Model version must be YOLOV8 or YOLOV11");
        }

        this.properties = properties;
    }

    /** Return the unique identifier for the model. In this case, it's the model's path. */
    public String getUID() {
        return properties.modelPath().toString();
    }

    public String getNickname() {
        return properties.nickname();
    }

    public Family getFamily() {
        return properties.family();
    }

    public ModelProperties getProperties() {
        return properties;
    }

    public ObjectDetector load() {
        return new RubikObjectDetector(
                this, new Size(this.properties.resolutionWidth(), this.properties.resolutionHeight()));
    }

    public String toString() {
        return "RubikModel{" + "modelFile=" + modelFile + ", properties=" + properties + '}';
    }
}
