package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Rect;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.pipe.CVPipe;

public class UncropArucoPipe
        extends CVPipe<List<ArucoDetectionResult>, List<ArucoDetectionResult>, Rect> {

    public UncropArucoPipe(int width, int height) {
        this.params = new Rect(0, 0, width, height);
    }

    @Override
    protected List<ArucoDetectionResult> process(List<ArucoDetectionResult> in) {
        List<ArucoDetectionResult> uncroppedDetections = new ArrayList<>();

        double dx = this.params.x;
        double dy = this.params.y;

        for (ArucoDetectionResult detection : in) {
            double[] originalXCorners = detection.getXCorners();
            double[] originalYCorners = detection.getYCorners();
            double[] adjustedXCorners = new double[4];
            double[] adjustedYCorners = new double[4];

            // Adjust each corner by adding the offset
            for (int i = 0; i < 4; i++) {
                adjustedXCorners[i] = originalXCorners[i] + dx; // X
                adjustedYCorners[i] = originalYCorners[i] + dy; // Y
            }

            // Create a new ArucoDetectionResult with adjusted coordinates
            ArucoDetectionResult adjustedDetection =
                    new ArucoDetectionResult(adjustedXCorners, adjustedYCorners, detection.getId());

            uncroppedDetections.add(adjustedDetection);
        }

        return uncroppedDetections;
    }
}
