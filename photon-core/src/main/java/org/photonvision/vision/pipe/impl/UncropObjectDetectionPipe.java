package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.photonvision.vision.pipe.CVPipe;

public class UncropObjectDetectionPipe
        extends CVPipe<List<NeuralNetworkPipeResult>, List<NeuralNetworkPipeResult>, Rect> {

    public UncropObjectDetectionPipe(int width, int height) {
        this.params = new Rect(0, 0, width, height);
    }

    @Override
    protected List<NeuralNetworkPipeResult> process(List<NeuralNetworkPipeResult> in) {
        List<NeuralNetworkPipeResult> temp = new ArrayList<>();

        for (NeuralNetworkPipeResult result : in) {
            temp.add(
                    new NeuralNetworkPipeResult(
                            offsetObjectBoundingBox(result.bbox), result.classIdx, result.confidence));
        }

        return temp;
    }

    private Rect2d offsetObjectBoundingBox(Rect2d in) {

        return new Rect2d(in.x + params.x, in.y + params.y, in.width, in.height);
    }
}
