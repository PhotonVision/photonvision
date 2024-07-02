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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.jni.RknnDetectorJNI.RknnObjectDetector;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class RknnDetectionPipe
        extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, RknnDetectionPipe.RknnDetectionPipeParams>
        implements Releasable {
    private RknnObjectDetector detector;

    public RknnDetectionPipe() {
        // For now this is hard-coded to defaults. Should be refactored into set pipe
        // params, though. And ideally a little wrapper helper for only changing native stuff on content
        // change created.
        this.detector =
                new RknnObjectDetector(NeuralNetworkModelManager.getInstance().getDefaultRknnModel());
    }

    private static class Letterbox {
        double dx;
        double dy;
        double scale;

        public Letterbox(double dx, double dy, double scale) {
            this.dx = dx;
            this.dy = dy;
            this.scale = scale;
        }
    }

    @Override
    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        var frame = in.getMat();

        // Make sure we don't get a weird empty frame
        if (frame.empty()) {
            return List.of();
        }

        // letterbox
        var letterboxed = new Mat();
        var scale =
                letterbox(frame, letterboxed, new Size(640, 640), ColorHelper.colorToScalar(Color.GRAY));

        if (letterboxed.width() != 640 || letterboxed.height() != 640) {
            // huh whack give up lol
            throw new RuntimeException("RGA bugged but still wrong size");
        }
        var ret = detector.detect(letterboxed, params.nms, params.confidence);
        letterboxed.release();

        return resizeDetections(ret, scale);
    }

    private List<NeuralNetworkPipeResult> resizeDetections(
            List<NeuralNetworkPipeResult> unscaled, Letterbox letterbox) {
        var ret = new ArrayList<NeuralNetworkPipeResult>();

        for (var t : unscaled) {
            var scale = 1.0 / letterbox.scale;
            var boundingBox = t.bbox;
            double x = (boundingBox.x - letterbox.dx) * scale;
            double y = (boundingBox.y - letterbox.dy) * scale;
            double width = boundingBox.width * scale;
            double height = boundingBox.height * scale;

            ret.add(
                    new NeuralNetworkPipeResult(new Rect2d(x, y, width, height), t.classIdx, t.confidence));
        }

        return ret;
    }

    private static Letterbox letterbox(Mat frame, Mat letterboxed, Size newShape, Scalar color) {
        // from https://github.com/ultralytics/yolov5/issues/8427#issuecomment-1172469631
        var frameSize = frame.size();
        var r = Math.min(newShape.height / frameSize.height, newShape.width / frameSize.width);

        var newUnpad = new Size(Math.round(frameSize.width * r), Math.round(frameSize.height * r));

        if (!(frameSize.equals(newUnpad))) {
            Imgproc.resize(frame, letterboxed, newUnpad, Imgproc.INTER_LINEAR);
        } else {
            frame.copyTo(letterboxed);
        }

        var dw = newShape.width - newUnpad.width;
        var dh = newShape.height - newUnpad.height;

        dw /= 2;
        dh /= 2;

        int top = (int) (Math.round(dh - 0.1f));
        int bottom = (int) (Math.round(dh + 0.1f));
        int left = (int) (Math.round(dw - 0.1f));
        int right = (int) (Math.round(dw + 0.1f));
        Core.copyMakeBorder(
                letterboxed, letterboxed, top, bottom, left, right, Core.BORDER_CONSTANT, color);

        return new Letterbox(dw, dh, r);
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
