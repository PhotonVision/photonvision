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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.photonvision.vision.opencv.Releasable;

public class CameraCalibrationCoefficients implements Releasable {
    @JsonProperty("resolution")
    public final Size resolution;

    @JsonProperty("cameraIntrinsics")
    public final JsonMat cameraIntrinsics;

    @JsonProperty("cameraExtrinsics")
    public final JsonMat cameraExtrinsics;

    @JsonProperty("perViewErrors")
    public final double[] perViewErrors;

    @JsonProperty("standardDeviation")
    public final double standardDeviation;

    @JsonCreator
    public CameraCalibrationCoefficients(
            @JsonProperty("resolution") Size resolution,
            @JsonProperty("cameraIntrinsics") JsonMat cameraIntrinsics,
            @JsonProperty("cameraExtrinsics") JsonMat cameraExtrinsics,
            @JsonProperty("perViewErrors") double[] perViewErrors,
            @JsonProperty("standardDeviation") double standardDeviation) {
        this.resolution = resolution;
        this.cameraIntrinsics = cameraIntrinsics;
        this.cameraExtrinsics = cameraExtrinsics;
        this.perViewErrors = perViewErrors;
        this.standardDeviation = standardDeviation;
    }

    @JsonIgnore
    public Mat getCameraIntrinsicsMat() {
        return cameraIntrinsics.getAsMat();
    }

    @JsonIgnore
    public MatOfDouble getCameraExtrinsicsMat() {
        return cameraExtrinsics.getAsMatOfDouble();
    }

    @JsonIgnore
    public double[] getPerViewErrors() {
        return perViewErrors;
    }

    @JsonIgnore
    public double getStandardDeviation() {
        return standardDeviation;
    }

    @Override
    public void release() {
        cameraIntrinsics.release();
        cameraExtrinsics.release();
    }
}
