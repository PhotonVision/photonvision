package com.chameleonvision.vision.pipeline;

import com.chameleonvision.Main;
import com.chameleonvision.util.MemoryManager;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.pipes.*;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;

import java.util.List;

import static com.chameleonvision.vision.pipeline.CVPipeline2d.*;

@SuppressWarnings("WeakerAccess")
public class CVPipeline2d extends CVPipeline<CVPipeline2dResult, CVPipeline2dSettings> {

    private Mat rawCameraMat = new Mat();

    private RotateFlipPipe rotateFlipPipe;
    private BlurPipe blurPipe;
    private ErodeDilatePipe erodeDilatePipe;
    private HsvPipe hsvPipe;
    private FindContoursPipe findContoursPipe;
    private FilterContoursPipe filterContoursPipe;
    private SpeckleRejectPipe speckleRejectPipe;
    private GroupContoursPipe groupContoursPipe;
    private SortContoursPipe sortContoursPipe;
    private Collect2dTargetsPipe collect2dTargetsPipe;
    private Draw2dContoursPipe.Draw2dContoursSettings draw2dContoursSettings;
    private Draw2dContoursPipe draw2dContoursPipe;
    private Draw2dCrosshairPipe draw2dCrosshairPipe;
    private Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings draw2dCrosshairPipeSettings;
    private OutputMatPipe outputMatPipe;

    private String pipelineTimeString = "";
    private CaptureStaticProperties camProps;
    private Scalar hsvLower, hsvUpper;

    public CVPipeline2d() {
        super(new CVPipeline2dSettings());
    }

    public CVPipeline2d(String name) {
        super(name, new CVPipeline2dSettings());
    }

    public CVPipeline2d(CVPipeline2dSettings settings) {
        super(settings);
    }

    @Override
    public void initPipeline(CameraCapture process) {
        super.initPipeline(process);

        camProps = cameraCapture.getProperties().getStaticProperties();
        hsvLower = new Scalar(settings.hue.get(0).intValue(), settings.saturation.get(0).intValue(), settings.value.get(0).intValue());
        hsvUpper = new Scalar(settings.hue.get(1).intValue(), settings.saturation.get(1).intValue(), settings.value.get(1).intValue());

        rotateFlipPipe = new RotateFlipPipe(settings.rotationMode, settings.flipMode);
        blurPipe = new BlurPipe(5);
        erodeDilatePipe = new ErodeDilatePipe(settings.erode, settings.dilate, 7);
        hsvPipe = new HsvPipe(hsvLower, hsvUpper);
        findContoursPipe = new FindContoursPipe();
        filterContoursPipe = new FilterContoursPipe(settings.area, settings.ratio, settings.extent, camProps);
        speckleRejectPipe = new SpeckleRejectPipe(settings.speckle.doubleValue());
        groupContoursPipe = new GroupContoursPipe(settings.targetGroup, settings.targetIntersection);
        sortContoursPipe = new SortContoursPipe(settings.sortMode, camProps, 5);
        collect2dTargetsPipe = new Collect2dTargetsPipe(settings.calibrationMode, settings.point,
                settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);
        draw2dContoursSettings = new Draw2dContoursPipe.Draw2dContoursSettings();
        // TODO: make settable from UI? config?
        draw2dContoursSettings.showCentroid = false;
        draw2dContoursSettings.boxOutlineSize = 2;
        draw2dContoursSettings.showRotatedBox = true;
        draw2dContoursSettings.showMaximumBox = true;
        draw2dContoursSettings.showMultiple = settings.multiple;
        draw2dContoursPipe = new Draw2dContoursPipe(draw2dContoursSettings, camProps);
        draw2dCrosshairPipeSettings = new Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings();
        draw2dCrosshairPipeSettings.showCrosshair=true;
        draw2dCrosshairPipe=new Draw2dCrosshairPipe(draw2dCrosshairPipeSettings);
        outputMatPipe = new OutputMatPipe(settings.isBinary);
    }

    private final MemoryManager memManager = new MemoryManager(120, 20000);

