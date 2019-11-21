package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.classabstraction.camera.CameraStaticProperties;
import com.chameleonvision.classabstraction.pipeline.pipes.*;
import com.chameleonvision.vision.ImageRotation;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.function.Supplier;

import static com.chameleonvision.classabstraction.pipeline.CVPipeline2d.*;

@SuppressWarnings("WeakerAccess")
public class CVPipeline2d extends CVPipeline<CVPipeline2dResult, CVPipeline2dSettings> {

    private Mat rawCameraMat = new Mat();
    private Mat hsvOutputMat = new Mat();

    public CVPipeline2d(Supplier<CVPipeline2dSettings> settingsSupplier) {
        super(settingsSupplier);
    }

    @Override
    public CVPipeline2dResult runPipeline(Mat inputMat) {

        if (cameraProcess == null) {
            throw new RuntimeException("Pipeline was not initialized before being run!");
        }

        long totalProcessTimeNanos = 0;
        StringBuilder procTimeStringBuilder = new StringBuilder();

        var settings = settingsSupplier.get();
        CameraStaticProperties camProps = cameraProcess.getProperties().staticProperties;

		rawCameraMat = inputMat;

        // prepare pipes
        RotateFlipPipe rotateFlipPipe = new RotateFlipPipe(ImageRotation.DEG_0, settings.flipMode);
        BlurPipe blurPipe = new BlurPipe(5);
        ErodeDilatePipe erodeDilatePipe = new ErodeDilatePipe(settings.erode, settings.dilate, 7);

        Scalar hsvLower = new Scalar(settings.hue.get(0).intValue(), settings.saturation.get(0).intValue(), settings.value.get(0).intValue());
        Scalar hsvUpper = new Scalar(settings.hue.get(1).intValue(), settings.saturation.get(1).intValue(), settings.value.get(1).intValue());

        HsvPipe hsvPipe = new HsvPipe(hsvLower, hsvUpper);

        FindContoursPipe findContoursPipe = new FindContoursPipe();
        FilterContoursPipe filterContoursPipe = new FilterContoursPipe(settings.area, settings.ratio, settings.extent, camProps);
        SpeckleRejectPipe speckleRejectPipe = new SpeckleRejectPipe(settings.speckle.doubleValue());
        GroupContoursPipe groupContoursPipe = new GroupContoursPipe(settings.targetGroup, settings.targetIntersection);
        SortContoursPipe sortContoursPipe = new SortContoursPipe(settings.sortMode, camProps);
        Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe(settings.calibrationMode, settings.point,
                settings.dualTargetCalibrationM, settings.dualTargetCalibrationB, camProps);

        OutputMatPipe outputMatPipe = new OutputMatPipe(settings.isBinary);

        Draw2dContoursPipe.Draw2dContoursSettings draw2dContoursSettings = new Draw2dContoursPipe.Draw2dContoursSettings();
        draw2dContoursSettings.showCentroid = false;
        draw2dContoursSettings.showCrosshair = true;
        draw2dContoursSettings.boxOutlineSize = 2;
        draw2dContoursSettings.showRotatedBox = true;
        draw2dContoursSettings.showMaximumBox = true;

        Draw2dContoursPipe draw2dContoursPipe = new Draw2dContoursPipe(draw2dContoursSettings, camProps);

        // run pipes
        Pair<Mat, Long> rotateFlipResult = rotateFlipPipe.run(inputMat);
        totalProcessTimeNanos += rotateFlipResult.getRight();
        procTimeStringBuilder.append(String.format("RotateFlip: %.2fms, ", rotateFlipResult.getRight() / 1000.0));

        Pair<Mat, Long> blurResult = blurPipe.run(rotateFlipResult.getLeft());
        totalProcessTimeNanos += blurResult.getRight();
        procTimeStringBuilder.append(String.format("Blur: %.2fms, ", blurResult.getRight() / 1000.0));

        Pair<Mat, Long> erodeDilateResult = erodeDilatePipe.run(blurResult.getLeft());
        totalProcessTimeNanos += erodeDilateResult.getRight();
        procTimeStringBuilder.append(String.format("ErodeDilate: %.2fms, ", erodeDilateResult.getRight() / 1000.0));

        Pair<Mat, Long> hsvResult = hsvPipe.run(erodeDilateResult.getLeft());
        totalProcessTimeNanos += hsvResult.getRight();
        Imgproc.cvtColor(hsvResult.getLeft(), hsvOutputMat, Imgproc.COLOR_GRAY2BGR, 3);
        procTimeStringBuilder.append(String.format("HSV: %.2fms, ", hsvResult.getRight() / 1000.0));

        Pair<List<MatOfPoint>, Long> findContoursResult = findContoursPipe.run(hsvResult.getLeft());
        totalProcessTimeNanos += findContoursResult.getRight();
        procTimeStringBuilder.append(String.format("FindContours: %.2fms, ", findContoursResult.getRight() / 1000.0));

        Pair<List<MatOfPoint>, Long> filterContoursResult = filterContoursPipe.run(findContoursResult.getLeft());
        totalProcessTimeNanos += filterContoursResult.getRight();
        procTimeStringBuilder.append(String.format("FilterContours: %.2fms, ", filterContoursResult.getRight() / 1000.0));

        Pair<List<MatOfPoint>, Long> speckleRejectResult = speckleRejectPipe.run(filterContoursResult.getLeft());
        totalProcessTimeNanos += speckleRejectResult.getRight();
        procTimeStringBuilder.append(String.format("SpeckleReject: %.2fms, ", speckleRejectResult.getRight() / 1000.0));

        Pair<List<RotatedRect>, Long> groupContoursResult = groupContoursPipe.run(speckleRejectResult.getLeft());
        totalProcessTimeNanos += groupContoursResult.getRight();
        procTimeStringBuilder.append(String.format("GroupContours: %.2fms, ", groupContoursResult.getRight() / 1000.0));

        Pair<List<RotatedRect>, Long> sortContoursResult = sortContoursPipe.run(groupContoursResult.getLeft());
        totalProcessTimeNanos += sortContoursResult.getRight();
        procTimeStringBuilder.append(String.format("SortContours: %.2fms, ", sortContoursResult.getRight() / 1000.0));

        Pair<List<Target>, Long> collect2dTargetsResult = collect2dTargetsPipe.run(sortContoursResult.getLeft());
        totalProcessTimeNanos += collect2dTargetsResult.getRight();
        procTimeStringBuilder.append(String.format("SortContours: %.2fms, ", sortContoursResult.getRight() / 1000.0));

        // takes pair of (Mat of original camera image, Mat of HSV thresholded image)
        Pair<Mat, Long> outputMatResult = outputMatPipe.run(Pair.of(rawCameraMat, hsvOutputMat));
        totalProcessTimeNanos += outputMatResult.getRight();
        procTimeStringBuilder.append(String.format("OutputMat: %.2fms, ", outputMatResult.getRight() / 1000.0));

        // takes pair of (Mat to draw on, List<RotatedRect> of sorted contours)
        Pair<Mat, Long> draw2dContoursResult = draw2dContoursPipe.run(Pair.of(outputMatResult.getLeft(), sortContoursResult.getLeft()));
        totalProcessTimeNanos += draw2dContoursResult.getRight();
        procTimeStringBuilder.append(String.format("Draw2dContours: %.2fms, ", draw2dContoursResult.getRight() / 1000.0));

        System.out.println(procTimeStringBuilder.toString());
        System.out.printf("Pipeline ran in %.3fms\n", totalProcessTimeNanos / 1000.0);

        return new CVPipeline2dResult(collect2dTargetsResult.getLeft(), draw2dContoursResult.getLeft(), totalProcessTimeNanos / 1000);
    }

    public static class CVPipeline2dResult extends CVPipelineResult<Target> {
        public CVPipeline2dResult(List<Target> targets, Mat outputMat, long processTime) {
            super(targets, outputMat, processTime);
        }
    }

    public static class Target {
        public double calibratedX = 0.0;
        public double calibratedY = 0.0;
        public double pitch = 0.0;
        public double yaw = 0.0;
        public double area = 0.0;
        public RotatedRect rawPoint;
    }
}
