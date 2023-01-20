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
import com.fasterxml.jackson.databind.JsonNode;
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

    @JsonCreator
    public CameraCalibrationCoefficients(
            @JsonProperty("resolution") Size resolution,
            @JsonProperty("cameraIntrinsics") JsonMat cameraIntrinsics,
            @JsonProperty("cameraExtrinsics") JsonMat distCoeffs,
            @JsonProperty("perViewErrors") double[] perViewErrors,
            @JsonProperty("standardDeviation") double standardDeviation) {
        this.resolution = resolution;
        this.cameraIntrinsics = cameraIntrinsics;
        this.distCoeffs = distCoeffs;
        this.perViewErrors = perViewErrors;
        this.standardDeviation = standardDeviation;
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
        
        double[] dist_array;
        JsonMat distortion_jsonmat;

        if(dist_coefs.size() == 4){
            dist_array =
            new double[] {
                dist_coefs.get(0).doubleValue(),
                dist_coefs.get(1).doubleValue(),
                dist_coefs.get(2).doubleValue(),
                dist_coefs.get(3).doubleValue()
            };

            distortion_jsonmat = new JsonMat(1, 4, dist_array);
        }
        else{
            dist_array =
            new double[] {
                dist_coefs.get(0).doubleValue(),
                dist_coefs.get(1).doubleValue(),
                dist_coefs.get(2).doubleValue(),
                dist_coefs.get(3).doubleValue(),
                dist_coefs.get(4).doubleValue()
            };

            distortion_jsonmat = new JsonMat(1, 5, dist_array);
        }

        var cam_jsonmat = new JsonMat(3, 3, cam_arr);

        var error = json.get("avg_reprojection_error").asDouble();
        var width = json.get("img_size").get(0).doubleValue();
        var height = json.get("img_size").get(1).doubleValue();

        return new CameraCalibrationCoefficients(
                new Size(width, height), cam_jsonmat, distortion_jsonmat, new double[] {error}, 0);
    }
}
