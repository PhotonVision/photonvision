package org.photonvision.vision.apriltag;

import java.util.List;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe;
import org.photonvision.vision.opencv.Releasable;
import edu.wpi.first.apriltag.AprilTagDetection;

public interface AprilTagDetectorAdapter extends Releasable {
    AprilTagDetectorBackend getBackendType();

    void setParams(AprilTagDetectionPipe.AprilTagDetectionPipeParams params);

    List<AprilTagDetection> detect(CVMat in);
}
