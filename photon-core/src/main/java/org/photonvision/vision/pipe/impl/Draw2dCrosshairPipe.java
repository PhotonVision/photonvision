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

import java.awt.Color;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.opencv.ImageRotationMode;
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

        var image = in.getLeft();

        if (params.showCrosshair) {
            double x = params.frameStaticProperties.centerX;
            double y = params.frameStaticProperties.centerY;
            double scale = params.frameStaticProperties.imageWidth / (double) params.divisor.value / 32.0;

            if (this.params.rotMode == ImageRotationMode.DEG_270
                    || this.params.rotMode == ImageRotationMode.DEG_90) {
                var tmp = x;
                x = y;
                y = tmp;
            }

            switch (params.robotOffsetPointMode) {
                case Single:
                    if (params.singleOffsetPoint.x != 0 && params.singleOffsetPoint.y != 0) {
                        x = params.singleOffsetPoint.x;
                        y = params.singleOffsetPoint.y;
                    }
                    break;
                case Dual:
                    if (in.getRight().size() >= 1) {
                        var target = in.getRight().get(0);
                        if (target != null) {
                            var area = target.getArea();
                            var offsetCrosshair =
                                    TargetCalculations.calculateDualOffsetCrosshair(params.dualOffsetValues, area);
                            x = offsetCrosshair.x;
                            y = offsetCrosshair.y;
                        }
                    }
                    break;
            }

            x /= (double) params.divisor.value;
            y /= (double) params.divisor.value;

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
        public final ImageRotationMode rotMode;
        public final RobotOffsetPointMode robotOffsetPointMode;
        public final Point singleOffsetPoint;
        public final DualOffsetValues dualOffsetValues;
        private final FrameDivisor divisor;

        public Draw2dCrosshairParams(
                FrameStaticProperties frameStaticProperties,
                FrameDivisor divisor,
                ImageRotationMode rotMode) {
            shouldDraw = true;
            this.frameStaticProperties = frameStaticProperties;
            this.rotMode = rotMode;
            robotOffsetPointMode = RobotOffsetPointMode.None;
            singleOffsetPoint = new Point();
            dualOffsetValues = new DualOffsetValues();
            this.divisor = divisor;
        }

        public Draw2dCrosshairParams(
                boolean shouldDraw,
                RobotOffsetPointMode robotOffsetPointMode,
                Point singleOffsetPoint,
                DualOffsetValues dualOffsetValues,
                FrameStaticProperties frameStaticProperties,
                FrameDivisor divisor,
                ImageRotationMode rotMode) {
            this.shouldDraw = shouldDraw;
            this.frameStaticProperties = frameStaticProperties;
            this.robotOffsetPointMode = robotOffsetPointMode;
            this.singleOffsetPoint = singleOffsetPoint;
            this.dualOffsetValues = dualOffsetValues;
            this.divisor = divisor;
            this.rotMode = rotMode;
        }
    }
}
