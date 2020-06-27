package org.photonvision._2.vision.pipeline.pipes;

import org.photonvision._2.config.CameraCalibrationConfig;
import org.photonvision._2.vision.pipeline.Pipe;
import org.photonvision._2.vision.pipeline.impl.StandardCVPipeline;
import org.photonvision._2.vision.pipeline.impl.StandardCVPipelineSettings;
import org.photonvision.common.util.ColorHelper;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class DrawSolvePNPPipe
        implements Pipe<Pair<Mat, List<StandardCVPipeline.TrackedTarget>>, Mat> {

    private MatOfPoint3f boxCornerMat = new MatOfPoint3f();

    public Scalar green = ColorHelper.colorToScalar(Color.GREEN);
    public Scalar blue = ColorHelper.colorToScalar(Color.BLUE);
    public Scalar red = ColorHelper.colorToScalar(Color.RED);
    public Scalar orange = ColorHelper.colorToScalar(Color.orange);

    public DrawSolvePNPPipe(
        StandardCVPipelineSettings standardCVPipelineSettings, CameraCalibrationConfig settings) {
        setConfig(settings);
        setBox(standardCVPipelineSettings.targetCornerMat);
    }

    private void setBox(MatOfPoint3f mat) {
        boxCornerMat.release();
        var list = mat.toList();
        var auxList =
                list.stream().map(it -> new Point3(it.x, it.y, it.z + 6)).collect(Collectors.toList());
        var finalList = new ArrayList<>(list);
        finalList.addAll(auxList);
        boxCornerMat.fromList(finalList);
    }

    public void setConfig(StandardCVPipelineSettings settings) {
        setBox(settings.targetCornerMat);
    }

    private Mat cameraMatrix = new Mat();
    private MatOfDouble distortionCoefficients = new MatOfDouble();

    public void setConfig(CameraCalibrationConfig config) {
        if (config == null) {
            System.err.println("got passed a null config! Returning...");
            return;
        }
        setConfig(config.getCameraMatrixAsMat(), config.getDistortionCoeffsAsMat());
    }

    public void setConfig(Mat cameraMatrix_, MatOfDouble distortionMatrix_) {
        this.cameraMatrix = cameraMatrix_;
        this.distortionCoefficients = distortionMatrix_;
    }

    MatOfPoint2f imagePoints = new MatOfPoint2f();

    @Override
    public Pair<Mat, Long> run(Pair<Mat, List<StandardCVPipeline.TrackedTarget>> targets) {
        long processStartNanos = System.nanoTime();

        var image = targets.getLeft();
        for (var it : targets.getRight()) {

            try {
                Calib3d.projectPoints(
                        boxCornerMat,
                        it.rVector,
                        it.tVector,
                        this.cameraMatrix,
                        this.distortionCoefficients,
                        imagePoints,
                        new Mat(),
                        0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            var pts = imagePoints.toList();

            // draw left and right targets if possible
            if (it.leftRightDualTargetPair != null) {
                var left = it.leftRightDualTargetPair.getLeft();
                var right = it.leftRightDualTargetPair.getRight();
                Imgproc.rectangle(image, left.tl(), left.br(), new Scalar(200, 200, 0), 4);
                Imgproc.rectangle(image, right.tl(), right.br(), new Scalar(200, 200, 0), 2);
            }

            // draw poly dp
            var list = it.approxPoly.toList();
            for (int i = 0; i < list.size(); i++) {
                var next = (i == list.size() - 1) ? list.get(0) : list.get(i + 1);
                Imgproc.line(image, list.get(i), next, red, 2);
            }

            // draw center
            Imgproc.circle(image, it.minAreaRect.center, 5, red);

            // draw corners
            for (int i = 0; i < it.imageCornerPoints.rows(); i++) {
                var point = new Point(it.imageCornerPoints.get(i, 0));
                Imgproc.circle(image, point, 4, green, 5);
            }

            // sketch out floor
            Imgproc.line(image, pts.get(0), pts.get(1), green, 3);
            Imgproc.line(image, pts.get(1), pts.get(2), green, 3);
            Imgproc.line(image, pts.get(2), pts.get(3), green, 3);
            Imgproc.line(image, pts.get(3), pts.get(0), green, 3);

            // draw pillars
            Imgproc.line(image, pts.get(0), pts.get(4), blue, 3);
            Imgproc.line(image, pts.get(1), pts.get(5), blue, 3);
            Imgproc.line(image, pts.get(2), pts.get(6), blue, 3);
            Imgproc.line(image, pts.get(3), pts.get(7), blue, 3);

            // draw top
            Imgproc.line(image, pts.get(4), pts.get(5), red, 3);
            Imgproc.line(image, pts.get(5), pts.get(6), red, 3);
            Imgproc.line(image, pts.get(6), pts.get(7), red, 3);
            Imgproc.line(image, pts.get(7), pts.get(4), red, 3);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(image, processTime);
    }
}
