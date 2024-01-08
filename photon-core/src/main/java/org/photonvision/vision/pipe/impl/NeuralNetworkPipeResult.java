package org.photonvision.vision.pipe.impl;

import org.opencv.core.Rect2d;

public class NeuralNetworkPipeResult {
    public NeuralNetworkPipeResult(Rect2d box2, Integer integer, Float float1) {
        box = box2;
        classIdx = integer;
        confidence = float1;
    }
    public final int classIdx;
    public final Rect2d box;
    public final double confidence;
}