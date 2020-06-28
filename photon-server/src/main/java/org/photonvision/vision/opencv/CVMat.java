package org.photonvision.vision.opencv;

import java.util.HashSet;
import org.opencv.core.Mat;
import org.photonvision.common.util.ReflectionUtils;

public class CVMat implements Releasable {
    private static final HashSet<Mat> allMats = new HashSet<>();

    private static boolean shouldPrint;

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
        if (allMats.add(mat) && shouldPrint) {
            System.out.println(
                    "(CVMat) Added new Mat (count: "
                            + allMats.size()
                            + ") from: "
                            + ReflectionUtils.getNthCaller(3));
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

    public static void enablePrint(boolean enabled) {
        shouldPrint = enabled;
    }
}
