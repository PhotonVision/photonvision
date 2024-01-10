package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.rknn.RknnJNI;
import org.photonvision.rknn.RknnJNI.RknnResult;
import org.opencv.core.Rect2d;
import org.opencv.core.Point;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

public class RknnDetector extends PhotonJNICommon {
    private static final Logger logger = new Logger(RknnDetector.class, LogGroup.General);
    static long objPointer = -1;
    static boolean hasBeenDestroyed = false;
    private boolean isLoaded;
    private static RknnDetector instance = null;
    private RknnDetector() {
        isLoaded = false;
    }
    public static RknnDetector getRknnDetector() {
        if (instance == null)
            instance = new RknnDetector();
 
        return instance;
    }
    public static void createRknnDetector() {
        objPointer = RknnJNI.create("/home/mdurrani808/photon-testing/photonvision/photon-core/src/main/java/org/photonvision/jni/yolov5s-640-640.rknn");
    }
    public static synchronized void forceLoad() throws IOException {
        TestUtils.loadLibraries();
        
        forceLoad(getRknnDetector(), RknnDetector.class, List.of("rga", "rknnrt", "rknn_jni"));
        createRknnDetector();
    }
    public static List<NeuralNetworkPipeResult> detect(CVMat in) {
        RknnResult[] ret = RknnJNI.detect(objPointer, in.getMat().getNativeObjAddr());

        return List.of(ret).stream().map(it->new NeuralNetworkPipeResult(
            it.rect, it.class_id, it.conf
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
    @Override
    public boolean isLoaded() {
        return isLoaded;
    }
    @Override
    public void setLoaded(boolean state) {
        isLoaded = state;
    }
    
}