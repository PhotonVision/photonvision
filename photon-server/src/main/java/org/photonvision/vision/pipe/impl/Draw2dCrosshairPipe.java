/*
 * Copyright (C) 2020 Photon Vision.
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

import java.awt.Color;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.MutatingPipe;
import org.photonvision.vision.target.RobotOffsetPointMode;
import org.photonvision.vision.target.TargetCalculations;
import org.photonvision.vision.target.TrackedTarget;

public class Draw2dCrosshairPipe
        extends MutatingPipe<
                Pair<Mat, List<TrackedTarget>>, Draw2dCrosshairPipe.Draw2dCrosshairParams> {

    @Override
    protected Void process(Pair<Mat, List<TrackedTarget>> in) {
        if (!params.shouldDraw) return null;

        var camCenterPoint = params.frameStaticProperties.centerPoint;
        var image = in.getLeft();

        if (params.showCrosshair) {
            double x = params.frameStaticProperties.centerX;
            double y = params.frameStaticProperties.centerY;
            double scale = params.frameStaticProperties.imageWidth / 32.0;

            switch (params.robotOffsetPointMode) {
                case Single:
                    if (!params.singleOffsetPoint.isEmpty()) {
                        x = params.singleOffsetPoint.getFirst();
                        y = params.singleOffsetPoint.getSecond();
                    }
                    break;
                case Dual:
                    var beginningPoint = in.getRight().get(0) != null ? in.getRight().get(0).getRobotOffsetPoint() : new Point(0, 0);
                    var dualOffsetPoint = TargetCalculations.calculateRobotOffsetPoint(beginningPoint, camCenterPoint, params.dualOffsetLineValues, RobotOffsetPointMode.Dual);
                    // TODO: draw crosshair based on dual calibration
                    break;
            }

            Point xMax = new Point(x + scale, y);
            Point xMin = new Point(x - scale, y);
            Point yMax = new Point(x, y + scale);
            Point yMin = new Point(x, y - scale);

            Imgproc.line(image, xMax, xMin, ColorHelper.colorToScalar(params.crosshairColor));
            Imgproc.line(image, yMax, yMin, ColorHelper.colorToScalar(params.crosshairColor));
        }
        return null;
    }

    public static class Draw2dCrosshairParams {
        public boolean showCrosshair = true;
        public Color crosshairColor = Color.GREEN;

        public final boolean shouldDraw;
        public final FrameStaticProperties frameStaticProperties;
        public final RobotOffsetPointMode robotOffsetPointMode;
        public final DoubleCouple singleOffsetPoint;
        public final DoubleCouple dualOffsetLineValues;

        public Draw2dCrosshairParams(FrameStaticProperties frameStaticProperties) {
            shouldDraw = true;
            this.frameStaticProperties = frameStaticProperties;
            robotOffsetPointMode = RobotOffsetPointMode.None;
            singleOffsetPoint = new DoubleCouple();
            dualOffsetLineValues = new DoubleCouple();
        }

        public Draw2dCrosshairParams(
                boolean shouldDraw,
                FrameStaticProperties frameStaticProperties,
                RobotOffsetPointMode robotOffsetPointMode,
                DoubleCouple singleOffsetPoint,
                DoubleCouple dualOffsetLineValues) {
            this.shouldDraw = shouldDraw;
            this.frameStaticProperties = frameStaticProperties;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.singleOffsetPoint = singleOffsetPoint;
            this.dualOffsetLineValues = dualOffsetLineValues;
        }
    }
}
