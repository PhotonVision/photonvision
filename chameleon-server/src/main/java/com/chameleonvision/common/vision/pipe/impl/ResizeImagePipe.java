package com.chameleonvision.common.vision.pipe.impl;

import com.chameleonvision.common.vision.frame.FrameDivisor;
import com.chameleonvision.common.vision.pipe.CVPipe;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/** Pipe that resizes an image to a given resolution */
public class ResizeImagePipe extends CVPipe<Mat, Mat, ResizeImagePipe.ResizeImageParams> {

    public ResizeImagePipe() {
        setParams(ResizeImageParams.DEFAULT);
    }

    public ResizeImagePipe(ResizeImageParams params) {
        setParams(params);
    }

    /**
    * Process this pipe
    *
    * @param in {@link Mat} to be resized
    * @return Resized {@link Mat}
    */
    @Override
    protected Mat process(Mat in) {

        // if a divisor is set, use that instead of a size.
        if (params.getDivisor() != null) {
            int width = in.cols() / params.getDivisor().value;
            int height = in.rows() / params.getDivisor().value;
            setParams(new ResizeImageParams(width, height));
        }

        Imgproc.resize(in, in, params.getSize());
        return in;
    }

    public static class ResizeImageParams {
        public static ResizeImageParams DEFAULT = new ResizeImageParams(320, 240);

        private Size size;
        private int width;
        private int height;
        private FrameDivisor divisor;

        public ResizeImageParams() {
            this(DEFAULT.width, DEFAULT.height);
        }

        public ResizeImageParams(int width, int height) {
            this.width = width;
            this.height = height;
            size = new Size(new double[] {width, height});
        }

        public ResizeImageParams(FrameDivisor divisor) {
            this.divisor = divisor;
        }

        public Size getSize() {
            return size;
        }

        public FrameDivisor getDivisor() {
            return divisor;
        }
    }
}
