package com.chameleonvision.common.vision.base.pipeline.pipe;

import com.chameleonvision.common.vision.base.pipeline.CVPipe;
import com.chameleonvision.common.vision.base.pipeline.pipe.params.ResizeImageParams;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Pipe that resizes an image to a given resolution
 */
public class ResizeImagePipe extends CVPipe<Mat, Mat, ResizeImageParams> {

    public ResizeImagePipe() {
        setParams(ResizeImageParams.DEFAULT);
    }

    public ResizeImagePipe(ResizeImageParams params) {
        setParams(params);
    }

    /**
     * Process this pipe
     * @param in {@link Mat} to be resized
     * @return Resized {@link Mat}
     */
    @Override
    protected Mat process(Mat in) {
        Imgproc.resize(in, in, params.getSize());
        return in;
    }
}
