package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.util.Helpers;
import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.CVPipeline2d;
import com.chameleonvision.vision.pipeline.CVPipeline2dSettings;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.List;

public class Draw2dCrosshairPipe implements Pipe<Pair<Mat, List<CVPipeline2d.Target2d>>, Mat> {

    //Settings
    private Draw2dCrosshairPipeSettings crosshairSettings;
    private CVPipeline2dSettings.Calibration calibrationSettings;


    private Point xMax = new Point(), xMin = new Point(), yMax = new Point(), yMin = new Point();

    public Draw2dCrosshairPipe(Draw2dCrosshairPipeSettings crosshairSettings, CVPipeline2dSettings.Calibration calibrationSettings) {
        this.crosshairSettings = crosshairSettings;
        this.calibrationSettings = calibrationSettings;
    }

    public void setConfig(boolean showCrosshair, CVPipeline2dSettings.Calibration calibrationSettings) {
        this.crosshairSettings.showCrosshair = showCrosshair;
        this.calibrationSettings = calibrationSettings;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat, List<CVPipeline2d.Target2d>> inputPair) {
        long processStartNanos = System.nanoTime();
        Mat image = inputPair.getLeft();
        List<CVPipeline2d.Target2d> targets = inputPair.getRight();
        double x = 0, y = 0, scale = image.cols() / 32.0;

        drawCrosshair:
        if (this.crosshairSettings.showCrosshair) {
            switch (calibrationSettings.calibrationMode) {
                case None:
                    x = image.rows() / 2;
                    y = image.cols() / 2;
                    break;
                case Single:
                    x = targets.get(0).calibratedX;
                    y = targets.get(0).calibratedY;
                    break;
                case Dual:
                    if (targets != null && !targets.isEmpty()) {
                        x = targets.get(0).calibratedX;
                        y = targets.get(0).calibratedY;
                        //TODO dual point calibration crosshair checks
                    } else {
                        break drawCrosshair;
                    }
                    break;
            }
            xMax.set(new double[]{x + scale, y});
            xMin.set(new double[]{x - scale, y});
            yMax.set(new double[]{x, y + scale});
            yMin.set(new double[]{x, y - scale});
            Imgproc.line(inputPair.getLeft(), xMax, xMin, Helpers.colorToScalar(this.crosshairSettings.crosshairColor), 2);
            Imgproc.line(inputPair.getLeft(), yMax, yMin, Helpers.colorToScalar(this.crosshairSettings.crosshairColor), 2);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(inputPair.getLeft(), processTime);
    }

    public static class Draw2dCrosshairPipeSettings {
        public boolean showCrosshair = true;
        public Color crosshairColor = Color.GREEN;
    }
}
