package org.photonvision.jni;

import java.lang.ref.Cleaner;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.rknn.RknnJNI;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/**
 * A class to represent an object detector using the Rknn library.
 *
 * <p>TODO: When we start supporting more platforms, we should consider moving most of this code
 * into a common "ObjectDetector" class to define the common interface for all object detectors.
 */
public class RknnObjectDetector implements Releasable {
    /** logger for the RknnObjectDetector */
    private static final Logger logger = new Logger(RknnDetectorJNI.class, LogGroup.General);

    /** Cleaner instance to release the detector when it is no longer needed */
    private final Cleaner cleaner = Cleaner.create();

    /** Pointer to the native object */
    private final long objPointer;

    /** Model configuration */
    private final NeuralNetworkModelManager.Model model;

    /** Returns the model used by the detector. */
    public NeuralNetworkModelManager.Model getModel() {
        return model;
    }

    /** Atomic boolean to ensure that the detector is only released _once_. */
    private AtomicBoolean released = new AtomicBoolean(false);

    /**
     * Creates a new RknnObjectDetector from the given model.
     *
     * @param model The model to create the detector from.
     */
    public RknnObjectDetector(NeuralNetworkModelManager.Model model) {
        this.model = model;

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

        // Set the detector to be released when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(this::release));
    }

    /**
     * Returns the classes that the detector can detect
     *
     * @return The classes
     */
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
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh) {
        if (objPointer <= 0) {
            // Report error and make sure to include the model name
            logger.error("Detector is not initialized! Model: " + model.modelFile.getName());
            return List.of();
        }

        var results = RknnJNI.detect(objPointer, in.getNativeObjAddr(), nmsThresh, boxThresh);
        if (results == null) {
            return List.of();
        }

        return List.of(results).stream()
                .map(it -> new NeuralNetworkPipeResult(it.rect, it.class_id, it.conf))
                .toList();
    }

    /** Thread-safe method to release the detector. */
    @Override
    public void release() {
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
