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

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Rect2d;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.NullModel;
import org.photonvision.vision.objects.ObjectDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

/**
 * Pipe that runs YOLO model on NPU to detect AprilTag bounding boxes in the full frame. This is
 * Stage 1 of the ML-assisted AprilTag detection pipeline.
 */
public class AprilTagROIDetectionPipe
        extends CVPipe<CVMat, List<Rect2d>, AprilTagROIDetectionPipe.AprilTagROIDetectionParams>
        implements Releasable {

    private ObjectDetector detector;
    private Model currentModel;

    public static class AprilTagROIDetectionParams {
        public final Model model;
        public final double confidenceThreshold;
        public final double nmsThreshold;

        public AprilTagROIDetectionParams(Model model, double confidence, double nms) {
            this.model = model;
            this.confidenceThreshold = confidence;
            this.nmsThreshold = nms;
        }
    }

    public AprilTagROIDetectionPipe() {
        detector = NullModel.getInstance();
    }

    @Override
    protected List<Rect2d> process(CVMat in) {
        List<Rect2d> rois = new ArrayList<>();

        if (detector == null || detector instanceof NullModel) {
            return rois; // Empty list - fallback to traditional detection
        }

        if (in.getMat().empty()) {
            return rois;
        }

        List<NeuralNetworkPipeResult> detections =
                detector.detect(in.getMat(), params.nmsThreshold, params.confidenceThreshold);

        for (NeuralNetworkPipeResult detection : detections) {
            rois.add(detection.bbox());
        }

        return rois;
    }

    @Override
    public void setParams(AprilTagROIDetectionParams newParams) {
        if (newParams.model != null && newParams.model != currentModel) {
            if (detector != null && !(detector instanceof NullModel)) {
                detector.release();
            }
            detector = newParams.model.load();
            currentModel = newParams.model;
        }
        super.setParams(newParams);
    }

    /**
     * Returns whether ML detection is available (a valid model is loaded).
     *
     * @return true if ML detection is available
     */
    public boolean isAvailable() {
        return detector != null && !(detector instanceof NullModel);
    }

    @Override
    public void release() {
        if (detector != null && !(detector instanceof NullModel)) {
            detector.release();
            detector = null;
        }
    }
}
