package org.photonvision.common.vision.pipe.impl;

import org.photonvision.common.util.ColorHelper;
import org.photonvision.common.vision.pipe.CVPipe;
import org.photonvision.common.vision.target.TrackedTarget;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class Draw2dContoursPipe
        extends CVPipe<Pair<Mat, List<TrackedTarget>>, Mat, Draw2dContoursPipe.Draw2dContoursParams> {

    private List<MatOfPoint> m_drawnContours = new ArrayList<>();

    @Override
    protected Mat process(Pair<Mat, List<TrackedTarget>> in) {
        if (!in.getRight().isEmpty()
                && (params.showCentroid
                        || params.showMaximumBox
                        || params.showRotatedBox
                        || params.showShape)) {

            var centroidColour = ColorHelper.colorToScalar(params.centroidColor);
            var maximumBoxColour = ColorHelper.colorToScalar(params.maximumBoxColor);
            var rotatedBoxColour = ColorHelper.colorToScalar(params.rotatedBoxColor);
            var shapeColour = ColorHelper.colorToScalar(params.shapeOutlineColour);

            for (int i = 0; i < (params.showMultiple ? in.getRight().size() : 1); i++) {
                Point[] vertices = new Point[4];
                MatOfPoint contour = new MatOfPoint();

                if (i != 0 && !params.showMultiple) {
                    break;
                }

                TrackedTarget target = in.getRight().get(i);
                RotatedRect r = target.getMinAreaRect();

                if (r == null) continue;

                m_drawnContours.forEach(Mat::release);
                m_drawnContours.clear();
                m_drawnContours = new ArrayList<>();

                r.points(vertices);
                contour.fromArray(vertices);
                m_drawnContours.add(contour);

                if (params.showRotatedBox) {
                    Imgproc.drawContours(
                            in.getLeft(), m_drawnContours, 0, rotatedBoxColour, params.boxOutlineSize);
                }

                if (params.showMaximumBox) {
                    Rect box = Imgproc.boundingRect(contour);
                    Imgproc.rectangle(
                            in.getLeft(),
                            new Point(box.x, box.y),
                            new Point(box.x + box.width, box.y + box.height),
                            maximumBoxColour,
                            params.boxOutlineSize);
                }

                if (params.showShape) {
                    Imgproc.drawContours(
                            in.getLeft(),
                            List.of(target.m_mainContour.mat),
                            -1,
                            shapeColour,
                            params.boxOutlineSize);
                }

                if (params.showCentroid) {
                    Imgproc.circle(in.getLeft(), target.getTargetOffsetPoint(), 3, centroidColour, 2);
                }
            }
        }

        return in.getLeft();
    }

    public static class Draw2dContoursParams {
        public boolean showCentroid = true;
        public boolean showMultiple = true;
        public int boxOutlineSize = 1;
        public boolean showRotatedBox = true;
        public boolean showShape = false;
        public boolean showMaximumBox = true;
        public Color centroidColor = Color.GREEN;
        public Color rotatedBoxColor = Color.BLUE;
        public Color maximumBoxColor = Color.RED;
        public Color shapeOutlineColour = Color.MAGENTA;

        // TODO: set other params from UI/settings file?
        public Draw2dContoursParams(boolean showMultipleTargets) {
            this.showMultiple = showMultipleTargets;
        }
    }
}
