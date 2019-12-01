package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.util.Helpers;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Draw2dContoursPipe implements Pipe<Pair<Mat, List<RotatedRect>>, Mat> {

    private final Draw2dContoursSettings settings;
    private CaptureStaticProperties camProps;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    private Point[] vertices = new Point[4];
    private List<MatOfPoint> drawnContours = new ArrayList<>();
    @SuppressWarnings("FieldCanBeLocal")
    private Point xMax = new Point(), xMin = new Point(), yMax = new Point(), yMin = new Point();


    public Draw2dContoursPipe(Draw2dContoursSettings settings, CaptureStaticProperties camProps) {
        this.settings = settings;
        this.camProps = camProps;
    }

    public void setConfig(boolean showMultiple,CaptureStaticProperties captureProps) {
        settings.showMultiple = showMultiple;
        camProps = captureProps;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat, List<RotatedRect>> input) {
        long processStartNanos = System.nanoTime();

        if (settings.showCrosshair || settings.showCentroid || settings.showMaximumBox || settings.showRotatedBox) {
//            input.getLeft().copyTo(processBuffer);
            processBuffer = input.getLeft();

            if (input.getRight().size() > 0) {
                for (int i = 0; i < input.getRight().size(); i++) {
                    if (i != 0 && !settings.showMultiple){
                        break;
                    }
                    RotatedRect r = input.getRight().get(i);
                    if (r == null) continue;

                    drawnContours.forEach(Mat::release);
                    drawnContours.clear();

                    r.points(vertices);
                    MatOfPoint contour = new MatOfPoint(vertices);
                    drawnContours.add(contour);

                    if (settings.showCentroid) {
                        Imgproc.circle(processBuffer, r.center, 3, Helpers.colorToScalar(settings.centroidColor));
                    }

                    if (settings.showRotatedBox) {
                        Imgproc.drawContours(processBuffer, drawnContours, 0, Helpers.colorToScalar(settings.rotatedBoxColor), settings.boxOutlineSize);
                    }

                    if (settings.showMaximumBox) {
                        Rect box = Imgproc.boundingRect(contour);
                        Imgproc.rectangle(processBuffer, new Point(box.x, box.y), new Point((box.x + box.width), (box.y + box.height)), Helpers.colorToScalar(settings.maximumBoxColor), settings.boxOutlineSize);
                    }
                }
            }

            if (settings.showCrosshair) {
                xMax.set(new double[] {camProps.centerX + 10, camProps.centerY});
                xMin.set(new double[] {camProps.centerX - 10, camProps.centerY});
                yMax.set(new double[] {camProps.centerX, camProps.centerY + 10});
                yMin.set(new double[] {camProps.centerX, camProps.centerY - 10});
                Imgproc.line(processBuffer, xMax, xMin, Helpers.colorToScalar(settings.crosshairColor), 2);
                Imgproc.line(processBuffer, yMax, yMin, Helpers.colorToScalar(settings.crosshairColor), 2);
            }

//            processBuffer.copyTo(outputMat);
//            processBuffer.release();
        } else {
//            input.getLeft().copyTo(outputMat);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(processBuffer, processTime);
    }

    public static class Draw2dContoursSettings {
        public boolean showCentroid = false;
        public boolean showCrosshair = false;
        public boolean showMultiple = false;
        public int boxOutlineSize = 0;
        public boolean showRotatedBox = false;
        public boolean showMaximumBox = false;
        public Color centroidColor = Color.GREEN;
        public Color crosshairColor = Color.GREEN;
        public Color rotatedBoxColor = Color.BLUE;
        public Color maximumBoxColor = Color.RED;
    }
}
