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
import org.photonvision.vision.pipe.MutatingPipe;
import org.photonvision.vision.target.RobotOffsetPointMode;
import org.photonvision.vision.target.TrackedTarget;

public class Draw2dCrosshairPipe
        extends MutatingPipe<
                Pair<Mat, List<TrackedTarget>>, Draw2dCrosshairPipe.Draw2dCrosshairParams> {

    @Override
    protected Void process(Pair<Mat, List<TrackedTarget>> in) {
        if (!params.shouldDraw) return null;

        Mat image = in.getLeft();

        if (params.showCrosshair) {
            double x = image.cols() / 2.0;
            double y = image.rows() / 2.0;
            double scale = image.cols() / 32.0;

            switch (params.calibrationMode) {
                case Single:
                    if (!params.calibrationPoint.isEmpty()) {
                        x = params.calibrationPoint.getFirst();
                        y = params.calibrationPoint.getSecond();
                    }
                    break;
                case Dual:
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
        public final RobotOffsetPointMode calibrationMode;
        public final DoubleCouple calibrationPoint;

        public Draw2dCrosshairParams(
                boolean shouldDraw, RobotOffsetPointMode calibrationMode, DoubleCouple calibrationPoint) {
            this.shouldDraw = shouldDraw;
            this.calibrationMode = calibrationMode;
            this.calibrationPoint = calibrationPoint;
        }
    }
}
