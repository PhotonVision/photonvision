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
import org.photonvision.common.configuration.NeuralNetworkProperties.RknnModelProperties;
import org.photonvision.jni.RknnObjectDetector;

public class RknnModel implements Model {
    public final File modelFile;
    public final RknnModelProperties properties;

    /**
     * rknn model constructor.
     *
     * @param properties The properties of the model.
     * @throws IllegalArgumentException
     */
    public RknnModel(RknnModelProperties properties) throws IllegalArgumentException {
        modelFile = new File(properties.modelPath);
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("Model file does not exist: " + modelFile);
        }

        if (properties.labels == null || properties.labels.isEmpty()) {
            throw new IllegalArgumentException("Labels must be provided");
        }

        if (properties.resolutionWidth <= 0 || properties.resolutionHeight <= 0) {
            throw new IllegalArgumentException("Resolution must be greater than 0");
        }

        if (properties.family != Family.RKNN) {
            throw new IllegalArgumentException("Model family must be RKNN");
        }

        if (properties.rknnVersion == null) {
            throw new IllegalArgumentException("Model version must be provided");
        }

        this.properties = properties;
    }

    public String getName() {
        return properties.nickname;
    }

    public Family getFamily() {
        return properties.family;
    }

    public ObjectDetector load() {
        return new RknnObjectDetector(
                this, new Size(properties.resolutionWidth, properties.resolutionHeight));
    }
}
