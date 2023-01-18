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

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"cameraExtrinsics", "distCoeffs"})
    public final JsonMat distCoeffs;

    @JsonProperty("perViewErrors")
    public final double[] perViewErrors;

    @JsonProperty("standardDeviation")
    public final double standardDeviation;

    @JsonProperty("isFisheye")
    public final boolean isFisheye;

    @JsonCreator
    public CameraCalibrationCoefficients(
            @JsonProperty("resolution") Size resolution,
            @JsonProperty("cameraIntrinsics") JsonMat cameraIntrinsics,
            @JsonProperty("cameraExtrinsics") JsonMat distCoeffs,
            @JsonProperty("perViewErrors") double[] perViewErrors,
            @JsonProperty("standardDeviation") double standardDeviation,
            @JsonProperty("isFisheye") boolean isFisheye) {
        this.resolution = resolution;
        this.cameraIntrinsics = cameraIntrinsics;
        this.distCoeffs = distCoeffs;
        this.perViewErrors = perViewErrors;
        this.standardDeviation = standardDeviation;
        this.isFisheye = isFisheye;
    }

    @JsonIgnore
    public Mat getCameraIntrinsicsMat() {
        return cameraIntrinsics.getAsMat();
    }

    @JsonIgnore
    public MatOfDouble getDistCoeffsMat() {
        return distCoeffs.getAsMatOfDouble();
    }

    @JsonIgnore
    public double[] getPerViewErrors() {
        return perViewErrors;
    }

    @JsonIgnore
    public double getStandardDeviation() {
        return standardDeviation;
    }

    @JsonIgnore
    public boolean getisFisheye() {
        return isFisheye;
    }

    @Override
    public void release() {
        cameraIntrinsics.release();
        distCoeffs.release();
    }
}
