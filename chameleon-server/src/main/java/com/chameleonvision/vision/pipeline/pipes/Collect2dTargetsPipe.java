package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.Pipe;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision.vision.enums.CalibrationMode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.List;

public class Collect2dTargetsPipe implements Pipe<Pair<List<StandardCVPipeline.TrackedTarget>, CaptureStaticProperties>, List<StandardCVPipeline.TrackedTarget>> {


    private CaptureStaticProperties camProps;
    private CalibrationMode calibrationMode;
    private List<Number> calibrationPoint;
    private double calibrationM, calibrationB;
    private List<StandardCVPipeline.TrackedTarget> targets = new ArrayList<>();

    public Collect2dTargetsPipe(CalibrationMode calibrationMode, List<Number> calibrationPoint, double calibrationM, double calibrationB, CaptureStaticProperties camProps) {
        setConfig(calibrationMode, calibrationPoint, calibrationM, calibrationB, camProps);
    }

    public void setConfig(CalibrationMode calibrationMode, List<Number> calibrationPoint, double calibrationM, double calibrationB, CaptureStaticProperties camProps) {
        this.calibrationMode = calibrationMode;
        this.calibrationPoint = calibrationPoint;
        this.calibrationM = calibrationM;
        this.calibrationB = calibrationB;
        this.camProps = camProps;
    }

    @Override
    public Pair<List<StandardCVPipeline.TrackedTarget>, Long> run(Pair<List<StandardCVPipeline.TrackedTarget>, CaptureStaticProperties> inputPair) {
        long processStartNanos = System.nanoTime();

        targets.clear();
        var input = inputPair.getLeft();
        var imageArea = inputPair.getRight().imageArea;

        if (input.size() > 0) {
            for (var t : input) {
                switch (this.calibrationMode) {
                    case Single:
                        if(this.calibrationPoint.isEmpty())
                        {
                            this.calibrationPoint.add(camProps.centerX);
                            this.calibrationPoint.add(camProps.centerY);
                        }
                        t.calibratedX = this.calibrationPoint.get(0).doubleValue();
                        t.calibratedY = this.calibrationPoint.get(1).doubleValue();
                        break;
                    case None:
                        t.calibratedX = camProps.centerX;
                        t.calibratedY = camProps.centerY;
                        break;
                    case Dual:
                        t.calibratedX = (t.minAreaRect.center.y - this.calibrationB) / this.calibrationM;
                        t.calibratedY = (t.minAreaRect.center.x * this.calibrationM) + this.calibrationB;
                        break;
                }

                t.pitch = calculatePitch(t.minAreaRect.center.y, t.calibratedY);
                t.yaw = calculateYaw(t.minAreaRect.center.x, t.calibratedX);
                t.area = t.minAreaRect.size.area() / imageArea;

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
