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

package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.ArucoNanoV5Detector;
import org.photonvision.ArucoNanoV5Detector.DetectionResult;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.opencv.CVMat;

public class ArucoNanoDetectorJNI extends PhotonJNICommon {
    private boolean isLoaded;
    private static ArucoNanoDetectorJNI instance = null;

    private ArucoNanoDetectorJNI() {
        isLoaded = false;
    }

    public static ArucoNanoDetectorJNI getInstance() {
        if (instance == null) instance = new ArucoNanoDetectorJNI();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        forceLoad(getInstance(), ArucoNanoDetectorJNI.class, List.of("photonmiscjnijni"));
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void setLoaded(boolean state) {
        isLoaded = state;
    }

    public static List<ArucoDetectionResult> detect(CVMat in) {
        DetectionResult[] ret = ArucoNanoV5Detector.detect(in.getMat().getNativeObjAddr(), 0);

        return List.of(ret).stream()
                .map(it -> new ArucoDetectionResult(it.xCorners, it.yCorners, it.id))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        TestUtils.loadLibraries();
        forceLoad();
    }
}
