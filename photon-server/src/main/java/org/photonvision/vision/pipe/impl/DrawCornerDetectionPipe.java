package org.photonvision.vision.pipe.impl;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TrackedTarget;

public class DrawCornerDetectionPipe
        extends CVPipe<Pair<Mat, List<TrackedTarget>>, Mat, DrawCornerDetectionPipe.DrawCornerParams> {

    @Override
    protected Mat process(Pair<Mat, List<TrackedTarget>> in) {
        Mat image = in.getLeft();

        for (var target : in.getRight()) {
            var corners = target.getTargetCorners();
            for (var corner : corners) {
                Imgproc.circle(image, corner, params.dotRadius, params.dotColor);
            }
        }

        return image;
    }

    public static class DrawCornerParams {
        int dotRadius;
        Scalar dotColor;
    }
}
