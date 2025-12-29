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
import java.io.File;
import java.util.Arrays;
import org.opencv.core.Size;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;

/** Manages an Apple CoreML model for object detection using the Apple Vision framework. */
public class AppleModel implements Model {
    public final File modelFile;
    public final ModelProperties properties;

    /**
     * Creates a new AppleModel from the given properties.
     *
     * @param properties The properties to create the model from
     * @throws IllegalArgumentException If the properties are invalid
     */
    public AppleModel(ModelProperties properties) throws IllegalArgumentException {
        // Validate: file must exist
        if (!new File(properties.modelPath().toString()).exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + properties.modelPath());
        }

        // Validate: must have labels
        if (properties.labels() == null || properties.labels().isEmpty()) {
            throw new IllegalArgumentException(
                    "AppleModel properties must have labels: " + properties.modelPath());
        }

        // Validate: must have resolution
        if (properties.resolutionWidth() == 0 || properties.resolutionHeight() == 0) {
            throw new IllegalArgumentException(
                    "AppleModel properties must have resolution: " + properties.modelPath());
        }

        // Validate: must be APPLE family
        if (properties.family() != Family.APPLE) {
            throw new IllegalArgumentException(
                    "AppleModel properties must have family APPLE: " + properties.family());
        }

        // Validate: version must be supported (YOLOV8 or YOLOV11)
        var supportedVersions = Arrays.asList(Version.YOLOV8, Version.YOLOV11);
        if (!supportedVersions.contains(properties.version())) {
            throw new IllegalArgumentException(
                    "AppleModel properties must have version in "
                            + supportedVersions
                            + ", got "
                            + properties.version());
        }

        this.modelFile = new File(properties.modelPath().toString());
        this.properties = properties;
    }

    @Override
    public ObjectDetector load() {
        // Initialize and extract native libraries before creating detector
        AppleVisionLibraryLoader.initialize();

        return new AppleObjectDetector(
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
        return "AppleModel{"
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
