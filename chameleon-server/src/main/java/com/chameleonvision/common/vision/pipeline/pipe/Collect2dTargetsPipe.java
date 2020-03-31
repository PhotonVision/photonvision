package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.camera.CaptureStaticProperties;
import com.chameleonvision.common.vision.pipeline.CVPipe;
import com.chameleonvision.common.vision.target.PotentialTarget;
import com.chameleonvision.common.vision.target.TrackedTarget;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Point;

/** Represents a pipe that collects available 2d targets. */
public class Collect2dTargetsPipe
        extends CVPipe<
                List<PotentialTarget>, List<TrackedTarget>, Collect2dTargetsPipe.Collect2dTargetsParams> {

    /**
    * Processes this pipeline.
    *
    * @param in Input for pipe processing.
    * @return A list of tracked targets.
    */
    @Override
    protected List<TrackedTarget> process(List<PotentialTarget> in) {
        List<TrackedTarget> targets = new ArrayList<>();

        var calculationParams =
                new TrackedTarget.TargetCalculationParameters(
                        params.getOrientation() == TrackedTarget.TargetOrientation.Landscape,
                        params.getOffsetPointRegion(),
                        params.getUserOffsetPoint(),
                        params.getCaptureStaticProperties().centerPoint,
                        new DoubleCouple(params.getCalibrationB(), params.getCalibrationM()),
                        params.getOffsetMode(),
                        params.getCaptureStaticProperties().horizontalFocalLength,
                        params.getCaptureStaticProperties().verticalFocalLength,
                        params.getCaptureStaticProperties().imageArea);

        for (PotentialTarget target : in) {
            targets.add(new TrackedTarget(target, calculationParams));
        }

        return targets;
    }

    public static class Collect2dTargetsParams {
        private CaptureStaticProperties m_captureStaticProperties;
        private TrackedTarget.RobotOffsetPointMode m_offsetMode;
        private double m_calibrationM, m_calibrationB;
        private Point m_userOffsetPoint;
        private TrackedTarget.TargetOffsetPointRegion m_region;
        private TrackedTarget.TargetOrientation m_orientation;

        public Collect2dTargetsParams(
                CaptureStaticProperties captureStaticProperties,
                TrackedTarget.RobotOffsetPointMode offsetMode,
                double calibrationM,
                double calibrationB,
                Point calibrationPoint,
                TrackedTarget.TargetOffsetPointRegion region,
                TrackedTarget.TargetOrientation orientation) {
            m_captureStaticProperties = captureStaticProperties;
            m_offsetMode = offsetMode;
            m_calibrationM = calibrationM;
            m_calibrationB = calibrationB;
            m_userOffsetPoint = calibrationPoint;
            m_region = region;
            m_orientation = orientation;
        }

        public CaptureStaticProperties getCaptureStaticProperties() {
            return m_captureStaticProperties;
        }

        public TrackedTarget.RobotOffsetPointMode getOffsetMode() {
            return m_offsetMode;
        }

        public double getCalibrationM() {
            return m_calibrationM;
        }

        public double getCalibrationB() {
            return m_calibrationB;
        }

        public Point getUserOffsetPoint() {
            return m_userOffsetPoint;
        }

        public TrackedTarget.TargetOffsetPointRegion getOffsetPointRegion() {
            return m_region;
        }

        public TrackedTarget.TargetOrientation getOrientation() {
            return m_orientation;
        }
    }
}
