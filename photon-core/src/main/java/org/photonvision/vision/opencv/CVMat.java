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

package org.photonvision.vision.opencv;

import java.util.HashMap;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class CVMat implements Releasable {
    private static final Logger logger = new Logger(CVMat.class, LogGroup.General);

    private static int allMatCounter = 0;
    private static final HashMap<Mat, Integer> allMats = new HashMap<>();

    private static boolean shouldPrint;

    private final Mat mat;

    public CVMat() {
        this(new Mat());
    }

    public void copyTo(CVMat srcMat) {
        copyTo(srcMat.getMat());
    }

    public void copyTo(Mat srcMat) {
        srcMat.copyTo(mat);
    }

    private StringBuilder getStackTraceBuilder() {
        var trace = Thread.currentThread().getStackTrace();

        final int STACK_FRAMES_TO_SKIP = 3;
        final var traceStr = new StringBuilder();
        for (int idx = STACK_FRAMES_TO_SKIP; idx < trace.length; idx++) {
            traceStr.append("\t\n").append(trace[idx]);
        }
        traceStr.append("\n");
        return traceStr;
    }

    public CVMat(Mat mat) {
        this.mat = mat;
        allMatCounter++;
        allMats.put(mat, allMatCounter);

        if (shouldPrint) {
            logger.trace(() -> "CVMat" + allMatCounter + " alloc - new count: " + allMats.size());
            logger.trace(getStackTraceBuilder()::toString);
        }
    }

    @Override
    public void release() {
        // If this mat is empty, all we can do is return
        if (mat.empty()) return;

        // If the mat isn't in the hashmap, we can't remove it
        Integer matNo = allMats.get(mat);
        if (matNo != null) allMats.remove(mat);
        mat.release();

        if (shouldPrint) {
            logger.trace(() -> "CVMat" + matNo + " de-alloc - new count: " + allMats.size());
            logger.trace(getStackTraceBuilder()::toString);
        }
    }

    public Mat getMat() {
        return mat;
    }

    @Override
    public String toString() {
        return "CVMat{" + mat.toString() + '}';
    }

    public static int getMatCount() {
        return allMats.size();
    }

    public static void enablePrint(boolean enabled) {
        shouldPrint = enabled;
    }
}