    @Override
    public CVPipeline2dResult runPipeline(Mat inputMat) {
        long totalPipelineTimeNanos = 0;
        long pipelineStartTimeNanos = System.nanoTime();

        if (cameraCapture == null) {
            throw new RuntimeException("Pipeline was not initialized before being run!");
        }

        // TODO (HIGH) find the source of the random NPE
        if (settings == null) {
            throw new RuntimeException("settings was not initialized!");
        }
        if (inputMat.cols() <= 1) {
            throw new RuntimeException("Input Mat is empty!");
        }

        pipelineTimeString = "";

        // prepare pipes
        camProps = cameraCapture.getProperties().getStaticProperties();
        hsvLower = new Scalar(settings.hue.get(0).intValue(), settings.saturation.get(0).intValue(), settings.value.get(0).intValue());
        hsvUpper = new Scalar(settings.hue.get(1).intValue(), settings.saturation.get(1).intValue(), settings.value.get(1).intValue());
        rotateFlipPipe.setConfig(settings.rotationMode, settings.flipMode);
        blurPipe.setConfig(0);
        erodeDilatePipe.setConfig(settings.erode, settings.dilate, 7);
        hsvPipe.setConfig(hsvLower, hsvUpper);
        filterContoursPipe.setConfig(settings.area, settings.ratio, settings.extent, camProps);
        speckleRejectPipe.setConfig(settings.speckle.doubleValue());
        groupContoursPipe.setConfig(settings.targetGroup, settings.targetIntersection);
        sortContoursPipe.setConfig(settings.sortMode, camProps, 5);
        collect2dTargetsPipe.setConfig(settings.calibrationMode, settings.point,
                settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);
        draw2dContoursPipe.setConfig(settings.multiple, camProps);
        outputMatPipe.setConfig(settings.isBinary);

        long pipeInitTimeNanos = System.nanoTime() - pipelineStartTimeNanos;

        // run pipes
        Pair<Mat, Long> rotateFlipResult = rotateFlipPipe.run(inputMat);
        totalPipelineTimeNanos += rotateFlipResult.getRight();
		
        inputMat.copyTo(rawCameraMat);

//        Pair<Mat, Long> blurResult = blurPipe.run(rotateFlipResult.getLeft());
//        totalPipelineTimeNanos += blurResult.getRight();

        Pair<Mat, Long> erodeDilateResult = erodeDilatePipe.run(rotateFlipResult.getLeft());
        totalPipelineTimeNanos += erodeDilateResult.getRight();

        Pair<Mat, Long> hsvResult = hsvPipe.run(erodeDilateResult.getLeft());
        totalPipelineTimeNanos += hsvResult.getRight();

        Pair<List<MatOfPoint>, Long> findContoursResult = findContoursPipe.run(hsvResult.getLeft());
        totalPipelineTimeNanos += findContoursResult.getRight();

        Pair<List<MatOfPoint>, Long> filterContoursResult = filterContoursPipe.run(findContoursResult.getLeft());
        totalPipelineTimeNanos += filterContoursResult.getRight();

        Pair<List<MatOfPoint>, Long> speckleRejectResult = speckleRejectPipe.run(filterContoursResult.getLeft());
        totalPipelineTimeNanos += speckleRejectResult.getRight();

        Pair<List<RotatedRect>, Long> groupContoursResult = groupContoursPipe.run(speckleRejectResult.getLeft());
        totalPipelineTimeNanos += groupContoursResult.getRight();

        Pair<List<RotatedRect>, Long> sortContoursResult = sortContoursPipe.run(groupContoursResult.getLeft());
        totalPipelineTimeNanos += sortContoursResult.getRight();

        Pair<List<Target2d>, Long> collect2dTargetsResult = collect2dTargetsPipe.run(Pair.of(sortContoursResult.getLeft(), camProps));
        totalPipelineTimeNanos += collect2dTargetsResult.getRight();

        // takes pair of (Mat of original camera image (8UC3), Mat of HSV thresholded image(8UC1))
        Pair<Mat, Long> outputMatResult = outputMatPipe.run(Pair.of(rawCameraMat, hsvResult.getLeft()));
        totalPipelineTimeNanos += outputMatResult.getRight();

        // takes pair of (Mat to draw on, List<RotatedRect> of sorted contours)
        Pair<Mat, Long> draw2dContoursResult = draw2dContoursPipe.run(Pair.of(outputMatResult.getLeft(), sortContoursResult.getLeft()));
        totalPipelineTimeNanos += draw2dContoursResult.getRight();

        // takes pair of (Mat to draw on, List<RotatedRect> of sorted contours)
        Pair<Mat, Long> draw2dCrosshairResult = draw2dCrosshairPipe.run(Pair.of(draw2dContoursResult.getLeft(),collect2dTargetsResult.getLeft()));
        totalPipelineTimeNanos += draw2dCrosshairResult.getRight();

        if (Main.testMode) {
            pipelineTimeString += String.format("PipeInit: %.2fms, ", pipeInitTimeNanos / 1000000.0);
            pipelineTimeString += String.format("RotateFlip: %.2fms, ", rotateFlipResult.getRight() / 1000000.0);
//            pipelineTimeString += String.format("Blur: %.2fms, ", blurResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("ErodeDilate: %.2fms, ", erodeDilateResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("HSV: %.2fms, ", hsvResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("FindContours: %.2fms, ", findContoursResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("FilterContours: %.2fms, ", filterContoursResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("SpeckleReject: %.2fms, ", speckleRejectResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("GroupContours: %.2fms, ", groupContoursResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("SortContours: %.2fms, ", sortContoursResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("Collect2dTargets: %.2fms, ", collect2dTargetsResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("OutputMat: %.2fms, ", outputMatResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("Draw2dContours: %.2fms, ", draw2dContoursResult.getRight() / 1000000.0);
            pipelineTimeString += String.format("Draw2dCrosshair: %.2fms, ", draw2dCrosshairResult.getRight() / 1000000.0);

            System.out.println(pipelineTimeString);
            double totalPipelineTimeMillis = totalPipelineTimeNanos / 1000000.0;
            double totalPipelineTimeFPS = 1.0 / (totalPipelineTimeMillis / 1000.0);
            double truePipelineTimeMillis = (System.nanoTime() - pipelineStartTimeNanos) / 1000000.0;
            double truePipelineFPS = 1.0 / (truePipelineTimeMillis / 1000.0);
            System.out.printf("Pipeline processed in %.3fms (%.2fFPS), ", totalPipelineTimeMillis, totalPipelineTimeFPS);
            System.out.printf("full pipeline run time was %.3fms (%.2fFPS)\n", truePipelineTimeMillis, truePipelineFPS);
        }

        memManager.run();

        return new CVPipeline2dResult(collect2dTargetsResult.getLeft(), draw2dCrosshairResult.getLeft(), totalPipelineTimeNanos);
    }

    public static class CVPipeline2dResult extends CVPipelineResult<Target2d> {
        public CVPipeline2dResult(List<Target2d> targets, Mat outputMat, long processTimeNanos) {
            super(targets, outputMat, processTimeNanos);
        }
    }

    public static class Target2d {
        public double calibratedX = 0.0;
        public double calibratedY = 0.0;
        public double pitch = 0.0;
        public double yaw = 0.0;
        public double area = 0.0;
        public RotatedRect rawPoint;
    }
}
