package com.chameleonvision.common.vision.base.pipeline.pipe;

import com.chameleonvision.common.vision.base.pipeline.CVPipe;
import com.chameleonvision.common.vision.base.pipeline.pipe.params.RotateImageParams;
import org.opencv.core.Core;
import org.opencv.core.Mat;

/**
 * Pipe that rotates an image to a given orientation
 */
public class RotateImagePipe extends CVPipe<Mat, Mat, RotateImageParams> {

    public RotateImagePipe() {
        setParams(RotateImageParams.DEFAULT);
    }

    public RotateImagePipe(RotateImageParams params) {
        setParams(params);
    }

    /**
     * Process this pipe
     * @param in {@link Mat} to be rotated
     * @return Rotated {@link Mat}
     */
    @Override
    protected Mat process(Mat in) {
        Core.rotate(in, in, params.rotation.value);
        return in;
    }
}
