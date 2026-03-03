package org.photonvision.vision.apriltag;

import edu.wpi.first.apriltag.AprilTagDetection;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe;

/**
 * Placeholder for the optimized UMich-backed detector. Currently falls back to the WPILib detector
 * while providing a drop-in slot for wiring the optimized JNI.
 */
public class OptimizedAprilTagDetectorAdapter implements AprilTagDetectorAdapter {
    private static final Logger logger = new Logger(OptimizedAprilTagDetectorAdapter.class, LogGroup.VisionModule);

    private final WpilibAprilTagDetectorAdapter fallback = new WpilibAprilTagDetectorAdapter();
    private boolean warnedAboutFallback = false;

    @Override
    public AprilTagDetectorBackend getBackendType() {
        return AprilTagDetectorBackend.OPTIMIZED_UMICH;
    }

    @Override
    public void setParams(AprilTagDetectionPipe.AprilTagDetectionPipeParams newParams) {
        // TODO: replace fallback with optimized detector configuration once JNI is available.
        fallback.setParams(newParams);
    }

    @Override
    public List<AprilTagDetection> detect(CVMat in) {
        if (!warnedAboutFallback) {
            logger.warn("Optimized AprilTag backend not yet wired to JNI; using WPILib fallback");
            warnedAboutFallback = true;
        }
        return fallback.detect(in);
    }

    @Override
    public void release() {
        fallback.release();
    }
}
