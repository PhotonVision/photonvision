package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.pipeline.pipes.Draw2dContoursPipe;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

import java.util.List;
import java.util.function.Supplier;

import static com.chameleonvision.classabstraction.pipeline.DriverVisionPipeline.DriverPipelineResult;

public class DriverVisionPipeline extends CVPipeline<DriverPipelineResult, CVPipelineSettings> {

    public DriverVisionPipeline(CVPipelineSettings settings) {
        super(settings);
    }

    @Override
    public DriverPipelineResult runPipeline(Mat inputMat) {

        outputMat = inputMat;

        var camProps = cameraProcess.getProperties().staticProperties;

        Draw2dContoursPipe.Draw2dContoursSettings draw2dContoursSettings = new Draw2dContoursPipe.Draw2dContoursSettings();
        draw2dContoursSettings.showCrosshair = true;
        Draw2dContoursPipe draw2dContoursPipe = new Draw2dContoursPipe(draw2dContoursSettings, camProps);

        outputMat = draw2dContoursPipe.run(Pair.of(outputMat, null)).getLeft();

        return new DriverPipelineResult(null, inputMat, 0);
    }

    public static class DriverPipelineResult extends CVPipelineResult<Void> {
        public DriverPipelineResult(List<Void> targets, Mat outputMat, long processTime) {
            super(targets, outputMat, processTime);
        }
    }
}
