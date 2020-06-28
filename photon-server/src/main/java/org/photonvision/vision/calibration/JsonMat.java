package org.photonvision.vision.calibration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.photonvision.vision.opencv.Releasable;

public class JsonMat implements Releasable {
    public final int rows;
    public final int cols;
    public final int type;
    public final double[] data;

    @JsonIgnore private Mat wrappedMat;
    private MatOfDouble wrappedMatOfDouble;

    public JsonMat(int rows, int cols, double[] data) {
        this(rows, cols, CvType.CV_64FC1, data);
    }

    public JsonMat(
            @JsonProperty("rows") int rows,
            @JsonProperty("cols") int cols,
            @JsonProperty("type") int type,
            @JsonProperty("data") double[] data) {
        this.rows = rows;
        this.cols = cols;
        this.type = type;
        this.data = data;
    }

    private static boolean isCameraMatrixMat(Mat mat) {
        return mat.type() == CvType.CV_64FC1 && mat.cols() == 3 && mat.rows() == 3;
    }

    private static boolean isDistortionCoeffsMat(Mat mat) {
        return mat.type() == CvType.CV_64FC1 && mat.cols() == 5 && mat.rows() == 1;
    }

    private static boolean isCalibrationMat(Mat mat) {
        return isDistortionCoeffsMat(mat) || isCameraMatrixMat(mat);
    }

    @JsonIgnore
    public static double[] getDataFromMat(Mat mat) {
        if (!isCalibrationMat(mat)) return null;

        double[] data = new double[(int) (mat.total() * mat.elemSize())];
        mat.get(0, 0, data);

        int dataLen = -1;

        if (isCameraMatrixMat(mat)) dataLen = 9;
        if (isDistortionCoeffsMat(mat)) dataLen = 5;

        // truncate Mat data to correct number data points.
        return Arrays.copyOfRange(data, 0, dataLen);
    }

    public static JsonMat fromMat(Mat mat) {
        if (!isCalibrationMat(mat)) return null;
        return new JsonMat(mat.rows(), mat.cols(), getDataFromMat(mat));
    }

    @JsonIgnore
    public Mat getAsMat() {
        if (this.type != CvType.CV_64FC1) return null;

        if (wrappedMat == null) {
            this.wrappedMat = new Mat(this.rows, this.cols, this.type);
            this.wrappedMat.put(0, 0, this.data);
        }
        return this.wrappedMat;
    }

    @JsonIgnore
    public MatOfDouble getAsMatOfDouble() {
        if (this.wrappedMatOfDouble == null) {
            this.wrappedMatOfDouble = new MatOfDouble();
            getAsMat().convertTo(wrappedMatOfDouble, CvType.CV_64F);
        }
        return this.wrappedMatOfDouble;
    }

    @Override
    public void release() {
        getAsMat().release();
    }
}
