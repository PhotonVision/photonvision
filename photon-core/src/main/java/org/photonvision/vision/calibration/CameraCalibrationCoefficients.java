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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.photonvision.vision.opencv.Releasable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CameraCalibrationCoefficients implements Releasable {
    @JsonProperty("resolution")
    public final Size resolution;

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

    @JsonIgnore private final double[] intrinsicsArr = new double[9];
    @JsonIgnore private final double[] distCoeffsArr = new double[5];

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
        this.resolution = resolution;
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

        // do this once so gets are quick
        getCameraIntrinsicsMat().get(0, 0, intrinsicsArr);
        getDistCoeffsMat().get(0, 0, distCoeffsArr);
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
        return intrinsicsArr;
    }

    @JsonIgnore
    public double[] getDistCoeffsArr() {
        return distCoeffsArr;
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

    public static CameraCalibrationCoefficients parseFromCalibdbJson(JsonNode json) {
        // camera_matrix is a row major, array of arrays
        var cam_matrix = json.get("camera_matrix");

        double[] cam_arr =
                new double[] {
                    cam_matrix.get(0).get(0).doubleValue(),
                    cam_matrix.get(0).get(1).doubleValue(),
                    cam_matrix.get(0).get(2).doubleValue(),
                    cam_matrix.get(1).get(0).doubleValue(),
                    cam_matrix.get(1).get(1).doubleValue(),
                    cam_matrix.get(1).get(2).doubleValue(),
                    cam_matrix.get(2).get(0).doubleValue(),
                    cam_matrix.get(2).get(1).doubleValue(),
                    cam_matrix.get(2).get(2).doubleValue()
                };

        var dist_coefs = json.get("distortion_coefficients");

        double[] dist_array =
                new double[] {
                    dist_coefs.get(0).doubleValue(),
                    dist_coefs.get(1).doubleValue(),
                    dist_coefs.get(2).doubleValue(),
                    dist_coefs.get(3).doubleValue(),
                    dist_coefs.get(4).doubleValue(),
                };

        var cam_jsonmat = new JsonMatOfDouble(3, 3, cam_arr);
        var distortion_jsonmat = new JsonMatOfDouble(1, 5, dist_array);

        var width = json.get("img_size").get(0).doubleValue();
        var height = json.get("img_size").get(1).doubleValue();

        return new CameraCalibrationCoefficients(
                new Size(width, height),
                cam_jsonmat,
                distortion_jsonmat,
                new double[0],
                List.of(),
                new Size(0, 0),
                0,
                CameraLensModel.LENSMODEL_OPENCV);
    }

    @Override
    public String toString() {
        return "CameraCalibrationCoefficients [resolution="
                + resolution
                + ", cameraIntrinsics="
                + cameraIntrinsics
                + ", distCoeffs="
                + distCoeffs
                + ", observationslen="
                + observations.size()
                + ", calobjectWarp="
                + Arrays.toString(calobjectWarp)
                + ", intrinsicsArr="
                + Arrays.toString(intrinsicsArr)
                + ", distCoeffsArr="
                + Arrays.toString(distCoeffsArr)
                + "]";
    }

    public UICameraCalibrationCoefficients cloneWithoutObservations() {
        return new UICameraCalibrationCoefficients(
                resolution,
                cameraIntrinsics,
                distCoeffs,
                calobjectWarp,
                observations,
                calobjectSize,
                calobjectSpacing,
                lensmodel);
    }
}
