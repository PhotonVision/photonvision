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

import edu.wpi.first.apriltag.AprilTagDetection;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Rect;
import org.photonvision.vision.pipe.CVPipe;

public class UncropApriltagsPipe
        extends CVPipe<List<AprilTagDetection>, List<AprilTagDetection>, Rect> {

    public UncropApriltagsPipe(int width, int height) {
        this.params = new Rect(0, 0, width, height);
    }

    @Override
    protected List<AprilTagDetection> process(List<AprilTagDetection> in) {
        List<AprilTagDetection> results = new ArrayList<>();

        for (AprilTagDetection detection : in) {
            results.add(
                    new AprilTagDetection(
                            detection.getFamily(),
                            detection.getId(),
                            detection.getHamming(),
                            detection.getDecisionMargin(),
                            offsetHomography(detection),
                            detection.getCenterX() + this.params.x,
                            detection.getCenterY() + this.params.y,
                            offsetCorners(detection)));
        }

        return results;
    }

    private double[] offsetHomography(AprilTagDetection detection) {
        double dx = this.params.x;
        double dy = this.params.y;
        double[] h = detection.getHomography().clone();

        h[2] = h[2] + dx;
        h[5] = h[5] + dy;

        // New homography from tag to original is H' = T_o * H
        return h;
    }

    private double[] offsetCorners(AprilTagDetection detection) {
        return new double[] {
            detection.getCornerX(0) + this.params.x,
            detection.getCornerY(0) + this.params.y,
            detection.getCornerX(1) + this.params.x,
            detection.getCornerY(1) + this.params.y,
            detection.getCornerX(2) + this.params.x,
            detection.getCornerY(2) + this.params.y,
            detection.getCornerX(3) + this.params.x,
            detection.getCornerY(3) + this.params.y
        };
    }
}
