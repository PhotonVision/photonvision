package com.chameleonvision.vision.pipeline.impl;

import com.chameleonvision.vision.pipeline.CVPipeline;
import com.chameleonvision.vision.pipeline.CVPipelineResult;
import org.opencv.core.Mat;

import java.util.List;

import static com.chameleonvision.vision.pipeline.impl.CVPipeline3d.*;

public class CVPipeline3d extends CVPipeline<CVPipeline3dResult, CVPipeline3dSettings> {


    public CVPipeline3d(CVPipeline3dSettings settings) {
        super(settings);
    }

    public CVPipeline3d() {
        super(new CVPipeline3dSettings());
    }

    @Override
    public CVPipeline3dResult runPipeline(Mat inputMat) {
        return null;
    }


    public static class CVPipeline3dResult extends CVPipelineResult<Target3d> {
        public CVPipeline3dResult(List<Target3d> targets, Mat outputMat, long processTime) {
            super(targets, outputMat, processTime);
        }
    }

    public static class Target3d extends CVPipeline2d.Target2d {
        // TODO: (2.1) Define 3d-specific target data
    }
}
