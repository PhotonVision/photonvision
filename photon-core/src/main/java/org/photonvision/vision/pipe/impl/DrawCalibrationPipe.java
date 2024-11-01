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
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.MutatingPipe;
import org.photonvision.vision.target.TrackedTarget;

public class DrawCalibrationPipe
        extends MutatingPipe<
                Pair<Mat, List<TrackedTarget>>, DrawCalibrationPipe.DrawCalibrationPipeParams> {
    Scalar[] chessboardColors =
            new Scalar[] {
                ColorHelper.colorToScalar(Color.RED, 0.4),
                ColorHelper.colorToScalar(Color.ORANGE, 0.4),
                ColorHelper.colorToScalar(Color.GREEN, 0.4),
                ColorHelper.colorToScalar(Color.BLUE, 0.4),
                ColorHelper.colorToScalar(Color.MAGENTA, 0.4),
            };

    @Override
    protected Void process(Pair<Mat, List<TrackedTarget>> in) {
        if (!params.drawAllSnapshots) return null;

        var image = in.getLeft();

        var imgSz = image.size();
        var diag = Math.hypot(imgSz.width, imgSz.height);

        // heuristic: about 4px at a diagonal of 750px, or .5%, 'looks good'. keep it at least 3px at
        // worst tho
        int r = (int) Math.max(diag * 4.0 / 750.0, 3);
        int thickness = (int) Math.max(diag * 1.0 / 600.0, 1);

        int i = 0;
        for (var target : in.getRight()) {
            for (var c : target.getTargetCorners()) {
                if (c.x < 0 || c.y < 0) {
                    // Skip if the corner is less than zero
                    continue;
                }

                c =
                        new Point(
                                c.x / params.divisor.value.doubleValue(), c.y / params.divisor.value.doubleValue());

                var r2 = r / Math.sqrt(2);
                var color = chessboardColors[i % chessboardColors.length];
                Imgproc.circle(image, c, r, color, thickness);
                Imgproc.line(
                        image, new Point(c.x - r2, c.y - r2), new Point(c.x + r2, c.y + r2), color, thickness);
                Imgproc.line(
                        image, new Point(c.x + r2, c.y - r2), new Point(c.x - r2, c.y + r2), color, thickness);
            }

            i++;
        }

        return null;
    }

    public static class DrawCalibrationPipeParams {
        private final FrameDivisor divisor;
        public boolean drawAllSnapshots;

        public DrawCalibrationPipeParams(FrameDivisor divisor, boolean drawSnapshots) {
            this.divisor = divisor;
            this.drawAllSnapshots = drawSnapshots;
        }
    }
}
