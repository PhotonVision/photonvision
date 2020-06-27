package org.photonvision.common.vision.pipe.impl;

import org.photonvision.common.vision.opencv.CVShape;
import org.photonvision.common.vision.opencv.Contour;
import org.photonvision.common.vision.opencv.ContourShape;
import org.photonvision.common.vision.pipe.CVPipe;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class FindPolygonPipe
        extends CVPipe<List<Contour>, List<CVShape>, FindPolygonPipe.FindPolygonPipeParams> {
    private int corners;
    private MatOfPoint2f approx = new MatOfPoint2f();

    /*
    * Runs the process for the pipe.
    *
    * @param in Input for pipe processing.
    * @return Result of processing.
    */
    @Override
    protected List<CVShape> process(List<Contour> in) {
        // List containing all the output shapes
        List<CVShape> output = new ArrayList<>();

        for (Contour contour : in) output.add(getShape(contour));

        return output;
    }

    private CVShape getShape(Contour in) {

        corners = getCorners(in);
        if (ContourShape.fromSides(corners) == null) {
            return new CVShape(in, ContourShape.Custom);
        }
        switch (ContourShape.fromSides(corners)) {
            case Circle:
                return new CVShape(in, ContourShape.Circle);
            case Triangle:
                return new CVShape(in, ContourShape.Triangle);
            case Quadrilateral:
                return new CVShape(in, ContourShape.Quadrilateral);
        }

        return new CVShape(in, ContourShape.Custom);
    }

    private int getCorners(Contour contour) {
        approx.release();
        Imgproc.approxPolyDP(
                contour.getMat2f(),
                approx,
                params.accuracyPercentage / 600.0 * Imgproc.arcLength(contour.getMat2f(), true),
                true);
        return (int) approx.size().height;
    }

    public static class FindPolygonPipeParams {
        double accuracyPercentage;

        public FindPolygonPipeParams(double accuracyPercentage) {
            this.accuracyPercentage = accuracyPercentage;
        }
    }
}
