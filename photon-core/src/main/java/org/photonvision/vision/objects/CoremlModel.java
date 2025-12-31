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
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;

/** Manages an Apple CoreML model for object detection using the Apple Vision framework. */
public class CoremlModel implements Model {
    public final File modelFile;
    public final ModelProperties properties;

    /**
     * Creates a new AppleModel from the given properties.
     *
     * @param properties The properties to create the model from
     * @throws IllegalArgumentException If the properties are invalid
     */
    public CoremlModel(ModelProperties properties) throws IllegalArgumentException {
        // Validate: file must exist
        if (!new File(properties.modelPath().toString()).exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + properties.modelPath());
        }

        // Validate: must have labels
        if (properties.labels() == null || properties.labels().isEmpty()) {
            throw new IllegalArgumentException(
                    "CoremlModel properties must have labels: " + properties.modelPath());
        }

        // Validate: must be APPLE family
        if (properties.family() != Family.COREML) {
            throw new IllegalArgumentException(
                    "CoremlModel properties must have family COREML: " + properties.family());
        }

        this.modelFile = new File(properties.modelPath().toString());
        this.properties = properties;
    }

    @Override
    public ObjectDetector load() {
        return new CoremlObjectDetector(
                this, new Size(properties.resolutionWidth(), properties.resolutionHeight()));
    }

    public ModelProperties getModel() {
        return properties;
    }

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

    @Override
    public String toString() {
        return "CoremlModel{"
                + "modelFile="
                + modelFile
                + ", nickname='"
                + properties.nickname()
                + '\''
                + ", version="
                + properties.version()
                + '}';
    }
}
