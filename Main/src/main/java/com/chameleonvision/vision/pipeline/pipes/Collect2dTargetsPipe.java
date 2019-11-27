package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.CVPipeline2d;
import com.chameleonvision.vision.enums.CalibrationMode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.RotatedRect;

import java.util.ArrayList;
import java.util.List;

public class Collect2dTargetsPipe implements Pipe<List<RotatedRect>, List<CVPipeline2d.Target2d>> {

    private CalibrationMode calibrationMode;
    private CaptureStaticProperties camProps;
    private List<Number> calibrationPoint;
    private double calibrationM, calibrationB;

    private List<CVPipeline2d.Target2d> targets = new ArrayList<>();

    public Collect2dTargetsPipe(CalibrationMode calibrationMode, List<Number> calibrationPoint,
                                double calibrationM, double calibrationB, CaptureStaticProperties camProps) {
        this.calibrationMode = calibrationMode;
        this.camProps = camProps;
        this.calibrationPoint = calibrationPoint;
        this.calibrationM = calibrationM;
        this.calibrationB = calibrationB;
    }

    public void setConfig(CalibrationMode calibrationMode, List<Number> calibrationPoint,
                          double calibrationM, double calibrationB, CaptureStaticProperties camProps) {
        this.calibrationMode = calibrationMode;
        this.camProps = camProps;
        this.calibrationPoint = calibrationPoint;
        this.calibrationM = calibrationM;
        this.calibrationB = calibrationB;
    }

    @Override
    public Pair<List<CVPipeline2d.Target2d>, Long> run(List<RotatedRect> input) {
        long processStartNanos = System.nanoTime();

        targets.clear();

        if (input.size() > 0) {
            for (RotatedRect r : input) {
                CVPipeline2d.Target2d t = new CVPipeline2d.Target2d();
                t.rawPoint = r;
                switch (calibrationMode) {
                    case None:
                        t.calibratedX = camProps.centerX;
                        t.calibratedY = camProps.centerY;
                        break;
                    case Single:
                        t.calibratedX = calibrationPoint.get(0).doubleValue();
                        t.calibratedY = calibrationPoint.get(1).doubleValue();
                        break;
                    case Dual:
                        t.calibratedX = (r.center.y - calibrationB) / calibrationM;
                        t.calibratedY = (r.center.x * calibrationM) + calibrationB;
                        break;
                }

                t.pitch = calculatePitch(r.center.y, t.calibratedY);
                t.yaw = calculateYaw(r.center.x, t.calibratedX);
                t.area = r.size.area();

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
