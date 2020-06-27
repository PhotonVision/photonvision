package org.photonvision.common.calibration;

import org.photonvision.common.vision.opencv.Releasable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;

public class CameraCalibrationCoefficients implements Releasable {
    @JsonProperty("resolution")
    public final Size resolution;

    @JsonProperty("cameraIntrinsics")
    public final JsonMat cameraIntrinsics;

    @JsonProperty("cameraExtrinsics")
    public final JsonMat cameraExtrinsics;

    @JsonCreator
    public CameraCalibrationCoefficients(
            @JsonProperty("resolution") Size resolution,
            @JsonProperty("cameraIntrinsics") JsonMat cameraIntrinsics,
            @JsonProperty("cameraExtrinsics") JsonMat cameraExtrinsics) {
        this.resolution = resolution;
        this.cameraIntrinsics = cameraIntrinsics;
        this.cameraExtrinsics = cameraExtrinsics;
    }

    @JsonIgnore
    public Mat getCameraIntrinsicsMat() {
        return cameraIntrinsics.getAsMat();
    }

    @JsonIgnore
    public MatOfDouble getCameraExtrinsicsMat() {
        return cameraExtrinsics.getAsMatOfDouble();
    }

    @Override
    public void release() {
        cameraIntrinsics.release();
        cameraExtrinsics.release();
    }
}
