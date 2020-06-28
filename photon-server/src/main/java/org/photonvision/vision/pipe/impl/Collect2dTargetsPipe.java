package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Point;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.*;

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
                        params.getOrientation() == TargetOrientation.Landscape,
                        params.getOffsetPointRegion(),
                        params.getUserOffsetPoint(),
                        params.getFrameStaticProperties().centerPoint,
                        new DoubleCouple(params.getCalibrationB(), params.getCalibrationM()),
                        params.getOffsetMode(),
                        params.getFrameStaticProperties().horizontalFocalLength,
                        params.getFrameStaticProperties().verticalFocalLength,
                        params.getFrameStaticProperties().imageArea);

        for (PotentialTarget target : in) {
            targets.add(new TrackedTarget(target, calculationParams));
        }

        return targets;
    }

    public static class Collect2dTargetsParams {
        private FrameStaticProperties m_captureStaticProperties;
        private RobotOffsetPointMode m_offsetMode;
        private double m_calibrationM, m_calibrationB;
        private Point m_userOffsetPoint;
        private TargetOffsetPointEdge m_region;
        private TargetOrientation m_orientation;

        public Collect2dTargetsParams(
                FrameStaticProperties captureStaticProperties,
                RobotOffsetPointMode offsetMode,
                double calibrationM,
                double calibrationB,
                Point calibrationPoint,
                TargetOffsetPointEdge region,
                TargetOrientation orientation) {
            m_captureStaticProperties = captureStaticProperties;
            m_offsetMode = offsetMode;
            m_calibrationM = calibrationM;
            m_calibrationB = calibrationB;
            m_userOffsetPoint = calibrationPoint;
            m_region = region;
            m_orientation = orientation;
        }

        public FrameStaticProperties getFrameStaticProperties() {
            return m_captureStaticProperties;
        }

        public RobotOffsetPointMode getOffsetMode() {
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

        public TargetOffsetPointEdge getOffsetPointRegion() {
            return m_region;
        }

        public TargetOrientation getOrientation() {
            return m_orientation;
        }
    }
}
