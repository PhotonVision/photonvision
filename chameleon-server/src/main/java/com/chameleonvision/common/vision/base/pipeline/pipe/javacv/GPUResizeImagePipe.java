package com.chameleonvision.common.vision.base.pipeline.pipe.javacv;

import com.chameleonvision.common.vision.base.pipeline.Pipe;
import org.bytedeco.opencv.opencv_core.GpuMat;

public class GPUResizeImagePipe extends Pipe<GpuMat, GpuMat> {

    public GPUResizeImagePipe() {

    }

    @Override
    protected GpuMat process(GpuMat in) {
        return null;
    }
}
