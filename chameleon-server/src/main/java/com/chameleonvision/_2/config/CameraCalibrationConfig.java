package com.chameleonvision._2.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;

/** A class that holds a camera matrix and distortion coefficients for a given resolution */
public class CameraCalibrationConfig {
    @JsonProperty("resolution")
    public final Size resolution;

    @JsonProperty("cameraMatrix")
    public final JsonMat cameraMatrix;

    @JsonProperty("distortionCoeffs")
    public final JsonMat distortionCoeffs;

    @JsonProperty("squareSize")
    public final double squareSize;

    @JsonCreator
    public CameraCalibrationConfig(
            @JsonProperty("resolution") Size resolution,
            @JsonProperty("cameraMatrix") JsonMat cameraMatrix,
            @JsonProperty("distortionCoeffs") JsonMat distortionCoeffs,
            @JsonProperty("squareSize") double squareSize) {
        this.resolution = resolution;
        this.cameraMatrix = cameraMatrix;
        this.distortionCoeffs = distortionCoeffs;
        this.squareSize = squareSize;
    }

    public CameraCalibrationConfig(
            Size resolution, Mat cameraMatrix, Mat distortionCoeffs, double squareSize) {
        this.resolution = resolution;
        this.cameraMatrix = JsonMat.fromMat(cameraMatrix);
        this.distortionCoeffs = JsonMat.fromMat(distortionCoeffs);
        this.squareSize = squareSize;
    }

    @JsonIgnoreType
    public static class UICameraCalibrationConfig {
        public final int width;
        public final int height;
        public final double[] cameraMatrix;
        public final double[] distortionCoeffs;

        public UICameraCalibrationConfig(CameraCalibrationConfig config) {
            width = (int) config.resolution.width;
            height = (int) config.resolution.height;
            cameraMatrix = config.cameraMatrix.data;
            distortionCoeffs = config.distortionCoeffs.data;
        }
    }

    @JsonIgnore
    public Mat getCameraMatrixAsMat() {
        return cameraMatrix.getAsMat();
    }

    @JsonIgnore
    public MatOfDouble getDistortionCoeffsAsMat() {
        return new MatOfDouble(distortionCoeffs.getAsMat());
    }
}
