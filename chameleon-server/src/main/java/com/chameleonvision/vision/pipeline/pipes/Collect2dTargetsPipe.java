package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.CVPipeline2d;
import com.chameleonvision.vision.enums.CalibrationMode;
import com.chameleonvision.vision.pipeline.CVPipeline2dSettings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.ArrayList;
import java.util.List;

public class Collect2dTargetsPipe implements Pipe<Pair<List<RotatedRect>, CaptureStaticProperties>, List<CVPipeline2d.Target2d>> {


    private CaptureStaticProperties camProps;
    private CVPipeline2dSettings.Calibration calibrationSettings;
    private List<CVPipeline2d.Target2d> targets = new ArrayList<>();

    public Collect2dTargetsPipe(CVPipeline2dSettings.Calibration calibrationSettings, CaptureStaticProperties camProps) {
        setConfig(calibrationSettings,camProps);
    }

    public void setConfig(CVPipeline2dSettings.Calibration calibrationSettings, CaptureStaticProperties camProps) {
        this.calibrationSettings = calibrationSettings;
        this.camProps = camProps;
    }

    @Override
    public Pair<List<CVPipeline2d.Target2d>, Long> run(Pair<List<RotatedRect>, CaptureStaticProperties> inputPair) {
        long processStartNanos = System.nanoTime();

        targets.clear();
        var input = inputPair.getLeft();
        var imageArea = inputPair.getRight().imageArea;

        if (input.size() > 0) {
            for (RotatedRect r : input) {
                CVPipeline2d.Target2d t = new CVPipeline2d.Target2d();
                t.rawPoint = r;
                switch (calibrationSettings.calibrationMode) {
                    case None:
                        t.calibratedX = camProps.centerX;
                        t.calibratedY = camProps.centerY;
                        break;
                    case Single:
                        t.calibratedX = calibrationSettings.calibrationPoint.get(0).doubleValue();
                        t.calibratedY = calibrationSettings.calibrationPoint.get(1).doubleValue();
                        break;
                    case Dual:
                        t.calibratedX = (r.center.y - calibrationSettings.calibrationB) / calibrationSettings.calibrationM;
                        t.calibratedY = (r.center.x * calibrationSettings.calibrationM) + calibrationSettings.calibrationB;
                        break;
                }

                t.pitch = calculatePitch(r.center.y, t.calibratedY);
                t.yaw = calculateYaw(r.center.x, t.calibratedX);
                t.area = r.size.area() / imageArea;

                targets.add(t);
            }
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(targets, processTime);
    }

    private double calculatePitch(double pixelY, double centerY) {
        double pitch = FastMath.toDegrees(FastMath.atan((pixelY - centerY) / camProps.verticalFocalLength));
        return (pitch * -1);
    }

    private double calculateYaw(double pixelX, double centerX) {
        return FastMath.toDegrees(FastMath.atan((pixelX - centerX) / camProps.horizontalFocalLength));
    }
}
