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

import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/**
 * ObjectDetector lifecycle:
 *
 * <ol>
 *   <li>{@link Model}s are discovered by {@link NeuralNetworkModelManager}
 *   <li>{@link Model} is selected as a parameter of {@link
 *       org.photonvision.vision.pipe.impl.ObjectDetectionPipe ObjectDetectionPipe}
 *   <li>{@link Model#load()} is called to create a ObjectDetector instance
 *   <li>{@link ObjectDetector#detect(Mat, double, double)} is called to perform object detection
 *   <li>{@link ObjectDetector#release()} is called to release resources
 * </ol>
 */
public interface ObjectDetector extends Releasable {
    /** Returns the model that created this ObjectDetector. */
    public Model getModel();

    /**
     * Returns the classes that the detector can detect
     *
     * @return The classes
     */
    public List<String> getClasses();

    /**
     * Detects objects in the given input image. Preprocessing and postprocessing steps should be
     * embedded into this call.
     *
     * @param in The input image to perform object detection on.
     * @param nmsThresh The threshold value for non-maximum suppression.
     * @param boxThresh The threshold value for bounding box detection.
     * @return A list of NeuralNetworkPipeResult objects representing the detected objects. Returns an
     *     empty list if the detector is not initialized or if no objects are detected.
     */
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh);
}
