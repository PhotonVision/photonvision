package org.photonvision.vision.pipe.impl;

import edu.wpi.first.apriltag.AprilTagDetection;
import java.util.List;
import org.opencv.core.RotatedRect;

/** Result container for ML hybrid detection */
public record MLDetectionResult(
        List<AprilTagDetection> detections, List<RotatedRect> rois, long nanosElapsed) {}
