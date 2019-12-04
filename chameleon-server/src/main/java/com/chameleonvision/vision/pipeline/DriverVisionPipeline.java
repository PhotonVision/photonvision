package com.chameleonvision.vision.pipeline;

import com.chameleonvision.util.MemoryManager;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.enums.CalibrationMode;
import com.chameleonvision.vision.pipeline.pipes.Draw2dContoursPipe;
import com.chameleonvision.vision.pipeline.pipes.Draw2dCrosshairPipe;
import com.chameleonvision.vision.pipeline.pipes.RotateFlipPipe;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.List;

import static com.chameleonvision.vision.pipeline.DriverVisionPipeline.DriverPipelineResult;

public class DriverVisionPipeline extends CVPipeline<DriverPipelineResult, CVPipelineSettings> {

    private RotateFlipPipe rotateFlipPipe;
//    private Draw2dContoursPipe draw2dContoursPipe;
//    private Draw2dContoursPipe.Draw2dContoursSettings draw2dContoursSettings = new Draw2dContoursPipe.Draw2dContoursSettings();
    private Draw2dCrosshairPipe drawCrosshairPipe;
    private Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings crosshairPipeSettings = new Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings();
    private final List<RotatedRect> blankList = List.of();

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
//        draw2dContoursSettings.showCrosshair = true;
//        draw2dContoursPipe = new Draw2dContoursPipe(draw2dContoursSettings, cameraCapture.getProperties().getStaticProperties());
    }

    @Override
    public DriverPipelineResult runPipeline(Mat inputMat) {

        rotateFlipPipe.setConfig(settings.rotationMode, settings.flipMode);
//        drawCrosshairPipe.setConfig(false, settings);

        Pair<Mat, Long> rotateFlipResult = rotateFlipPipe.run(inputMat);
//        Pair<Mat, Long> draw2dContoursResult = draw2dContoursPipe.run(Pair.of(rotateFlipResult.getLeft(), blankList));
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
