package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.photonvision.rknn.RknnJNI;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.opencv.CVMat;

public class RknnDetector extends PhotonJNICommon {
    public static synchronized void forceLoad() throws IOException {
        forceLoad(RknnDetector.class, List.of("photonmiscjnijni"));
    }
    


    public static void main(String[] args) throws IOException {
        TestUtils.loadLibraries();
        forceLoad();
    }
}