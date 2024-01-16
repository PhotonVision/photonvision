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

package org.photonvision.vision.pipe.impl;

import java.util.List;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.jni.RknnDetectorJNI.RknnObjectDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class RknnDetectionPipe
        extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, RknnDetectionPipe.RknnDetectionPipeParams>
        implements Releasable {
    private RknnObjectDetector detector;

    public RknnDetectionPipe() {
        // For now this is hard-coded to defaults. Should be refactored into set pipe params, though.
        // And ideally a little wrapper helper for only changing native stuff on content change created.
        this.detector =
                new RknnObjectDetector(
                        NeuralNetworkModelManager.getInstance().getDefaultRknnModel().getAbsolutePath(),
                        NeuralNetworkModelManager.getInstance().getLabels());
    }

    @Override
    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        var frame = in.getMat();

        // Make sure we don't get a weird empty frame
        if (frame.empty()) {
            return List.of();
        }

        return detector.detect(in, params.nms, params.confidence);
    }

    public static class RknnDetectionPipeParams {
        public double confidence;
        public double nms;
        public int max_detections;

        public RknnDetectionPipeParams() {}
    }

    public List<String> getClassNames() {
        return detector.getClasses();
    }

    @Override
    public void release() {
        detector.release();
    }
}
