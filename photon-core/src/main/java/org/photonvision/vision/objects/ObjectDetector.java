package org.photonvision.vision.objects;

import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/**
 * ObjectDetector lifecycle: - {@link Model}s are discovered by {@link NeuralNetworkModelManager} -
 * {@link Model} is selected as a parameter of {@link ObjectDetectionPipeline} - {@link Model.load}
 * is called to create a ObjectDetector instance
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
