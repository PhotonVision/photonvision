package org.photonvision.vision.apriltag;

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagDetector;
import java.util.List;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe;

public class WpilibAprilTagDetectorAdapter implements AprilTagDetectorAdapter {
    private AprilTagDetector detector = new AprilTagDetector();

    @Override
    public AprilTagDetectorBackend getBackendType() {
        return AprilTagDetectorBackend.WPILIB;
    }

    @Override
    public void setParams(AprilTagDetectionPipe.AprilTagDetectionPipeParams newParams) {
        detector.setConfig(newParams.detectorParams());
        detector.setQuadThresholdParameters(newParams.quadParams());

        detector.clearFamilies();
        detector.addFamily(newParams.family().getNativeName());
    }

    @Override
    public List<AprilTagDetection> detect(CVMat in) {
        var ret = detector.detect(in.getMat());
        if (ret == null) {
            return List.of();
        }
        return List.of(ret);
    }

    @Override
    public void release() {
        detector.close();
        detector = null;
    }
}
