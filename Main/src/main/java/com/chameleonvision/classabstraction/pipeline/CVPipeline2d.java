package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.util.MathHandler;
import com.chameleonvision.vision.ImageFlipMode;
import com.chameleonvision.vision.camera.CameraValues;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class CVPipeline2d extends CVPipeline<CVPipeline2d.CVPipeline2dResult> {

    private List<MatOfPoint> foundContours_ = new ArrayList<>();
    private List<MatOfPoint> filteredContours_ = new ArrayList<>();
    private List<MatOfPoint> deSpeckledContours_ = new ArrayList<>();
    private List<RotatedRect> groupedContours_ = new ArrayList<>();

    public CVPipeline2d(CVPipelineSettings settings) {
        super(settings);
    }

    @Override
    void initPipeline() {

    }

    @Override
    CVPipeline2d.CVPipeline2dResult runPipeline(Mat inputMat) {
        var shouldFlip = settings.flipMode.equals(ImageFlipMode.BOTH);
        var result = new CVPipeline2dResult();

        // flip the image
        if (shouldFlip) {
            Core.flip(inputMat, inputMat, -1);
        }

        foundContours_.clear();
        filteredContours_.clear();
        deSpeckledContours_.clear();
        groupedContours_.clear();

        // HSV threshold the image
        Scalar hsvLower = new Scalar(settings.hue.get(0).intValue(), settings.saturation.get(0).intValue(), settings.value.get(0).intValue());
        Scalar hsvUpper = new Scalar(settings.hue.get(1).intValue(), settings.saturation.get(1).intValue(), settings.value.get(1).intValue());
        hsvThreshold(inputImage, hsvThreshMat, settings.erode, settings.dilate);

        // Make sure we're BFR
        if (settings.isBinary) {
            Imgproc.cvtColor(hsvThreshMat, outputImage, Imgproc.COLOR_GRAY2BGR, 3);
        } else {
            inputImage.copyTo(outputImage);
        }

        // search for contours
        foundContours_ = findContours(hsvThreshMat);
        if (foundContours_.size() < 1) {
            return result;
        }

        // filter contours by area, ratio and extent
        filteredContours_ = filterContours(foundContours_, settings.area, settings.ratio, settings.extent);
        if (filteredContours_.size() < 1) {
            return result;
        }

        // reject "speckle" contours
        deSpeckledContours_ = rejectSpeckles(filteredContours_, settings.speckle.doubleValue());
        if (deSpeckledContours_.size() < 1) {
            return result;
        }

        // group targets
        groupedContours_ = groupTargets(deSpeckledContours_, settings.targetIntersection, settings.targetGroup);
        if (groupedContours_.size() < 1) {
            return result;
        }

        // sort targets down to our final target
        var finalRect = sortTargetsToOne(groupedContours_, settings.sortMode);
        result.RawPoint = finalRect;
        result.IsValid = true;
        switch (settings.calibrationMode) {
            case None:
                ///use the center of the USBCamera to find the pitch and yaw difference
                result.CalibratedX = cameraValues.CenterX;
                result.CalibratedY = cameraValues.CenterY;
                break;
            case Single:
                // use the static point as a calibration method instead of the center
                result.CalibratedX = settings.point.get(0).doubleValue();
                result.CalibratedY = settings.point.get(1).doubleValue();
                break;
            case Dual:
                // use the calculated line to find the difference in length between the point and the line
                result.CalibratedX = (finalRect.center.y - settings.b) / settings.m;
                result.CalibratedY = (finalRect.center.x * settings.m) + settings.b;
                break;
        }

        result.Pitch = cameraValues.CalculatePitch(finalRect.center.y, result.CalibratedY);
        result.Yaw = cameraValues.CalculateYaw(finalRect.center.x, result.CalibratedX);
        result.Area = finalRect.size.area();
        drawContour(outputImage, finalRect);

        return result;
    }

    /**
     * HSV Threshold a given image. Copies the HSV Thresholded image to the [dst] matrix with the given
     * hsv settings and blur settings. Can also erode and dilate the image
     * @param srcImage the source image, which is not mutated
     * @param dst the destination image, which is mutated to save the result
     * @param hsvLower the lower bound for the HSV settings
     * @param hsvUpper the upper bound for the HSV settings
     * @param kernel the kernal used to erode/dilate the image
     * @param blur the size of the blur image
     * @param shouldErode if we should erode
     * @param shouldDilate if we should dilate
     */
    public static void hsvThreshold(Mat srcImage, Mat dst, @NotNull Scalar hsvLower,
                                    @NotNull Scalar hsvUpper, Mat kernel, Size blur,
                                    boolean shouldErode, boolean shouldDilate) {
        Imgproc.cvtColor(srcImage, dst, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.blur(dst, dst, blur);
        Core.inRange(dst, hsvLower, hsvUpper, dst);
        if (shouldErode) {
            Imgproc.erode(dst, dst, kernel);
        }
        if (shouldDilate) {
            Imgproc.dilate(dst, dst, kernel);
        }
        dst.release();
    }

    /**
     * Find contours from an image
     * @param src the image we're looking at
     * @param binaryMat a temporary image
     * @param hierarchy the hierarchy of the image (just a new Mat();)
     * @param emptyList a list to fill with stuff. Will be cleared
     * @return the empty list, now full of contours
     */
    public static List<MatOfPoint> findContours(Mat src, Mat binaryMat, Mat hierarchy, List<MatOfPoint> emptyList) {
        src.copyTo(binaryMat);
        emptyList.clear();
        Imgproc.findContours(binaryMat, emptyList, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);
        binaryMat.release();
        return emptyList;
    }

    public static List<MatOfPoint> filterContours(List<MatOfPoint> inputContours, List<Number> area, List<Number> ratio, List<Number> extent, CameraValues cameraValues) {
        for (MatOfPoint Contour : inputContours) {
            try {
                double contourArea = Imgproc.contourArea(Contour);
                double AreaRatio = (contourArea / cameraValues.ImageArea) * 100;
                double minArea = (MathHandler.sigmoid(area.get(0)));
                double maxArea = (MathHandler.sigmoid(area.get(1)));
                if (AreaRatio < minArea || AreaRatio > maxArea) {
                    continue;
                }
                var rect = Imgproc.minAreaRect(new MatOfPoint2f(Contour.toArray()));

                var targetFullness = contourArea;
                double minExtent = (double) (extent.get(0).doubleValue() * rect.size.area()) / 100;
                double maxExtent = (double) (extent.get(1).doubleValue() * rect.size.area()) / 100;
                if (targetFullness <= minExtent || contourArea >= maxExtent) {
                    continue;
                }
                Rect bb = Imgproc.boundingRect(Contour);
                double aspectRatio = (bb.width / bb.height);
                if (aspectRatio < ratio.get(0).doubleValue() || aspectRatio > ratio.get(1).doubleValue()) {
                    continue;
                }
                filteredContours.add(Contour);
            } catch (Exception e) {
                System.err.println("Error while filtering contours");
                e.printStackTrace();
            }
        }
        return filteredContours;
    }

    @Override
    Mat getOutputMat() {
        return null;
    }

    public static class CVPipeline2dSettings extends CVPipelineSettings {
        double dualTargetCalibrationM = 1;
        double dualTargetCalibrationB = 0;
    }

    public static class CVPipeline2dResult {
        public boolean hasTarget = false;
        public ArrayList<Target> targets = new ArrayList<>(); // targets sorted by likelihood

        public CVPipeline2dResult(ArrayList<Target> targets, boolean hasTarget) {
            this.targets = targets;
            this.hasTarget = hasTarget;
        }

        public CVPipeline2dResult() {
        }
    }

    public static class Target {
        public boolean isValid = false;
        public double calibratedX = 0.0;
        public double calibratedY = 0.0;
        public double pitch = 0.0;
        public double yaw = 0.0;
        public double area = 0.0;
        RotatedRect rawPoint;
    }

}
