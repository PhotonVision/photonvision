/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
