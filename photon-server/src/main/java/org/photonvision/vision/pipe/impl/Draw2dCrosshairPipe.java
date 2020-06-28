package org.photonvision.vision.pipe.impl;

import java.awt.Color;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.RobotOffsetPointMode;
import org.photonvision.vision.target.TrackedTarget;

public class Draw2dCrosshairPipe
        extends CVPipe<Pair<Mat, List<TrackedTarget>>, Mat, Draw2dCrosshairPipe.Draw2dCrosshairParams> {

    @Override
    protected Mat process(Pair<Mat, List<TrackedTarget>> in) {
        Mat image = in.getLeft();

        if (params.m_showCrosshair) {
            double x = image.cols() / 2.0;
            double y = image.rows() / 2.0;
            double scale = image.cols() / 32.0;

            switch (params.m_calibrationMode) {
                case Single:
                    if (!params.m_calibrationPoint.isEmpty()) {
                        x = params.m_calibrationPoint.getFirst();
                        y = params.m_calibrationPoint.getSecond();
                    }
                    break;
                case Dual:
                    // TODO: draw crosshair based on dual calibration
                    break;
            }

            Point xMax = new Point(x + scale, y);
            Point xMin = new Point(x - scale, y);
            Point yMax = new Point(x, y + scale);
            Point yMin = new Point(x, y - scale);

            Imgproc.line(image, xMax, xMin, ColorHelper.colorToScalar(params.m_crosshairColor));
            Imgproc.line(image, yMax, yMin, ColorHelper.colorToScalar(params.m_crosshairColor));
        }
        return image;
    }

    public static class Draw2dCrosshairParams {
        private RobotOffsetPointMode m_calibrationMode;
        private DoubleCouple m_calibrationPoint;
        public boolean m_showCrosshair = true;
        public Color m_crosshairColor = Color.GREEN;

        public Draw2dCrosshairParams(
                RobotOffsetPointMode calibrationMode, DoubleCouple calibrationPoint) {
            m_calibrationMode = calibrationMode;
            m_calibrationPoint = calibrationPoint;
        }
    }
}
