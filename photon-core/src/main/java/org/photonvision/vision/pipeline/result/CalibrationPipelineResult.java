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

package org.photonvision.vision.pipeline.result;

import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Point;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.target.TrackedTarget;


public class CalibrationPipelineResult extends CVPipelineResult {

    private static List<TrackedTarget> cornersToTarget(List<List<Point>> corners) {
        return corners.stream().map(TrackedTarget::new).collect(Collectors.toList());
    }

    public CalibrationPipelineResult(double latencyNanos, double fps, Frame outputFrame, List<List<Point>> corners) {
        super(latencyNanos, fps, cornersToTarget(corners), outputFrame);
    }
}
