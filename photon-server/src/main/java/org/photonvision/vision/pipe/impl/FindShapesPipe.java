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

import java.util.List;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.opencv.CVShape;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipe.CVPipe;

public class FindShapesPipe
        extends CVPipe<List<Contour>, List<CVShape>, FindShapesPipe.FindShapesParams> {

    MatOfPoint2f approxCurve = new MatOfPoint2f();

    @Override
    protected List<CVShape> process(List<Contour> in) {
        approxCurve.release();
        approxCurve = new MatOfPoint2f();

        for (var contour : in) {

            if (params.desiredShape == ContourShape.Circle) {

            } else {
                int desiredSides = params.desiredShape.sides;
                Imgproc.approxPolyDP(contour.getMat2f(), approxCurve, params.approxEpsilon, true);

                //                int actualSides = approxCurve.
                //                switch ()
                System.out.println("fugg");
            }
        }
        return List.of();
    }

    public static class FindShapesParams {
        double approxEpsilon = 0.05;
        ContourShape desiredShape;
    }
}
