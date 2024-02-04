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

package org.photonvision.vision.calibration;

import java.util.List;
import java.util.stream.Collectors;
import org.opencv.core.Size;

public class UICameraCalibrationCoefficients extends CameraCalibrationCoefficients {
    public int numSnapshots;
    public List<Double> meanErrors;

    public UICameraCalibrationCoefficients(
            Size resolution,
            JsonMatOfDouble cameraIntrinsics,
            JsonMatOfDouble distCoeffs,
            double[] calobjectWarp,
            List<BoardObservation> observations,
            Size calobjectSize,
            double calobjectSpacing,
            CameraLensModel lensmodel) {
        // yeet observations, keep all else
        super(
                resolution,
                cameraIntrinsics,
                distCoeffs,
                calobjectWarp,
                List.of(),
                calobjectSize,
                calobjectSpacing,
                lensmodel);

        this.numSnapshots = observations.size();
        this.meanErrors =
                observations.stream()
                        .map(
                                it2 ->
                                        it2.reprojectionErrors.stream()
                                                .mapToDouble(it -> Math.sqrt(it.x * it.x + it.y * it.y))
                                                .average()
                                                .orElse(0))
                        .collect(Collectors.toList());
    }
}
