package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import org.opencv.core.Mat;

import java.util.List;
import java.util.function.Supplier;

import static com.chameleonvision.classabstraction.pipeline.CVPipeline3d.*;

public class CVPipeline3d extends CVPipeline<CVPipeline3dResult, CVPipeline3dSettings> {

    public CVPipeline3d(Supplier<CVPipeline3dSettings> settingsSupplier) {
        super(settingsSupplier);
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

    public static class Target3d {
        // TODO: Define 3d-specific target data
    }

}
