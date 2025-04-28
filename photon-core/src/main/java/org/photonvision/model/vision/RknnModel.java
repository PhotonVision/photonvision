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

package org.photonvision.model.vision;

import java.io.File;
import java.util.List;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.model.vision.object.ObjectDetector;
import org.photonvision.model.vision.object.RknnObjectDetector;
import org.photonvision.rknn.RknnJNI;

public class RknnModel implements Model {
    private static final Logger logger = new Logger(RknnModel.class, LogGroup.Config);

    public final File modelFile;
    public final RknnJNI.ModelVersion version;
    public final List<String> labels;
    public final Size inputSize;

    /**
     * rknn model constructor. Assumes parameters have been validated and parsed by the format
     * handler.
     *
     * @param modelFile path to model on disk.
     * @param labels List of labels read from the labels file.
     * @param version The determined model version.
     * @param inputSize The required input size.
     * @throws IllegalArgumentException (though less likely now with pre-validation)
     */
    public RknnModel(
            File modelFile, List<String> labels, RknnJNI.ModelVersion version, Size inputSize)
            throws IllegalArgumentException {
        this.modelFile = modelFile;
        this.labels = labels; // Assume labels are already read by handler
        this.version = version;
        this.inputSize = inputSize;

        // Removed logic to parse name, determine version/size, and read labels
        // as this is now handled by RknnFormatHandler
        logger.info(
                "RknnModel created for: "
                        + modelFile.getName()
                        + ", Version: "
                        + version
                        + ", Size: "
                        + inputSize);
    }

    @Override
    public String getName() {
        return modelFile.getName();
    }

    @Override
    public ObjectDetector loadToObjectDetector() {
        if (!modelFile.exists()) {
            logger.error("Model file does not exist when trying to load: " + modelFile.getPath());
            return null; // Or handle error appropriately
        }
        return new RknnObjectDetector(this, inputSize);
    }
}
