package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.util.Helpers;
import com.chameleonvision.vision.pipeline.Pipe;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipeline;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Draw2dContoursPipe implements Pipe<Pair<Mat, List<StandardCVPipeline.TrackedTarget>>, Mat> {

    private final Draw2dContoursSettings settings;
    private CaptureStaticProperties camProps;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    private Point[] vertices = new Point[4];
    private List<MatOfPoint> drawnContours = new ArrayList<>();
    private MatOfPoint contour = new MatOfPoint();
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
    public Pair<Mat, Long> run(Pair<Mat, List<StandardCVPipeline.TrackedTarget>> input) {
        long processStartNanos = System.nanoTime();

        if (settings.showCentroid || settings.showMaximumBox || settings.showRotatedBox) {
//            input.getLeft().copyTo(processBuffer);
//            processBuffer = input.getLeft();

            if (input.getRight().size() > 0) {
                for (int i = 0; i < input.getRight().size(); i++) {
                    if (i != 0 && !settings.showMultiple){
                        break;
                    }
                    StandardCVPipeline.TrackedTarget target = input.getRight().get(i);
                    RotatedRect r = input.getRight().get(i).minAreaRect;
                    if (r == null) continue;

                    drawnContours.forEach(Mat::release);
                    drawnContours.clear();
                    drawnContours = new ArrayList<>();

                    r.points(vertices);
                    contour.fromArray(vertices);
//                    MatOfPoint contour = new MatOfPoint(vertices);
                    drawnContours.add(contour);

                    if (settings.showCentroid) {
                        Imgproc.circle(input.getLeft(), r.center, 3, Helpers.colorToScalar(settings.centroidColor));
                    }

                    if (settings.showRotatedBox) {
                        Imgproc.drawContours(input.getLeft(), drawnContours, 0, Helpers.colorToScalar(settings.rotatedBoxColor), settings.boxOutlineSize);
                    }

                    if (settings.showMaximumBox) {
                        Rect box = Imgproc.boundingRect(contour);
                        Imgproc.rectangle(input.getLeft(), new Point(box.x, box.y), new Point((box.x + box.width), (box.y + box.height)), Helpers.colorToScalar(settings.maximumBoxColor), settings.boxOutlineSize);
                    }

//                    contour.release();
                }
            }

//            processBuffer.copyTo(outputMat);
//            processBuffer.release();
        } else {
//            input.getLeft().copyTo(outputMat);
        }
        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(input.getLeft(), processTime);
    }

    public static class Draw2dContoursSettings {
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
