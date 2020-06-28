package org.photonvision.vision.pipe.impl;

import java.util.List;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipe.CVPipe;

public class FilterShapesPipe
        extends CVPipe<List<CVShape>, List<CVShape>, FilterShapesPipe.FilterShapesPipeParams> {
    /**
    * Runs the process for the pipe.
    *
    * @param in Input for pipe processing.
    * @return Result of processing.
    */
    @Override
    protected List<CVShape> process(List<CVShape> in) {
        in.removeIf(
                shape ->
                        shape.shape != params.desiredShape
                                || shape.contour.getArea() > params.maxArea
                                || shape.contour.getArea() < params.minArea
                                || shape.contour.getPerimeter() > params.maxPeri
                                || shape.contour.getPerimeter() < params.minPeri);
        return in;
    }

    public static class FilterShapesPipeParams {
        ContourShape desiredShape;
        double minArea;
        double maxArea;
        double minPeri;
        double maxPeri;

        public FilterShapesPipeParams(
                ContourShape desiredShape, double minArea, double maxArea, double minPeri, double maxPeri) {
            this.desiredShape = desiredShape;
            this.minArea = minArea;
            this.maxArea = maxArea;
            this.minPeri = minPeri;
            this.maxPeri = maxPeri;
        }
    }
}
