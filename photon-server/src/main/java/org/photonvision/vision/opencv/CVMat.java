/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.vision.opencv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
