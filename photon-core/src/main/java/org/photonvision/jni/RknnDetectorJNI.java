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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.rknn.RknnJNI;
import org.photonvision.rknn.RknnJNI.RknnResult;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

public class RknnDetectorJNI extends PhotonJNICommon {
    private static final Logger logger = new Logger(RknnDetectorJNI.class, LogGroup.General);
    private boolean isLoaded;
    private static RknnDetectorJNI instance = null;

    private RknnDetectorJNI() {
        isLoaded = false;
    }

    public static RknnDetectorJNI getInstance() {
        if (instance == null) instance = new RknnDetectorJNI();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        TestUtils.loadLibraries();

        forceLoad(getInstance(), RknnDetectorJNI.class, List.of("rga", "rknnrt", "rknn_jni"));
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void setLoaded(boolean state) {
        isLoaded = state;
    }

    public static class RknnObjectDetector {
        long objPointer = -1;
        private List<String> labels;
        private final Object lock = new Object();
        private static final CopyOnWriteArrayList<Long> detectors = new CopyOnWriteArrayList<>();

        public RknnObjectDetector(String modelPath, List<String> labels, RknnJNI.ModelVersion version) {
            synchronized (lock) {
                objPointer = RknnJNI.create(modelPath, labels.size(), version.ordinal(), -1);
                detectors.add(objPointer);
                System.out.println(
                        "Created " + objPointer + "! Detectors: " + Arrays.toString(detectors.toArray()));
            }
            this.labels = labels;
        }

        public List<String> getClasses() {
            return labels;
        }

        /**
         * Detect forwards using this model
         *
         * @param in The image to process
         * @param nmsThresh Non-maximum supression threshold. Probably should not change
         * @param boxThresh Minimum confidence for a box to be added. Basically just confidence
         *     threshold
         */
        public List<NeuralNetworkPipeResult> detect(CVMat in, double nmsThresh, double boxThresh) {
            RknnResult[] ret;
            synchronized (lock) {
                // We can technically be asked to detect and the lock might be acquired _after_ release has
                // been called. This would mean objPointer would be invalid which would call everything to
                // explode.
                if (objPointer > 0) {
                    ret = RknnJNI.detect(objPointer, in.getMat().getNativeObjAddr(), nmsThresh, boxThresh);
                } else {
                    logger.warn("Detect called after destroy -- giving up");
                    return List.of();
                }
            }
            if (ret == null) {
                return List.of();
            }
            return List.of(ret).stream()
                    .map(it -> new NeuralNetworkPipeResult(it.rect, it.class_id, it.conf))
                    .collect(Collectors.toList());
        }

        public void release() {
            synchronized (lock) {
                if (objPointer > 0) {
                    RknnJNI.destroy(objPointer);
                    detectors.remove(objPointer);
                    System.out.println(
                            "Killed " + objPointer + "! Detectors: " + Arrays.toString(detectors.toArray()));
                    objPointer = -1;
                } else {
                    logger.error("RKNN Detector has already been destroyed!");
                }
            }
        }
    }

    // public static void createRknnDetector() {
    //     objPointer =
    //             RknnJNI.create(
    //                     NeuralNetworkModelManager.getInstance()
    //                             .getDefaultRknnModel()
    //                             .getAbsolutePath()
    //                             .toString(),
    //                     NeuralNetworkModelManager.getInstance().getLabels().size());
    // }
}
