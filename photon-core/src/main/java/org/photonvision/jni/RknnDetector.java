package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.photonvision.rknn.RknnJNI;
import org.photonvision.rknn.RknnJNI.RknnResult;
import org.opencv.core.Rect2d;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

public class RknnDetector extends PhotonJNICommon {
    static long objPointer = 0;
    static boolean hasBeenDestroyed = false;
    public static synchronized void forceLoad() throws IOException {
        forceLoad(RknnDetector.class, List.of("rknn_jni", "rga", "rknnrt"));
    }
    
    public static List<NeuralNetworkPipeResult> detect(CVMat in) {
        RknnResult[] ret = RknnJNI.detect(objPointer, in.getMat().getNativeObjAddr());

        return List.of(ret).stream().map(it->new NeuralNetworkPipeResult(
            new Rect2d(it.left,it.top,it.right,it.bottom), it.class_id, it.conf
        )).collect(Collectors.toList());
    }
    public static void release() {
        if(!hasBeenDestroyed) {
            RknnJNI.destroy(objPointer);
            hasBeenDestroyed = true;
        }
        else {
            System.out.print("aint no way bros tryna release the RKNN detector again skull");
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        TestUtils.loadLibraries();
        forceLoad();
        objPointer = RknnJNI.create("null");
    }
}