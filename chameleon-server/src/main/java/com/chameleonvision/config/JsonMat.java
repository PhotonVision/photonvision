package com.chameleonvision.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Arrays;


public class JsonMat {
    public final int rows;
    public final int cols;
    public final int type;
    public final double[] data;

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

    public Mat toMat() {
        return toMat(this);
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

    public static double[] getDataFromMat(Mat mat) {
        if (!isCalibrationMat(mat)) return null;

        double[] data = new double[(int)(mat.total()*mat.elemSize())];
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

    public static Mat toMat(JsonMat jsonMat) {
        if (jsonMat.type != CvType.CV_64FC1) return null;

        Mat retMat = new Mat(jsonMat.rows, jsonMat.cols, jsonMat.type);
        retMat.put(0, 0, jsonMat.data);
        return retMat;
    }
}
