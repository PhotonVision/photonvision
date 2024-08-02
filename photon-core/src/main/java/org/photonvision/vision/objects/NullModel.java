package org.photonvision.vision.objects;

import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/**
 * A 'null' implementation of the {@link Model} and {@link ObjectDetector} interfaces. This is used
 * when no model is available to load.
 */
public class NullModel implements Model, ObjectDetector {
    // Singleton instance
    public static final NullModel INSTANCE = new NullModel();

    private NullModel() {}

    public static NullModel getInstance() {
        return INSTANCE;
    }

    @Override
    public ObjectDetector load() {
        return this;
    }

    @Override
    public String getName() {
        return "NullModel";
    }

    @Override
    public void release() {
        // Do nothing
    }

    @Override
    public Model getModel() {
        return this;
    }

    @Override
    public List<String> getClasses() {
        return List.of();
    }

    @Override
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh) {
        return List.of();
    }
}
