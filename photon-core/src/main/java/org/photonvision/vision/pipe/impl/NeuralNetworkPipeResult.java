package org.photonvision.vision.pipe.impl;

import org.opencv.core.Rect2d;

public class NeuralNetworkPipeResult {
    public NeuralNetworkPipeResult(Rect2d box2, Integer classIdx, Float confidence) {
        box = box2;
        this.classIdx = classIdx;
        this.confidence = confidence;
    }
    public final int classIdx;
    public final Rect2d box;
    public final double confidence;
}