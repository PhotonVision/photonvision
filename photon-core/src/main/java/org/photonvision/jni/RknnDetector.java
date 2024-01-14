package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.rknn.RknnJNI;
import org.photonvision.rknn.RknnJNI.RknnResult;

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
    public static RknnDetector getInstance() {
        if (instance == null)
            instance = new RknnDetector();
 
        return instance;
    }
    public static void createRknnDetector() {
        objPointer = RknnJNI.create("/home/mdurrani808/photon-testing/photonvision/photon-core/src/main/java/org/photonvision/jni/yolov5s-640-640.rknn");
    }
    public static synchronized void forceLoad() throws IOException {
        TestUtils.loadLibraries();
        
        forceLoad(getInstance(), RknnDetector.class, List.of("rga", "rknnrt", "rknn_jni"));
        createRknnDetector();
    }
    public static List<NeuralNetworkPipeResult> detect(CVMat in) {
        RknnResult[] ret = RknnJNI.detect(objPointer, in.getMat().getNativeObjAddr());
        if(ret == null) { // this is yucky,,,why no return just 0?
            return List.of();
        }
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
            logger.error("RKNN Detector has already been destroyed!");
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