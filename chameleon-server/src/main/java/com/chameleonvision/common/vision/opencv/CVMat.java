package com.chameleonvision.common.vision.opencv;

import com.chameleonvision.common.util.ReflectionUtils;
import java.util.HashSet;
import org.opencv.core.Mat;

public class CVMat implements Releasable {
    private static final HashSet<Mat> allMats = new HashSet<>();

    private final Mat mat;

    public CVMat() {
        this.mat = new Mat();
    }

    public void copyTo(CVMat srcMat) {
        copyTo(srcMat.getMat());
    }

    public void copyTo(Mat srcMat) {
        srcMat.copyTo(mat);
    }

    public CVMat(Mat mat) {
        this.mat = mat;
        if (allMats.add(mat)) {
            System.out.println("(CVMat) Added new Mat from: \n" + ReflectionUtils.getNthCaller(3));
        }
    }

    @Override
    public void release() {
        allMats.remove(mat);
        mat.release();
    }

    public Mat getMat() {
        return mat;
    }

    public static int getMatCount() {
        return allMats.size();
    }
}
