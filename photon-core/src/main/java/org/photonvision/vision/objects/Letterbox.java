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

package org.photonvision.vision.objects;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

public class Letterbox {
    double dx;
    double dy;
    double scale;

    public Letterbox(double dx, double dy, double scale) {
        this.dx = dx;
        this.dy = dy;
        this.scale = scale;
    }

    /**
     * Resize the frame to the new shape and "letterbox" it.
     *
     * <p>Letterboxing is the process of resizing an image to a new shape while maintaining the aspect
     * ratio of the original image. The new image is padded with a color to fill the remaining space.
     *
     * @param frame
     * @param letterboxed
     * @param newShape
     * @param color
     * @return
     */
    public static Letterbox letterbox(Mat frame, Mat letterboxed, Size newShape, Scalar color) {
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

    /**
     * Resizes the detections to the original frame size.
     *
     * @param unscaled The detections to resize
     * @return The resized detections
     */
    public List<NeuralNetworkPipeResult> resizeDetections(List<NeuralNetworkPipeResult> unscaled) {
        var ret = new ArrayList<NeuralNetworkPipeResult>();

        for (var t : unscaled) {
            var scale = 1.0 / this.scale;
            var boundingBox = t.bbox;
            double x = (boundingBox.x - this.dx) * scale;
            double y = (boundingBox.y - this.dy) * scale;
            double width = boundingBox.width * scale;
            double height = boundingBox.height * scale;

            ret.add(
                    new NeuralNetworkPipeResult(new Rect2d(x, y, width, height), t.classIdx, t.confidence));
        }

        return ret;
    }
}
