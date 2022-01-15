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
import java.util.stream.Collectors;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.opencv.Contour;
import org.photonvision.vision.pipe.CVPipe;

public class FindContoursPipe
        extends CVPipe<Mat, List<Contour>, FindContoursPipe.FindContoursParams> {
    private final List<MatOfPoint> m_foundContours = new ArrayList<>();

    @Override
    protected List<Contour> process(Mat in) {
        for (var m : m_foundContours) {
            m.release(); // necessary?
        }
        m_foundContours.clear();

        Imgproc.findContours(
                in, m_foundContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_KCOS);

        return m_foundContours.stream().map(Contour::new).collect(Collectors.toList());
    }

    public static class FindContoursParams {}
}
