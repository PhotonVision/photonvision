package org.photonvision.vision.pipe.impl;
import java.util.Iterator;
import java.util.List;

import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.jni.RknnDetector;
public class RknnDetectionPipe extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, RknnDetectionPipe.RknnDetectionPipeParams> {

    @Override
    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        var frame = in.getMat();
        if (frame.empty()) {
            return List.of();
        }
        double confThreshold = params.confidence;
        List<NeuralNetworkPipeResult> result = RknnDetector.detect(in);
        if(result.isEmpty()) {
            return List.of();
        }
        Iterator<NeuralNetworkPipeResult> itr = result.iterator();
        while(itr.hasNext()) {
            NeuralNetworkPipeResult res = itr.next();
            if (res.confidence < confThreshold) {
                itr.remove();
            }
        }
        return result;

    }

    public static class RknnDetectionPipeParams implements Releasable{
        public double confidence;
        List<String> classNames;
        public RknnDetectionPipeParams() {
            this.classNames = List.of(
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
        @Override
        public void release() {
            RknnDetector.release();
        }
        public List<String> getClassNames(){
            return classNames;
        }
        
    }
    
}