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

import edu.wpi.first.math.Pair;
import java.util.List;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.MutatingPipe;

/**
 * Draws ML detection ROI bounding boxes on the output image. Used to visualize where the ML model
 * detected potential targets before traditional decoding.
 */
public class DrawMLROIPipe
        extends MutatingPipe<Pair<Mat, List<Rect2d>>, DrawMLROIPipe.DrawMLROIParams> {

    private static final Scalar ROI_COLOR = new Scalar(255, 255, 0); // Cyan in BGR

    @Override
    protected Void process(Pair<Mat, List<Rect2d>> in) {
        if (!params.shouldDraw || !params.showDetectionBoxes) return null;

        var mat = in.getFirst();
        var rois = in.getSecond();
        if (rois == null || rois.isEmpty()) return null;

        var imageSize = Math.sqrt((double) mat.rows() * mat.cols());
        var thickness = (int) Math.ceil(imageSize * 0.007);

        for (Rect2d roi : rois) {
            double x = roi.x / params.divisor.value;
            double y = roi.y / params.divisor.value;
            double w = roi.width / params.divisor.value;
            double h = roi.height / params.divisor.value;

            Imgproc.rectangle(
                    mat, new Point(x, y), new Point(x + w, y + h), ROI_COLOR, thickness);
        }

        return null;
    }

    public static class DrawMLROIParams {
        public final boolean shouldDraw;
        public final boolean showDetectionBoxes;
        public final FrameDivisor divisor;

        public DrawMLROIParams(
                boolean shouldDraw, boolean showDetectionBoxes, FrameDivisor divisor) {
            this.shouldDraw = shouldDraw;
            this.showDetectionBoxes = showDetectionBoxes;
            this.divisor = divisor;
        }
    }
}
