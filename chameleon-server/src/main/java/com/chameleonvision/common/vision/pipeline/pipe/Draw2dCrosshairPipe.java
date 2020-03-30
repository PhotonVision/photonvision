package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.util.ColorHelper;
import com.chameleonvision.common.vision.pipeline.CVPipe;
import com.chameleonvision.common.vision.target.TrackedTarget;
import java.awt.Color;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class Draw2dCrosshairPipe
        extends CVPipe<Pair<Mat, List<TrackedTarget>>, Mat, Draw2dCrosshairPipe.Draw2dCrosshairParams> {

    @Override
    protected Mat process(Pair<Mat, List<TrackedTarget>> in) {
        Mat image = in.getLeft();

        double x, y;
        double scale = image.cols() / 32.0;

        if (params.showCrosshair) {
            x = image.cols() / 2.0;
            y = image.rows() / 2.0;

            switch (params.calibrationMode) {
                case Single:
                    if (params.calibrationPoint.equals(new Point())) {
                        params.calibrationPoint.set(new double[] {x, y});
                    }
                    x = (int) params.calibrationPoint.x;
                    y = (int) params.calibrationPoint.y;
                    break;
                case Dual:
                    // TODO
                    break;
            }
            Point xMax = new Point(x + scale, y);
            Point xMin = new Point(x - scale, y);
            Point yMax = new Point(x, y + scale);
            Point yMin = new Point(x, y - scale);

            Imgproc.line(in.getLeft(), xMax, xMin, ColorHelper.colorToScalar(params.crosshairColor));
            Imgproc.line(in.getLeft(), yMax, yMin, ColorHelper.colorToScalar(params.crosshairColor));
        }
        return in.getLeft();
    }

    public static class Draw2dCrosshairParams {
        public TrackedTarget.RobotOffsetPointMode calibrationMode;
        public Point calibrationPoint;
        public boolean showCrosshair = true;
        public Color crosshairColor = Color.GREEN;
    }
}
