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
import java.util.Optional;
import org.opencv.core.Mat;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.NullModel;
import org.photonvision.vision.objects.ObjectDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class ObjectDetectionPipe
        extends CVPipe<
                CVMat, List<NeuralNetworkPipeResult>, ObjectDetectionPipe.ObjectDetectionPipeParams>
        implements Releasable {
    private ObjectDetector detector;

    public ObjectDetectionPipe() {
        Optional<Model> defaultModel = NeuralNetworkModelManager.getInstance().getDefaultModel();
        detector = defaultModel.map(Model::load).orElse(NullModel.getInstance());
    }

    @Override
    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        // Check if the model has changed
        if (detector.getModel() != params.model) {
            detector.release();
            detector = params.model.load();
        }

        Mat frame = in.getMat();
        if (frame.empty()) {
            return List.of();
        }

        return detector.detect(in.getMat(), params.nms, params.confidence);
    }

    public static class ObjectDetectionPipeParams {
        public double confidence;
        public double nms;
        public int max_detections;
        public Model model;

        public ObjectDetectionPipeParams() {}
    }

    public List<String> getClassNames() {
        return detector.getClasses();
    }

    @Override
    public void release() {
        detector.release();
    }
}
