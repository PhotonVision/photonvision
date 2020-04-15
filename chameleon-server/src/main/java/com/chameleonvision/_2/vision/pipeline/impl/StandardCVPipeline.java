package com.chameleonvision._2.vision.pipeline.impl;

import com.chameleonvision._2.Main;
import com.chameleonvision._2.vision.camera.CameraCapture;
import com.chameleonvision._2.vision.camera.CaptureStaticProperties;
import com.chameleonvision._2.vision.pipeline.CVPipeline;
import com.chameleonvision._2.vision.pipeline.CVPipelineResult;
import com.chameleonvision._2.vision.pipeline.pipes.*;
import com.chameleonvision.common.util.MemoryManager;
import com.chameleonvision.common.vision.opencv.Contour;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.opencv.core.*;

import java.awt.*;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class StandardCVPipeline extends CVPipeline<StandardCVPipeline.StandardCVPipelineResult, StandardCVPipelineSettings> {

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
    private DrawSolvePNPPipe drawSolvePNPPipe;
    private SolvePNPPipe solvePNPPipe;
    private Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings draw2dCrosshairPipeSettings;
    private OutputMatPipe outputMatPipe;

    private String pipelineTimeString = "";
    private CaptureStaticProperties camProps;
    private Scalar hsvLower, hsvUpper;

    public StandardCVPipeline() {
        super(new StandardCVPipelineSettings());
    }

    public StandardCVPipeline(String name) {
        super(name, new StandardCVPipelineSettings());
    }

    public StandardCVPipeline(StandardCVPipelineSettings settings) {
        super(settings);
    }

    @Override
    public void initPipeline(CameraCapture process) {
        super.initPipeline(process);

        camProps = cameraCapture.getProperties().getStaticProperties();
        hsvLower = new Scalar(settings.hue.getFirst(), settings.saturation.getFirst(), settings.value.getFirst());
        hsvUpper = new Scalar(settings.hue.getSecond(), settings.saturation.getSecond(), settings.value.getSecond());

        rotateFlipPipe = new RotateFlipPipe(settings.rotationMode, settings.flipMode);
        blurPipe = new BlurPipe(5);
        erodeDilatePipe = new ErodeDilatePipe(settings.erode, settings.dilate, 7);
        hsvPipe = new HsvPipe(hsvLower, hsvUpper);
        findContoursPipe = new FindContoursPipe();
        filterContoursPipe = new FilterContoursPipe(settings.area, settings.ratio, settings.extent, camProps);
        speckleRejectPipe = new SpeckleRejectPipe(settings.speckle.doubleValue());
        groupContoursPipe = new GroupContoursPipe(settings.targetGroup, settings.targetIntersection);
        sortContoursPipe = new SortContoursPipe(settings.sortMode, camProps, 5);
        collect2dTargetsPipe = new Collect2dTargetsPipe(settings.calibrationMode, settings.targetRegion, settings.targetOrientation, settings.point, settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);
        draw2dContoursSettings = new Draw2dContoursPipe.Draw2dContoursSettings();
        draw2dCrosshairPipeSettings = new Draw2dCrosshairPipe.Draw2dCrosshairPipeSettings();

        draw2dContoursSettings.showCentroid = true;
        draw2dContoursSettings.centroidColor = new Color(25, 239, 0);
        draw2dContoursSettings.boxOutlineSize = 2;
        draw2dContoursSettings.showRotatedBox = true;
        draw2dContoursSettings.showMaximumBox = true;
        draw2dContoursSettings.showMultiple = settings.multiple;
        draw2dCrosshairPipeSettings.showCrosshair = true;
        draw2dContoursPipe = new Draw2dContoursPipe(draw2dContoursSettings, camProps);
        draw2dCrosshairPipe = new Draw2dCrosshairPipe(draw2dCrosshairPipeSettings, settings.calibrationMode, settings.point, settings.dualTargetCalibrationM, settings.dualTargetCalibrationB);
        outputMatPipe = new OutputMatPipe(settings.isBinary);
    }

    private final MemoryManager memManager = new MemoryManager(120, 20000);

    private StandardCVPipelineResult resultCache = new StandardCVPipelineResult(List.of(), new Mat(), 0L);

    @Override
    public StandardCVPipelineResult runPipeline(Mat inputMat) {
        long totalPipelineTimeNanos = 0;
        long pipelineStartTimeNanos = System.nanoTime();

        resultCache.release();

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
        hsvLower = new Scalar(settings.hue.getFirst(), settings.saturation.getFirst(), settings.value.getFirst());
        hsvUpper = new Scalar(settings.hue.getSecond(), settings.saturation.getSecond(), settings.value.getSecond());
        rotateFlipPipe.setConfig(settings.rotationMode, settings.flipMode);
        blurPipe.setConfig(0);
        erodeDilatePipe.setConfig(settings.erode, settings.dilate, 7);
        hsvPipe.setConfig(hsvLower, hsvUpper);
        filterContoursPipe.setConfig(settings.area, settings.ratio, settings.extent, camProps);
        speckleRejectPipe.setConfig(settings.speckle.doubleValue());
        groupContoursPipe.setConfig(settings.targetGroup, settings.targetIntersection);
        sortContoursPipe.setConfig(settings.sortMode, camProps, 5);
        collect2dTargetsPipe.setConfig(settings.calibrationMode, settings.targetRegion, settings.targetOrientation, settings.point, settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);
        draw2dContoursPipe.setConfig(settings.multiple, camProps);
        draw2dCrosshairPipe.setConfig(draw2dCrosshairPipeSettings, settings.calibrationMode, settings.point, settings.dualTargetCalibrationM, settings.dualTargetCalibrationB);
        outputMatPipe.setConfig(settings.isBinary);

        if (settings.is3D) {
            if (solvePNPPipe == null)
                solvePNPPipe = new SolvePNPPipe(settings, cameraCapture.getCurrentCalibrationData(), cameraCapture.getProperties().getTilt());
            if (drawSolvePNPPipe == null)
                drawSolvePNPPipe = new DrawSolvePNPPipe(settings, cameraCapture.getCurrentCalibrationData());

            solvePNPPipe.setConfig(settings, cameraCapture.getCurrentCalibrationData(), cameraCapture.getProperties().getTilt());
            drawSolvePNPPipe.setConfig(cameraCapture.getCurrentCalibrationData());
            drawSolvePNPPipe.setConfig(settings);

        }

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

        Pair<List<Contour>, Long> findContoursResult = findContoursPipe.run(hsvResult.getLeft());
        totalPipelineTimeNanos += findContoursResult.getRight();

        Pair<List<Contour>, Long> filterContoursResult = filterContoursPipe.run(findContoursResult.getLeft().);
        totalPipelineTimeNanos += filterContoursResult.getRight();

        // ignore !
        Pair<List<Contour>, Long> speckleRejectResult = speckleRejectPipe.run(filterContoursResult.getLeft());
        totalPipelineTimeNanos += speckleRejectResult.getRight();

        Pair<List<TrackedTarget>, Long> groupContoursResult = groupContoursPipe.run(speckleRejectResult.getLeft());
        totalPipelineTimeNanos += groupContoursResult.getRight();

        Pair<List<TrackedTarget>, Long> sortContoursResult = sortContoursPipe.run(groupContoursResult.getLeft());
        totalPipelineTimeNanos += sortContoursResult.getRight();

        Pair<List<TrackedTarget>, Long> collect2dTargetsResult = collect2dTargetsPipe.run(Pair.of(sortContoursResult.getLeft(), camProps));
        totalPipelineTimeNanos += collect2dTargetsResult.getRight();

        // takes pair of (Mat of original camera image (8UC3), Mat of HSV thresholded image(8UC1))
        Pair<Mat, Long> outputMatResult = outputMatPipe.run(Pair.of(rawCameraMat, hsvResult.getLeft()));
        totalPipelineTimeNanos += outputMatResult.getRight();

        Pair<Mat, Long> result;

        if (!settings.is3D) {
            // takes pair of (Mat to draw on, List<RotatedRect> of sorted contours)
            result = draw2dContoursPipe.run(Pair.of(outputMatResult.getLeft(), sortContoursResult.getLeft()));
            totalPipelineTimeNanos += result.getRight();
        } else {
            result = outputMatResult;
        }

        // takes pair of (Mat to draw on, List<RotatedRect> of sorted contours)
        Pair<Mat, Long> draw2dCrosshairResult = draw2dCrosshairPipe.run(Pair.of(result.getLeft(), collect2dTargetsResult.getLeft()));
        totalPipelineTimeNanos += draw2dCrosshairResult.getRight();

        Mat outputMat;

        if (settings.is3D) {
            // once we've sorted our targets, perform solvePNP. The number of "best targets" is limited by the above pipe
            Pair<List<TrackedTarget>, Long> solvePNPResult = solvePNPPipe.run(Pair.of(collect2dTargetsResult.getLeft(), rotateFlipResult.getLeft()));
            totalPipelineTimeNanos += solvePNPResult.getRight();

            Pair<Mat, Long> draw3dContoursResult = drawSolvePNPPipe.run(Pair.of(outputMatResult.getLeft(), solvePNPResult.getLeft()));
            totalPipelineTimeNanos += draw3dContoursResult.getRight();

            outputMat = draw3dContoursResult.getLeft();
        } else {
            outputMat = draw2dCrosshairResult.getLeft();
        }

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
            pipelineTimeString += String.format("Draw2dContours: %.2fms, ", result.getRight() / 1000000.0);
            pipelineTimeString += String.format("Draw2dCrosshair: %.2fms, ", draw2dCrosshairResult.getRight() / 1000000.0);

            System.out.println(pipelineTimeString);
            double totalPipelineTimeMillis = totalPipelineTimeNanos / 1000000.0;
            double totalPipelineTimeFPS = 1.0 / (totalPipelineTimeMillis / 1000.0);
            double truePipelineTimeMillis = (System.nanoTime() - pipelineStartTimeNanos) / 1000000.0;
            double truePipelineFPS = 1.0 / (truePipelineTimeMillis / 1000.0);
            System.out.printf("Pipeline processed in %.3fms (%.2fFPS), ", totalPipelineTimeMillis, totalPipelineTimeFPS);
            System.out.printf("full pipeline run time was %.3fms (%.2fFPS)\n", truePipelineTimeMillis, truePipelineFPS);
        }

//        memManager.run();

        resultCache = new StandardCVPipelineResult(collect2dTargetsResult.getLeft(), outputMat, totalPipelineTimeNanos);
        return resultCache;
    }

    public static class StandardCVPipelineResult extends CVPipelineResult<TrackedTarget> {
        public StandardCVPipelineResult(List<TrackedTarget> targets, Mat outputMat, long processTimeNanos) {
            super(targets, outputMat, processTimeNanos);
        }

        public void release() {
            targets.forEach(TrackedTarget::release);
            outputMat.release();
        }
    }

    public static class TrackedTarget {
        public double calibratedX = 0.0;
        public double calibratedY = 0.0;
        public double pitch = 0.0;
        public double yaw = 0.0;
        public double area = 0.0;
        public Point point = new Point();
        public RotatedRect minAreaRect;
        public Rect boundingRect;

        // 3d stuff
        public Pose2d cameraRelativePose = new Pose2d();
        public Mat rVector = new Mat();
        public Mat tVector = new Mat();
        public MatOfPoint2f imageCornerPoints = new MatOfPoint2f();
        public Pair<Rect, Rect> leftRightDualTargetPair = null;
        public Pair<RotatedRect, RotatedRect> leftRightRotatedRect = null;
        public MatOfPoint2f rawContour = kMat2f;
        public MatOfPoint2f approxPoly = new MatOfPoint2f();

        public void release() {
            rVector.release();
            tVector.release();
            imageCornerPoints.release();
        }

        private static final MatOfPoint2f kMat2f = new MatOfPoint2f();
    }


}
