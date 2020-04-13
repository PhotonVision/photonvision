package com.chameleonvision._2.vision.pipeline.pipes;

import com.chameleonvision._2.vision.enums.CalibrationMode;
import com.chameleonvision._2.vision.pipeline.Pipe;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision.common.util.ColorHelper;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import java.awt.*;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

public class Draw2dCrosshairPipe
        implements Pipe<Pair<Mat, List<StandardCVPipeline.TrackedTarget>>, Mat> {

    // Settings
    private Draw2dCrosshairPipeSettings crosshairSettings;
    private CalibrationMode calibrationMode;
    private DoubleCouple calibrationPoint;
    private double calibrationM, calibrationB;
    private Point xMax = new Point(), xMin = new Point(), yMax = new Point(), yMin = new Point();

    public Draw2dCrosshairPipe(
            Draw2dCrosshairPipeSettings crosshairSettings,
            CalibrationMode calibrationMode,
            DoubleCouple calibrationPoint,
            double calibrationM,
            double calibrationB) {
        setConfig(crosshairSettings, calibrationMode, calibrationPoint, calibrationM, calibrationB);
    }

    public void setConfig(
            Draw2dCrosshairPipeSettings crosshairSettings,
            CalibrationMode calibrationMode,
            DoubleCouple calibrationPoint,
            double calibrationM,
            double calibrationB) {
        this.crosshairSettings = crosshairSettings;
        this.calibrationMode = calibrationMode;
        this.calibrationPoint = calibrationPoint;
        this.calibrationM = calibrationM;
        this.calibrationB = calibrationB;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat, List<StandardCVPipeline.TrackedTarget>> inputPair) {
        long processStartNanos = System.nanoTime();
        Mat image = inputPair.getLeft();
        //        List<StandardCVPipeline.TrackedTarget> targets = inputPair.getRight();
        double x, y;
        double scale = image.cols() / 32.0;

        drawCrosshair:
        if (this.crosshairSettings.showCrosshair) {
            x = image.cols() / 2.0;
            y = image.rows() / 2.0;
            switch (this.calibrationMode) {
                case Single:
                    if (this.calibrationPoint.equals(new DoubleCouple())) {
                        this.calibrationPoint.set(x, y);
                    }
                    x = this.calibrationPoint.getFirst().intValue();
                    y = this.calibrationPoint.getSecond().intValue();
                    break;
                case Dual:
                    //                    if (targets != null && !targets.isEmpty()) {
                    //                        x = targets.get(0).calibratedX;
                    //                        y = targets.get(0).calibratedY;
                    //                        //TODO dual point calibration crosshair checks
                    //                    } else
                    //                        break drawCrosshair;
                    break;
            }
            xMax.set(new double[] {x + scale, y});
            xMin.set(new double[] {x - scale, y});
            yMax.set(new double[] {x, y + scale});
            yMin.set(new double[] {x, y - scale});
            Imgproc.line(
                    inputPair.getLeft(),
                    xMax,
                    xMin,
                    ColorHelper.colorToScalar(this.crosshairSettings.crosshairColor),
                    2);
            Imgproc.line(
                    inputPair.getLeft(),
                    yMax,
                    yMin,
                    ColorHelper.colorToScalar(this.crosshairSettings.crosshairColor),
                    2);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(inputPair.getLeft(), processTime);
    }

    public static class Draw2dCrosshairPipeSettings {
        public boolean showCrosshair = true;
        public Color crosshairColor = Color.GREEN;
    }
}
