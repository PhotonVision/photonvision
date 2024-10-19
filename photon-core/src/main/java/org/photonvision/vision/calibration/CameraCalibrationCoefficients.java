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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.opencv.Releasable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CameraCalibrationCoefficients implements Releasable {
    @JsonProperty("resolution")
    public final Size unrotatedImageSize;

    @JsonProperty("cameraIntrinsics")
    public final JsonMatOfDouble cameraIntrinsics;

    @JsonProperty("distCoeffs")
    @JsonAlias({"distCoeffs", "distCoeffs"})
    public final JsonMatOfDouble distCoeffs;

    @JsonProperty("observations")
    public final List<BoardObservation> observations;

    @JsonProperty("calobjectWarp")
    public final double[] calobjectWarp;

    @JsonProperty("calobjectSize")
    public final Size calobjectSize;

    @JsonProperty("calobjectSpacing")
    public final double calobjectSpacing;

    @JsonProperty("lensmodel")
    public final CameraLensModel lensmodel;

    /**
     * Contains all camera calibration data for a particular resolution of a camera. Designed for use
     * with standard opencv camera calibration matrices. For details on the layout of camera
     * intrinsics/distortion matrices, see:
     * https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html#ga3207604e4b1a1758aa66acb6ed5aa65d
     *
     * @param resolution The resolution this applies to. We don't assume camera binning or try
     *     rescaling calibration
     * @param cameraIntrinsics Camera intrinsics parameters matrix, in the standard opencv form.
     * @param distCoeffs Camera distortion coefficients array. Variable length depending on order of
     *     distortion model
     * @param calobjectWarp Board deformation parameters, for calibrators that can estimate that. See:
     *     https://mrcal.secretsauce.net/formulation.html#board-deformation
     * @param observations List of snapshots used to construct this calibration
     * @param calobjectSize Dimensions of the object used to calibrate, in # of squares in
     *     width/height
     * @param calobjectSpacing Spacing between adjacent squares, in meters
     */
    @JsonCreator
    public CameraCalibrationCoefficients(
            @JsonProperty("resolution") Size resolution,
            @JsonProperty("cameraIntrinsics") JsonMatOfDouble cameraIntrinsics,
            @JsonProperty("distCoeffs") JsonMatOfDouble distCoeffs,
            @JsonProperty("calobjectWarp") double[] calobjectWarp,
            @JsonProperty("observations") List<BoardObservation> observations,
            @JsonProperty("calobjectSize") Size calobjectSize,
            @JsonProperty("calobjectSpacing") double calobjectSpacing,
            @JsonProperty("lensmodel") CameraLensModel lensmodel) {
        this.unrotatedImageSize = resolution;
        this.cameraIntrinsics = cameraIntrinsics;
        this.distCoeffs = distCoeffs;
        this.calobjectWarp = calobjectWarp;
        this.calobjectSize = calobjectSize;
        this.calobjectSpacing = calobjectSpacing;
        this.lensmodel = lensmodel;

        // Legacy migration just to make sure that observations is at worst empty and never null
        if (observations == null) {
            observations = List.of();
        }
        this.observations = observations;
    }

    public CameraCalibrationCoefficients rotateCoefficients(ImageRotationMode rotation) {
        if (rotation == ImageRotationMode.DEG_0) {
            return this;
        }
        Mat rotatedIntrinsics = getCameraIntrinsicsMat().clone();
        Mat rotatedDistCoeffs = getDistCoeffsMat().clone();
        double cx = getCameraIntrinsicsMat().get(0, 2)[0];
        double cy = getCameraIntrinsicsMat().get(1, 2)[0];
        double fx = getCameraIntrinsicsMat().get(0, 0)[0];
        double fy = getCameraIntrinsicsMat().get(1, 1)[0];

        // only adjust p1 and p2 the rest are radial distortion coefficients

        double p1 = getDistCoeffsMat().get(0, 2)[0];
        double p2 = getDistCoeffsMat().get(0, 3)[0];

        // A bunch of horrifying opaque rotation black magic. See image-rotation.md for more details.
        switch (rotation) {
            case DEG_0:
                break;
            case DEG_270_CCW:
                // FX
                rotatedIntrinsics.put(0, 0, fy);
                // FY
                rotatedIntrinsics.put(1, 1, fx);

                // CX
                rotatedIntrinsics.put(0, 2, unrotatedImageSize.height - cy);
                // CY
                rotatedIntrinsics.put(1, 2, cx);

                // P1
                rotatedDistCoeffs.put(0, 2, p2);
                // P2
                rotatedDistCoeffs.put(0, 3, -p1);

                break;
            case DEG_180_CCW:
                // CX
                rotatedIntrinsics.put(0, 2, unrotatedImageSize.width - cx);
                // CY
                rotatedIntrinsics.put(1, 2, unrotatedImageSize.height - cy);

                // P1
                rotatedDistCoeffs.put(0, 2, -p1);
                // P2
                rotatedDistCoeffs.put(0, 3, -p2);
                break;
            case DEG_90_CCW:
                // FX
                rotatedIntrinsics.put(0, 0, fy);
                // FY
                rotatedIntrinsics.put(1, 1, fx);

                // CX
                rotatedIntrinsics.put(0, 2, cy);
                // CY
                rotatedIntrinsics.put(1, 2, unrotatedImageSize.width - cx);

                // P1
                rotatedDistCoeffs.put(0, 2, -p2);
                // P2
                rotatedDistCoeffs.put(0, 3, p1);

                break;
        }

        JsonMatOfDouble newIntrinsics = JsonMatOfDouble.fromMat(rotatedIntrinsics);

        JsonMatOfDouble newDistCoeffs = JsonMatOfDouble.fromMat(rotatedDistCoeffs);

        rotatedIntrinsics.release();
        rotatedDistCoeffs.release();

        var rotatedImageSize = new Size(unrotatedImageSize.height, unrotatedImageSize.width);

        return new CameraCalibrationCoefficients(
                rotatedImageSize,
                newIntrinsics,
                newDistCoeffs,
                calobjectWarp,
                observations,
                calobjectSize,
                calobjectSpacing,
                lensmodel);
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
    public double[] getIntrinsicsArr() {
        return cameraIntrinsics.data;
    }

    @JsonIgnore
    public double[] getDistCoeffsArr() {
        return distCoeffs.data;
    }

    @JsonIgnore
    public List<BoardObservation> getPerViewErrors() {
        return observations;
    }

    @Override
    public void release() {
        cameraIntrinsics.release();
        distCoeffs.release();
    }

    @Override
    public String toString() {
        return "CameraCalibrationCoefficients [resolution="
                + unrotatedImageSize
                + ", cameraIntrinsics="
                + cameraIntrinsics
                + ", distCoeffs="
                + distCoeffs
                + ", observationslen="
                + observations.size()
                + ", calobjectWarp="
                + Arrays.toString(calobjectWarp)
                + "]";
    }

    public UICameraCalibrationCoefficients cloneWithoutObservations() {
        return new UICameraCalibrationCoefficients(
                unrotatedImageSize,
                cameraIntrinsics,
                distCoeffs,
                calobjectWarp,
                observations,
                calobjectSize,
                calobjectSpacing,
                lensmodel);
    }
}
