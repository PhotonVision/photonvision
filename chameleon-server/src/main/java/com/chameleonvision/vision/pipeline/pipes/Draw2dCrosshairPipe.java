package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.util.Helpers;
import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.CVPipeline2d;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.List;

public class Draw2dCrosshairPipe implements Pipe<Pair<Mat, List<CVPipeline2d.Target2d>>, Mat>{

    private final Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings settings;
    private Point xMax = new Point(), xMin = new Point(), yMax = new Point(), yMin = new Point();

    public Draw2dCrosshairPipe(Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings settings) {
        this.settings = settings;
    }

    public void setConfig(boolean showCrosshair) {
        this.settings.showCrosshair = showCrosshair;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat,List<CVPipeline2d.Target2d>> inputPair) {
        long processStartNanos = System.nanoTime();

        List<CVPipeline2d.Target2d> targets = inputPair.getRight();
        double x,y;
        if(targets != null && !targets.isEmpty()) {
            x = targets.get(0).calibratedX;
            y = targets.get(0).calibratedY;
            if (this.settings.showCrosshair) {
                xMax.set(new double[] {x + 10, y});
                xMin.set(new double[] {x - 10, y});
                yMax.set(new double[] {x, y + 10});
                yMin.set(new double[] {x, y - 10});
                Imgproc.line(inputPair.getLeft(), xMax, xMin, Helpers.colorToScalar(this.settings.crosshairColor), 2);
                Imgproc.line(inputPair.getLeft(), yMax, yMin, Helpers.colorToScalar(this.settings.crosshairColor), 2);
            }
        }
//        image.release();
        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(inputPair.getLeft(), processTime);
    }
    public static class Draw2dCrosshairPipeSettings{
        public boolean showCrosshair =true;
        public Color crosshairColor = Color.GREEN;
    }
}
