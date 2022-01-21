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
import org.opencv.core.Point;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.DualOffsetValues;
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
                        params.targetOrientation == TargetOrientation.Landscape,
                        params.targetOffsetPointEdge,
                        params.robotOffsetPointMode,
                        params.robotOffsetSinglePoint,
                        params.dualOffsetValues,
                        params.frameStaticProperties);

        for (PotentialTarget target : in) {
            targets.add(new TrackedTarget(target, calculationParams, target.shape));
        }

        return targets;
    }

    public static class Collect2dTargetsParams {
        private final RobotOffsetPointMode robotOffsetPointMode;
        private final Point robotOffsetSinglePoint;
        private final DualOffsetValues dualOffsetValues;
        private final TargetOffsetPointEdge targetOffsetPointEdge;
        private final TargetOrientation targetOrientation;
        private final FrameStaticProperties frameStaticProperties;

        public Collect2dTargetsParams(
                RobotOffsetPointMode robotOffsetPointMode,
                Point robotOffsetSinglePoint,
                DualOffsetValues dualOffsetValues,
                TargetOffsetPointEdge targetOffsetPointEdge,
                TargetOrientation orientation,
                FrameStaticProperties frameStaticProperties) {
            this.frameStaticProperties = frameStaticProperties;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.robotOffsetSinglePoint = robotOffsetSinglePoint;
            this.dualOffsetValues = dualOffsetValues;
            this.targetOffsetPointEdge = targetOffsetPointEdge;
            targetOrientation = orientation;
        }
    }
}
