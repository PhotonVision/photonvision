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

        public RknnObjectDetector(String modelPath, List<String> labels) {
            objPointer = RknnJNI.create(modelPath, labels.size());
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
            RknnResult[] ret =
                    RknnJNI.detect(objPointer, in.getMat().getNativeObjAddr(), nmsThresh, boxThresh);
            if (ret == null) {
                return List.of();
            }
            return List.of(ret).stream()
                    .map(it -> new NeuralNetworkPipeResult(it.rect, it.class_id, it.conf))
                    .collect(Collectors.toList());
        }

        public void release() {
            if (objPointer > 0) {
                RknnJNI.destroy(objPointer);
                objPointer = -1;
            } else {
                logger.error("RKNN Detector has already been destroyed!");
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
