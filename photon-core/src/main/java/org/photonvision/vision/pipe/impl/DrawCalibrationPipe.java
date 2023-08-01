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
import org.photonvision.vision.pipe.MutatingPipe;
import org.photonvision.vision.target.TrackedTarget;

public class DrawCalibrationPipe
        extends MutatingPipe<
                Pair<Mat, List<TrackedTarget>>, DrawCalibrationPipe.DrawCalibrationPipeParams> {
    @Override
    protected Void process(Pair<Mat, List<TrackedTarget>> in) {

        var image = in.getLeft();

        for (var target : in.getRight()) {

            for (var c : target.getTargetCorners()) {
                c = new Point(c.x / params.divisor.value.doubleValue(), c.y / params.divisor.value.doubleValue());
                var r = 4;
                var r2 = r / Math.sqrt(2);
                var color = ColorHelper.colorToScalar(Color.RED, 0.4);
                Imgproc.circle(image, c, r, color, 1);
                Imgproc.line(image, 
                    new Point(c.x-r2, c.y-r2),
                    new Point(c.x+r2, c.y+r2),
                    color);
                Imgproc.line(image, 
                    new Point(c.x+r2, c.y-r2),
                    new Point(c.x-r2, c.y+r2),
                    color);
            }

        }
            
        return null;
    }

    public static class DrawCalibrationPipeParams {
        private final FrameDivisor divisor;

        public DrawCalibrationPipeParams(
                FrameDivisor divisor) {
            this.divisor = divisor;
        }
    }
}
