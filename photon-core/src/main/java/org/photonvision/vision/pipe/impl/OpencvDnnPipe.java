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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.utils.Converters;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.PhotonNet;
import org.photonvision.vision.pipe.CVPipe;

public class OpencvDnnPipe
        extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, OpencvDnnPipe.OpencvDnnPipeParams> {
    @Override
    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        if (net == null) {
            // barf?
            return List.of();
        }

        var frame = in.getMat();

        if (frame.empty()) {
            return List.of();
        }

        var blob = Dnn.blobFromImage(frame, 1.0 / 255.0, new Size(640, 640));
        net.setInput(blob);

        List<Mat> result = new ArrayList<>();
        net.forward(result, outBlobNames); // outputlayer : output1 and output2

        // From
        // https://github.com/suddh123/YOLO-object-detection-in-java/blob/code/yolo.java

        float confThreshold =
                (float) params.confidence; // Insert thresholding beyond which the model will detect
        // objects//
        List<Integer> clsIds = new ArrayList<>();
        List<Float> confs = new ArrayList<>();
        List<Rect2d> rects = new ArrayList<>();
        for (int i = 0; i < result.size(); ++i) {
            // each row is a candidate detection, the 1st 4 numbers are
            // [center_x, center_y, width, height], followed by (N-4) class probabilities
            Mat level = result.get(i);
            for (int j = 0; j < level.rows(); ++j) {
                Mat row = level.row(j);
                Mat scores = row.colRange(5, level.cols());
                Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                float confidence = (float) mm.maxVal;
                Point classIdPoint = mm.maxLoc;
                if (confidence > confThreshold) {
                    // scaling for drawing the bounding boxes//
                    int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                    int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                    int width = (int) (row.get(0, 2)[0] * frame.cols());
                    int height = (int) (row.get(0, 3)[0] * frame.rows());
                    int left = centerX - width / 2;
                    int top = centerY - height / 2;

                    clsIds.add((int) classIdPoint.x);
                    confs.add((float) confidence);
                    rects.add(new Rect2d(left, top, width, height));
                }
            }
        }
        float nmsThresh = 0.5f;
        MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
        Rect2d[] boxesArray = rects.toArray(new Rect2d[0]);
        MatOfRect2d boxes = new MatOfRect2d(boxesArray);
        MatOfInt indices = new MatOfInt();
        Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);

        List<NeuralNetworkPipeResult> targetList = new ArrayList<>();

        int[] ind = indices.toArray();
        for (int i = 0; i < ind.length; ++i) {
            int idx = ind[i];
            Rect2d box = boxesArray[idx];

            targetList.add(new NeuralNetworkPipeResult(box, clsIds.get(idx), confs.get(idx)));
        }

        return targetList;
    }

    public OpencvDnnPipe() {
        super();
        this.params = new OpencvDnnPipeParams(null, 0.5f);
    }

    @Override
    public void setParams(OpencvDnnPipeParams newParams) {
        // Avoid extra reset if we don't have to
        if (params.modelPath != null && params.modelPath.equals(newParams.modelPath)) {
            super.setParams(newParams);
            return;
        }
        super.setParams(newParams);

        if (net != null) {
            net.release();
        }

        // from https://dev.to/kojix2/yolov7-object-detection-in-ruby-in-10-minutes-5cjh
        // https://s3.ap-northeast-2.wasabisys.com/pinto-model-zoo/307_YOLOv7/with-postprocess/resources_post.tar.gz
        try {
            // this.net = Dnn.readNetFromONNX("/home/matt/Downloads/best_1.onnx");
            // this.net = Dnn.readNet("/home/matt/Downloads/yolov7_post_640x640.onnx");
            var net =
                    Dnn.readNetFromDarknet(
                            "/home/matt/Downloads/yolov4-csp-swish.cfg",
                            "/home/matt/Downloads/yolov4-csp-swish.weights");
            this.net = new PhotonNet(net);
            Core.setNumThreads(6);
        } catch (Exception e) {
            System.out.println(e);
        }
        this.outBlobNames = getOutputNames(net);

        this.classNames =
                List.of(
                        "person",
                        "bicycle",
                        "car",
                        "motorcycle",
                        "airplane",
                        "bus",
                        "train",
                        "truck",
                        "boat",
                        "traffic light",
                        "fire hydrant",
                        "stop sign",
                        "parking meter",
                        "bench",
                        "bird",
                        "cat",
                        "dog",
                        "horse",
                        "sheep",
                        "cow",
                        "elephant",
                        "bear",
                        "zebra",
                        "giraffe",
                        "backpack",
                        "umbrella",
                        "handbag",
                        "tie",
                        "suitcase",
                        "frisbee",
                        "skis",
                        "snowboard",
                        "sports ball",
                        "kite",
                        "baseball bat",
                        "baseball glove",
                        "skateboard",
                        "surfboard",
                        "tennis racket",
                        "bottle",
                        "wine glass",
                        "cup",
                        "fork",
                        "knife",
                        "spoon",
                        "bowl",
                        "banana",
                        "apple",
                        "sandwich",
                        "orange",
                        "broccoli",
                        "carrot",
                        "hot dog",
                        "pizza",
                        "donut",
                        "cake",
                        "chair",
                        "couch",
                        "potted plant",
                        "bed",
                        "dining table",
                        "toilet",
                        "tv",
                        "laptop",
                        "mouse",
                        "remote",
                        "keyboard",
                        "cell phone",
                        "microwave",
                        "oven",
                        "toaster",
                        "sink",
                        "refrigerator",
                        "book",
                        "clock",
                        "vase",
                        "scissors",
                        "teddy bear",
                        "hair drier",
                        "toothbrush");
    }

    private PhotonNet net;
    private List<String> outBlobNames = List.of();
    private List<String> classNames;

    private static List<String> getOutputNames(Net net) {
        if (net == null) {
            // barf?
            return List.of();
        }

        List<String> names = new ArrayList<>();

        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();

        outLayers.forEach(
                (item) -> names.add(layersNames.get(item - 1))); // unfold and create R-CNN layers from the
        // loaded YOLO model//
        return names;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    public static class OpencvDnnPipeParams {
        String modelPath;

        public float confidence;

        public OpencvDnnPipeParams(String modelPath, float confidence) {
            this.modelPath = modelPath;
            this.confidence = confidence;
        }
    }
}
