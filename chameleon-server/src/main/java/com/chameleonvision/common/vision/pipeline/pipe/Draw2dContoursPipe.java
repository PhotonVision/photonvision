package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.util.ColorHelper;
import com.chameleonvision.common.vision.pipeline.CVPipe;
import com.chameleonvision.common.vision.target.TrackedTarget;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

public class Draw2dContoursPipe
        extends CVPipe<Pair<Mat, List<TrackedTarget>>, Mat, Draw2dContoursPipe.Draw2dContoursParams> {

    private List<MatOfPoint> m_drawnContours = new ArrayList<>();

    @Override
    protected Mat process(Pair<Mat, List<TrackedTarget>> in) {
        if (params.showCentroid || params.showMaximumBox || params.showRotatedBox) {
            for (int i = 0; i < in.getRight().size(); i++) {
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
                            in.getLeft(),
                            m_drawnContours,
                            0,
                            ColorHelper.colorToScalar(params.rotatedBoxColor),
                            params.boxOutlineSize);
                }

                if (params.showMaximumBox) {
                    Rect box = Imgproc.boundingRect(contour);
                    Imgproc.rectangle(
                            in.getLeft(),
                            new Point(box.x, box.y),
                            new Point(box.x + box.width, box.y + box.height),
                            ColorHelper.colorToScalar(params.maximumBoxColor),
                            params.boxOutlineSize);
                }

                if (params.showCentroid) {
                    Imgproc.circle(
                            in.getLeft(),
                            target.getTargetOffsetPoint(),
                            3,
                            ColorHelper.colorToScalar(params.centroidColor),
                            2);
                }
            }
        }

        return in.getLeft();
    }

    public static class Draw2dContoursParams {
        public boolean showCentroid = false;
        public boolean showMultiple = false;
        public int boxOutlineSize = 0;
        public boolean showRotatedBox = false;
        public boolean showMaximumBox = false;
        public Color centroidColor = Color.GREEN;
        public Color rotatedBoxColor = Color.BLUE;
        public Color maximumBoxColor = Color.RED;
    }
}
