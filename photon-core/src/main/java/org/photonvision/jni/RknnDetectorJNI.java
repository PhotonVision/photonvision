package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.rknn.RknnJNI;
import org.photonvision.rknn.RknnJNI.RknnResult;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

public class RknnDetectorJNI extends PhotonJNICommon {
    private static final Logger logger = new Logger(RknnDetectorJNI.class, LogGroup.General);
    static long objPointer = -1;
    static boolean hasBeenDestroyed = false;
    private boolean isLoaded;
    private static RknnDetectorJNI instance = null;

    private RknnDetectorJNI() {
        isLoaded = false;
    }

    public static RknnDetectorJNI getInstance() {
        if (instance == null) instance = new RknnDetectorJNI();

        return instance;
    }

    public static void createRknnDetector() {
        objPointer =
                RknnJNI.create(
                        NeuralNetworkModelManager.getInstance()
                                .getDefaultRknnModel()
                                .getAbsolutePath()
                                .toString(),
                        NeuralNetworkModelManager.getInstance().getLabels().size());
    }

    public static synchronized void forceLoad() throws IOException {
        TestUtils.loadLibraries();

        forceLoad(getInstance(), RknnDetectorJNI.class, List.of("rga", "rknnrt", "rknn_jni"));
    }

    public static List<NeuralNetworkPipeResult> detect(CVMat in, double nmsThresh, double boxThresh) {
        RknnResult[] ret =
                RknnJNI.detect(objPointer, in.getMat().getNativeObjAddr(), nmsThresh, boxThresh);
        if (ret == null) {
            return List.of();
        }
        return List.of(ret).stream()
                .map(it -> new NeuralNetworkPipeResult(it.rect, it.class_id, it.conf))
                .collect(Collectors.toList());
    }

    public static void release() {
        if (!hasBeenDestroyed) {
            RknnJNI.destroy(objPointer);
            hasBeenDestroyed = true;
        } else {
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

    public static List<String> getClasses() {
        return NeuralNetworkModelManager.getInstance().getLabels();
    }
}
