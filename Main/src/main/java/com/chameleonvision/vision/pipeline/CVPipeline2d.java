package com.chameleonvision.vision.pipeline;

import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.pipeline.pipes.*;
import com.chameleonvision.vision.enums.ImageRotation;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import static com.chameleonvision.vision.pipeline.CVPipeline2d.*;

@SuppressWarnings("WeakerAccess")
public class CVPipeline2d extends CVPipeline<CVPipeline2dResult, CVPipeline2dSettings> {

    private Mat rawCameraMat = new Mat();
    private Mat hsvOutputMat = new Mat();
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

        CaptureStaticProperties camProps = cameraCapture.getProperties().getStaticProperties();
        Scalar hsvLower = new Scalar(settings.hue.get(0).intValue(), settings.saturation.get(0).intValue(), settings.value.get(0).intValue());
        Scalar hsvUpper = new Scalar(settings.hue.get(1).intValue(), settings.saturation.get(1).intValue(), settings.value.get(1).intValue());

        rotateFlipPipe = new RotateFlipPipe(ImageRotation.DEG_0, settings.flipMode);
        blurPipe = new BlurPipe(5);
        erodeDilatePipe = new ErodeDilatePipe(settings.erode, settings.dilate, 7);
        hsvPipe = new HsvPipe(hsvLower, hsvUpper);
        findContoursPipe = new FindContoursPipe();
        filterContoursPipe = new FilterContoursPipe(settings.area, settings.ratio, settings.extent, camProps);
        speckleRejectPipe = new SpeckleRejectPipe(settings.speckle.doubleValue());
        groupContoursPipe = new GroupContoursPipe(settings.targetGroup, settings.targetIntersection);
        sortContoursPipe = new SortContoursPipe(settings.sortMode, camProps);
        collect2dTargetsPipe = new Collect2dTargetsPipe(settings.calibrationMode, settings.point,
                settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);
    }

    @Override
    public CVPipeline2dResult runPipeline(Mat inputMat) {
        long totalProcessTimeNanos = 0;
        long processStartTimeNanos = System.nanoTime();

        if (cameraCapture == null) {
            throw new RuntimeException("Pipeline was not initialized before being run!");
        }
        if(inputMat.cols() <= 1) {
            throw new RuntimeException("Input Mat is empty!");
        }

        StringBuilder procTimeStringBuilder = new StringBuilder();

        CaptureStaticProperties camProps = cameraCapture.getProperties().getStaticProperties();

        inputMat.copyTo(rawCameraMat);

        // prepare pipes
        Scalar hsvLower = new Scalar(settings.hue.get(0).intValue(), settings.saturation.get(0).intValue(), settings.value.get(0).intValue());
        Scalar hsvUpper = new Scalar(settings.hue.get(1).intValue(), settings.saturation.get(1).intValue(), settings.value.get(1).intValue());

        rotateFlipPipe.setConfig(ImageRotation.DEG_0, settings.flipMode);
        blurPipe.setConfig(0);
        erodeDilatePipe.setConfig(settings.erode, settings.dilate, 7);
        hsvPipe.setConfig(hsvLower, hsvUpper);
        filterContoursPipe.setConfig(settings.area, settings.ratio, settings.extent, camProps);
        speckleRejectPipe.setConfig(settings.speckle.doubleValue());
        groupContoursPipe.setConfig(settings.targetGroup, settings.targetIntersection);
        sortContoursPipe.setConfig(settings.sortMode, camProps);
        collect2dTargetsPipe.setConfig(settings.calibrationMode, settings.point,
                settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);

        OutputMatPipe outputMatPipe = new OutputMatPipe(settings.isBinary);

        Draw2dContoursPipe.Draw2dContoursSettings draw2dContoursSettings = new Draw2dContoursPipe.Draw2dContoursSettings();
        draw2dContoursSettings.showCentroid = false;
        draw2dContoursSettings.showCrosshair = true;
        draw2dContoursSettings.boxOutlineSize = 2;
        draw2dContoursSettings.showRotatedBox = true;
        draw2dContoursSettings.showMaximumBox = true;

        Draw2dContoursPipe draw2dContoursPipe = new Draw2dContoursPipe(draw2dContoursSettings, camProps);

        long pipeInitTimeNanos = System.nanoTime() - processStartTimeNanos;
        totalProcessTimeNanos += pipeInitTimeNanos;
        procTimeStringBuilder.append(String.format("PipeInit: %.2fms, ", pipeInitTimeNanos / 1000000.0));

        // run pipes
        Pair<Mat, Long> rotateFlipResult = rotateFlipPipe.run(inputMat);
        totalProcessTimeNanos += rotateFlipResult.getRight();
        procTimeStringBuilder.append(String.format("RotateFlip: %.2fms, ", rotateFlipResult.getRight() / 1000000.0));

        Pair<Mat, Long> blurResult = blurPipe.run(rotateFlipResult.getLeft());
        totalProcessTimeNanos += blurResult.getRight();
        procTimeStringBuilder.append(String.format("Blur: %.2fms, ", blurResult.getRight() / 1000000.0));

        Pair<Mat, Long> erodeDilateResult = erodeDilatePipe.run(blurResult.getLeft());
        totalProcessTimeNanos += erodeDilateResult.getRight();
        procTimeStringBuilder.append(String.format("ErodeDilate: %.2fms, ", erodeDilateResult.getRight() / 1000000.0));

        Pair<Mat, Long> hsvResult = hsvPipe.run(erodeDilateResult.getLeft());
        totalProcessTimeNanos += hsvResult.getRight();

        try {
            Imgproc.cvtColor(hsvResult.getLeft(), hsvOutputMat, Imgproc.COLOR_GRAY2BGR, 3);
        } catch (CvException e) {
            System.err.println("(CVPipeline2d) Exception thrown by OpenCV: \n" + e.getMessage());
        }

        procTimeStringBuilder.append(String.format("HSV: %.2fms, ", hsvResult.getRight() / 1000000.0));

        Pair<List<MatOfPoint>, Long> findContoursResult = findContoursPipe.run(hsvResult.getLeft());
        totalProcessTimeNanos += findContoursResult.getRight();
        procTimeStringBuilder.append(String.format("FindContours: %.2fms, ", findContoursResult.getRight() / 1000000.0));

        Pair<List<MatOfPoint>, Long> filterContoursResult = filterContoursPipe.run(findContoursResult.getLeft());
        totalProcessTimeNanos += filterContoursResult.getRight();
        procTimeStringBuilder.append(String.format("FilterContours: %.2fms, ", filterContoursResult.getRight() / 1000000.0));

        Pair<List<MatOfPoint>, Long> speckleRejectResult = speckleRejectPipe.run(filterContoursResult.getLeft());
        totalProcessTimeNanos += speckleRejectResult.getRight();
        procTimeStringBuilder.append(String.format("SpeckleReject: %.2fms, ", speckleRejectResult.getRight() / 1000000.0));

        Pair<List<RotatedRect>, Long> groupContoursResult = groupContoursPipe.run(speckleRejectResult.getLeft());
        totalProcessTimeNanos += groupContoursResult.getRight();
        procTimeStringBuilder.append(String.format("GroupContours: %.2fms, ", groupContoursResult.getRight() / 1000000.0));

        Pair<List<RotatedRect>, Long> sortContoursResult = sortContoursPipe.run(groupContoursResult.getLeft());
        totalProcessTimeNanos += sortContoursResult.getRight();
        procTimeStringBuilder.append(String.format("SortContours: %.2fms, ", sortContoursResult.getRight() / 1000000.0));

        Pair<List<Target2d>, Long> collect2dTargetsResult = collect2dTargetsPipe.run(sortContoursResult.getLeft());
        totalProcessTimeNanos += collect2dTargetsResult.getRight();
        procTimeStringBuilder.append(String.format("SortContours: %.2fms, ", sortContoursResult.getRight() / 1000000.0));

        // takes pair of (Mat of original camera image, Mat of HSV thresholded image)
        Pair<Mat, Long> outputMatResult = outputMatPipe.run(Pair.of(rawCameraMat, hsvOutputMat));
        totalProcessTimeNanos += outputMatResult.getRight();
        procTimeStringBuilder.append(String.format("OutputMat: %.2fms, ", outputMatResult.getRight() / 1000000.0));

        // takes pair of (Mat to draw on, List<RotatedRect> of sorted contours)
        Pair<Mat, Long> draw2dContoursResult = draw2dContoursPipe.run(Pair.of(outputMatResult.getLeft(), sortContoursResult.getLeft()));
        totalProcessTimeNanos += draw2dContoursResult.getRight();
        procTimeStringBuilder.append(String.format("Draw2dContours: %.2fms, ", draw2dContoursResult.getRight() / 1000000.0));

        System.out.println(procTimeStringBuilder.toString());
        System.out.printf("Pipeline ran in %.3fms\n", totalProcessTimeNanos / 1000000.0);

        return new CVPipeline2dResult(collect2dTargetsResult.getLeft(), draw2dContoursResult.getLeft(), totalProcessTimeNanos);
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
