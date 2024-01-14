package org.photonvision.vision.pipe.impl;
import java.util.Iterator;
import java.util.List;

import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.jni.RknnDetectorJNI;
public class RknnDetectionPipe extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, RknnDetectionPipe.RknnDetectionPipeParams> {
    public RknnDetectionPipe() {
        RknnDetectorJNI.createRknnDetector();
    }
    @Override
    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        var frame = in.getMat();
        if (frame.empty()) {
            return List.of();
        }
        double confThreshold = params.confidence;
        List<NeuralNetworkPipeResult> result = RknnDetectorJNI.detect(in);
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
        public RknnDetectionPipeParams() {
        }
        @Override
        public void release() {
            RknnDetectorJNI.release();
        }
        public List<String> getClassNames() {
            return RknnDetectorJNI.getClasses();
        }
    }
    
}