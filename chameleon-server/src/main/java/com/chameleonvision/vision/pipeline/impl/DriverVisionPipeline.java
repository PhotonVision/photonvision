package com.chameleonvision.vision.pipeline.impl;

import com.chameleonvision.util.MemoryManager;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.enums.CalibrationMode;
import com.chameleonvision.vision.pipeline.CVPipeline;
import com.chameleonvision.vision.pipeline.CVPipelineResult;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.vision.pipeline.pipes.Draw2dCrosshairPipe;
import com.chameleonvision.vision.pipeline.pipes.RotateFlipPipe;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.List;

import static com.chameleonvision.vision.pipeline.impl.DriverVisionPipeline.DriverPipelineResult;

public class DriverVisionPipeline extends CVPipeline<DriverPipelineResult, CVPipelineSettings> {

    private RotateFlipPipe rotateFlipPipe;
    private Draw2dCrosshairPipe drawCrosshairPipe;
    private Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings crosshairPipeSettings = new Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings();

    private final MemoryManager memoryManager = new MemoryManager(200, 20000);

    public DriverVisionPipeline(CVPipelineSettings settings) {
        super(settings);
        settings.index = -1;
    }

    @Override
    public void initPipeline(CameraCapture capture) {
        super.initPipeline(capture);
        rotateFlipPipe = new RotateFlipPipe(settings.rotationMode, settings.flipMode);
        crosshairPipeSettings.showCrosshair=true;
        drawCrosshairPipe = new Draw2dCrosshairPipe(crosshairPipeSettings, CalibrationMode.None,null,0,0);
    }

    @Override
    public DriverPipelineResult runPipeline(Mat inputMat) {

        rotateFlipPipe.setConfig(settings.rotationMode, settings.flipMode);

        Pair<Mat, Long> rotateFlipResult = rotateFlipPipe.run(inputMat);
        Pair<Mat, Long> draw2dCrosshairResult = drawCrosshairPipe.run(Pair.of(rotateFlipResult.getLeft(),null));
        memoryManager.run();

        return new DriverPipelineResult(null, draw2dCrosshairResult.getLeft(), 0);
    }

    public static class DriverPipelineResult extends CVPipelineResult<Void> {
        public DriverPipelineResult(List<Void> targets, Mat outputMat, long processTime) {
            super(targets, outputMat, processTime);
        }
    }
}
