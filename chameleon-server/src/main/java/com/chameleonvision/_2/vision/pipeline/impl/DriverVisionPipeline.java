package com.chameleonvision._2.vision.pipeline.impl;

import com.chameleonvision._2.vision.camera.CameraCapture;
import com.chameleonvision._2.vision.enums.CalibrationMode;
import com.chameleonvision._2.vision.pipeline.CVPipeline;
import com.chameleonvision._2.vision.pipeline.CVPipelineResult;
import com.chameleonvision._2.vision.pipeline.CVPipelineSettings;
import com.chameleonvision._2.vision.pipeline.pipes.Draw2dCrosshairPipe;
import com.chameleonvision._2.vision.pipeline.pipes.RotateFlipPipe;
import com.chameleonvision.common.util.MemoryManager;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class DriverVisionPipeline
        extends CVPipeline<DriverVisionPipeline.DriverPipelineResult, CVPipelineSettings> {

    private RotateFlipPipe rotateFlipPipe;
    private Draw2dCrosshairPipe drawCrosshairPipe;
    private Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings crosshairPipeSettings =
            new Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings();

    private final MemoryManager memoryManager = new MemoryManager(200, 20000);

    public DriverVisionPipeline(CVPipelineSettings settings) {
        super(settings);
        settings.index = -1;
    }

    @Override
    public void initPipeline(CameraCapture capture) {
        super.initPipeline(capture);
        rotateFlipPipe = new RotateFlipPipe(settings.rotationMode, settings.flipMode);
        crosshairPipeSettings.showCrosshair = true;
        drawCrosshairPipe =
                new Draw2dCrosshairPipe(crosshairPipeSettings, CalibrationMode.None, null, 0, 0);
    }

    @Override
    public DriverPipelineResult runPipeline(Mat inputMat) {

        rotateFlipPipe.setConfig(settings.rotationMode, settings.flipMode);

        Pair<Mat, Long> rotateFlipResult = rotateFlipPipe.run(inputMat);
        Pair<Mat, Long> draw2dCrosshairResult =
                drawCrosshairPipe.run(Pair.of(rotateFlipResult.getLeft(), null));
        memoryManager.run();

        return new DriverPipelineResult(null, draw2dCrosshairResult.getLeft(), 0);
    }

    public static class DriverPipelineResult extends CVPipelineResult<Void> {
        public DriverPipelineResult(List<Void> targets, Mat outputMat, long processTime) {
            super(targets, outputMat, processTime);
        }
    }
}
