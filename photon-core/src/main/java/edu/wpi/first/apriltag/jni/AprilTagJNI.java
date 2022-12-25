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

package edu.wpi.first.apriltag.jni;

import edu.wpi.first.util.RuntimeLoader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;

public class AprilTagJNI {
    static boolean libraryLoaded = false;

    static RuntimeLoader<AprilTagJNI> loader = null;

    public static class Helper {
        private static AtomicBoolean extractOnStaticLoad = new AtomicBoolean(true);

        public static boolean getExtractOnStaticLoad() {
            return extractOnStaticLoad.get();
        }

        public static void setExtractOnStaticLoad(boolean load) {
            extractOnStaticLoad.set(load);
        }
    }

    static {
        if (Helper.getExtractOnStaticLoad()) {
            try {
                loader =
                        new RuntimeLoader<>(
                                "apriltagjni", RuntimeLoader.getDefaultExtractionRoot(), AprilTagJNI.class);
                loader.loadLibrary();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            libraryLoaded = true;
        }
    }

    // Returns a pointer to a apriltag_detector_t
    public static native long aprilTagCreate(
            String fam, double decimate, double blur, int threads, boolean debug, boolean refine_edges);

    // Destroy and free a previously created detector.
    public static native void aprilTagDestroy(long detector);

    private static native Object[] aprilTagDetectInternal(
            long detector,
            long imgAddr,
            int rows,
            int cols,
            boolean doPoseEstimation,
            double tagWidth,
            double fx,
            double fy,
            double cx,
            double cy,
            int nIters);

    // Detect targets given a GRAY frame. Returns a pointer toa zarray
    public static DetectionResult[] aprilTagDetect(
            long detector,
            Mat img,
            boolean doPoseEstimation,
            double tagWidth,
            double fx,
            double fy,
            double cx,
            double cy,
            int nIters) {
        return (DetectionResult[])
                aprilTagDetectInternal(
                        detector,
                        img.dataAddr(),
                        img.rows(),
                        img.cols(),
                        doPoseEstimation,
                        tagWidth,
                        fx,
                        fy,
                        cx,
                        cy,
                        nIters);
    }
}
