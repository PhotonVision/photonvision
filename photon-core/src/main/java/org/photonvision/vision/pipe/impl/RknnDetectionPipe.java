package org.photonvision.vision.pipe.impl;

import java.util.List;

import org.opencv.dnn.Net;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.target.TrackedTarget;

public class RknnDetectionPipe extends CVPipe<CVMat, List<TrackedTarget>, RknnDetectionPipe.RknnDetectionPipeParams> {

    @Override
    protected List<TrackedTarget> process(CVMat in) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    public static class RknnDetectionPipeParams {
        
    }
    
}