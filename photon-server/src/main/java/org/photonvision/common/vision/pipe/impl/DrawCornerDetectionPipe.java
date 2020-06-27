package org.photonvision.common.vision.pipe.impl;

import org.photonvision.common.vision.pipe.CVPipe;
import org.photonvision.common.vision.target.TrackedTarget;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

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
