package com.chameleonvision.vision.process;

import com.chameleonvision.vision.*;
import com.chameleonvision.vision.camera.CameraValues;
import com.chameleonvision.util.MathHandler;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class CVProcess {

    private final CameraValues cameraValues;
    private Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
    private Size blur = new Size(3, 3);
    private Mat hsvImage = new Mat();
    private List<MatOfPoint> foundContours = new ArrayList<>();
    private Mat binaryMat = new Mat();
    private List<MatOfPoint> filteredContours = new ArrayList<>();
    private Comparator<RotatedRect> sortByCentermostComparator = Comparator.comparingDouble(this::calcDistance);
    private List<MatOfPoint> speckleRejectedContours = new ArrayList<>();
    private Comparator<MatOfPoint> sortByMomentsX = Comparator.comparingDouble(this::calcMomentsX);
    private List<RotatedRect> finalCountours = new ArrayList<>();
    private MatOfPoint2f intersectMatA = new MatOfPoint2f();
    private MatOfPoint2f intersectMatB = new MatOfPoint2f();

    CVProcess(CameraValues cameraValues) {
        this.cameraValues = cameraValues;
    }

    private Mat cameraInputMat = new Mat();
    private Mat hsvThreshMat = new Mat();
    private Mat streamOutputMat = new Mat();

    private List<MatOfPoint> foundContours_ = new ArrayList<>();
    private List<MatOfPoint> filteredContours_ = new ArrayList<>();
    private List<MatOfPoint> deSpeckledContours_ = new ArrayList<>();
    private List<RotatedRect> groupedContours_ = new ArrayList<>();

    private static final Scalar contourRectColor = new Scalar(255, 0, 0);
    private static final Scalar BoxRectColor = new Scalar(0, 0, 233);

    private void drawContour(Mat inputMat, RotatedRect contourRect) {
        if (contourRect == null) return;
        List<MatOfPoint> drawnContour = new ArrayList<>();
        Point[] vertices = new Point[4];
        contourRect.points(vertices);
        MatOfPoint contour = new MatOfPoint(vertices);
        drawnContour.add(contour);
        Rect box = Imgproc.boundingRect(contour);
        Imgproc.drawContours(inputMat, drawnContour, 0, contourRectColor, 3);
        Imgproc.circle(inputMat, contourRect.center, 3, contourRectColor);
        Imgproc.rectangle(inputMat, new Point(box.x, box.y), new Point((box.x + box.width), (box.y + box.height)), BoxRectColor, 2);
    }

    PipelineResult runPipeline(Pipeline currentPipeline, Mat inputImage, Mat outputImage, CameraValues cameraValues, boolean shouldFlip, boolean driverMode) {
        var pipelineResult = new PipelineResult();

        if (currentPipeline == null) {
            return pipelineResult;
        }

        // flip the image
        if (shouldFlip) {
            Core.flip(inputImage, inputImage, -1);
        }

        // if we're in driver mode don't do anything, and return a blank result
        if (driverMode) {
            inputImage.copyTo(outputImage);
            return pipelineResult;
        }

        foundContours_.clear();
        filteredContours_.clear();
        deSpeckledContours_.clear();
        groupedContours_.clear();

        // HSV threshold the image
        Scalar hsvLower = new Scalar(currentPipeline.hue.get(0).intValue(), currentPipeline.saturation.get(0).intValue(), currentPipeline.value.get(0).intValue());
        Scalar hsvUpper = new Scalar(currentPipeline.hue.get(1).intValue(), currentPipeline.saturation.get(1).intValue(), currentPipeline.value.get(1).intValue());
        hsvThreshold(inputImage, hsvThreshMat, hsvLower, hsvUpper, currentPipeline.erode, currentPipeline.dilate);

        // Make sure we're BFR
        if (currentPipeline.isBinary) {
            Imgproc.cvtColor(hsvThreshMat, outputImage, Imgproc.COLOR_GRAY2BGR, 3);
        } else {
            inputImage.copyTo(outputImage);
        }

        // search for contours
        foundContours_ = findContours(hsvThreshMat);
        if (foundContours_.size() < 1) {
            return pipelineResult;
        }

        // filter contours by area, ratio and extent
        filteredContours_ = filterContours(foundContours_, currentPipeline.area, currentPipeline.ratio, currentPipeline.extent);
        if (filteredContours_.size() < 1) {
            return pipelineResult;
        }

        // reject "speckle" contours
        deSpeckledContours_ = rejectSpeckles(filteredContours_, currentPipeline.speckle.doubleValue());
        if (deSpeckledContours_.size() < 1) {
            return pipelineResult;
        }

        // group targets
        groupedContours_ = groupTargets(deSpeckledContours_, currentPipeline.targetIntersection, currentPipeline.targetGroup);
        if (groupedContours_.size() < 1) {
            return pipelineResult;
        }

        // sort targets down to our final target
        var finalRect = sortTargetsToOne(groupedContours_, currentPipeline.sortMode);
        pipelineResult.RawPoint = finalRect;
        pipelineResult.IsValid = true;
        switch (currentPipeline.calibrationMode) {
            case None:
                ///use the center of the USBCamera to find the pitch and yaw difference
                pipelineResult.CalibratedX = cameraValues.CenterX;
                pipelineResult.CalibratedY = cameraValues.CenterY;
                break;
            case Single:
                // use the static point as a calibration method instead of the center
                pipelineResult.CalibratedX = currentPipeline.point.get(0).doubleValue();
                pipelineResult.CalibratedY = currentPipeline.point.get(1).doubleValue();
                break;
            case Dual:
                // use the calculated line to find the difference in length between the point and the line
                pipelineResult.CalibratedX = (finalRect.center.y - currentPipeline.b) / currentPipeline.m;
                pipelineResult.CalibratedY = (finalRect.center.x * currentPipeline.m) + currentPipeline.b;
                break;
        }

        pipelineResult.Pitch = cameraValues.CalculatePitch(finalRect.center.y, pipelineResult.CalibratedY);
        pipelineResult.Yaw = cameraValues.CalculateYaw(finalRect.center.x, pipelineResult.CalibratedX);
        pipelineResult.Area = finalRect.size.area();
        drawContour(outputImage, finalRect);

        return pipelineResult;
    }

    void hsvThreshold(Mat srcImage, Mat dst, @NotNull Scalar hsvLower, @NotNull Scalar hsvUpper, boolean shouldErode, boolean shouldDilate) {
        Imgproc.cvtColor(srcImage, hsvImage, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.blur(hsvImage, hsvImage, blur);
        Core.inRange(hsvImage, hsvLower, hsvUpper, dst);
        if (shouldErode) {
            Imgproc.erode(dst, dst, kernel);
        }
        if (shouldDilate) {
            Imgproc.dilate(dst, dst, kernel);
        }
        hsvImage.release();
    }

    List<MatOfPoint> findContours(Mat src) {
        src.copyTo(binaryMat);
        foundContours.clear();
        Imgproc.findContours(binaryMat, foundContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);
        binaryMat.release();
        return foundContours;
    }

    List<MatOfPoint> filterContours(List<MatOfPoint> inputContours, List<Number> area, List<Number> ratio, List<Number> extent) {
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

    List<MatOfPoint> rejectSpeckles(List<MatOfPoint> inputContours, Double minimumPercentOfAverage) {
        double averageArea = 0.0;
        for (MatOfPoint c : inputContours) {
            averageArea += Imgproc.contourArea(c);
        }
        averageArea /= inputContours.size();
        var minimumAllowableArea = minimumPercentOfAverage / 100.0 * averageArea;
        speckleRejectedContours.clear();
        for (MatOfPoint c : inputContours) {
            if (Imgproc.contourArea(c) >= minimumAllowableArea) speckleRejectedContours.add(c);
        }
        return speckleRejectedContours;
    }


    private double calcDistance(RotatedRect rect) {
        return FastMath.sqrt(FastMath.pow(cameraValues.CenterX - rect.center.x, 2) + FastMath.pow(cameraValues.CenterY - rect.center.y, 2));
    }

    private double calcMomentsX(MatOfPoint c) {
        Moments m = Imgproc.moments(c);
        return (m.get_m10() / m.get_m00());
    }

    RotatedRect sortTargetsToOne(List<RotatedRect> inputRects, SortMode sortMode) {
        switch (sortMode) {
            case Largest:
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.size.area()));
            case Smallest:
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.size.area()));
            case Highest:
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.center.y));
            case Lowest:
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.center.y));
            case Leftmost:
                return Collections.min(inputRects, Comparator.comparing(rect -> rect.center.x));
            case Rightmost:
                return Collections.max(inputRects, Comparator.comparing(rect -> rect.center.x));
            case Centermost:
                return Collections.min(inputRects, sortByCentermostComparator);
            default:
                return inputRects.get(0); // default to whatever the first contour is, but this should never happen
        }
    }

    List<RotatedRect> groupTargets(List<MatOfPoint> inputContours, TargetIntersection intersectionPoint, TargetGroup targetGroup) {
        finalCountours.clear();
        inputContours.sort(sortByMomentsX);
        Collections.reverse(inputContours);
        if (targetGroup.equals(TargetGroup.Dual)) {
            for (var i = 0; i < inputContours.size(); i++) {
                List<Point> FinalContourList = new ArrayList<>(inputContours.get(i).toList());
                try {
                    MatOfPoint firstContour = inputContours.get(i);
                    MatOfPoint secondContour = inputContours.get(i + 1);
                    if (isIntersecting(firstContour, secondContour, intersectionPoint)) {
                        FinalContourList.addAll(secondContour.toList());
                    } else {
                        FinalContourList.clear();
                        continue;
                    }
                    firstContour.release();
                    secondContour.release();
                    MatOfPoint2f contour = new MatOfPoint2f();
                    contour.fromList(FinalContourList);
                    if (contour.cols() != 0 && contour.rows() != 0) {
                        RotatedRect rect = Imgproc.minAreaRect(contour);
                        finalCountours.add(rect);
                    }
                } catch (IndexOutOfBoundsException e) {
                    FinalContourList.clear();
                }
            }

        } else if (targetGroup.equals(TargetGroup.Single)) {
            for (MatOfPoint inputContour : inputContours) {
                MatOfPoint2f contour = new MatOfPoint2f();
                contour.fromArray(inputContour.toArray());
                if (contour.cols() != 0 && contour.rows() != 0) {
                    RotatedRect rect = Imgproc.minAreaRect(contour);
                    finalCountours.add(rect);
                }
            }
        }
        return finalCountours;
    }

    private boolean isIntersecting(MatOfPoint ContourOne, MatOfPoint ContourTwo, TargetIntersection intersectionPoint) {
        if (intersectionPoint.equals(TargetIntersection.None)) {
            return true;
        }
        try {
            intersectMatA.fromArray(ContourOne.toArray());
            intersectMatB.fromArray(ContourTwo.toArray());
            RotatedRect a = Imgproc.fitEllipse(intersectMatA);
            RotatedRect b = Imgproc.fitEllipse(intersectMatB);
            double mA = MathHandler.toSlope(a.angle);
            double mB = MathHandler.toSlope(b.angle);
            double x0A = a.center.x;
            double y0A = a.center.y;
            double x0B = b.center.x;
            double y0B = b.center.y;
            double intersectionX = ((mA * x0A) - y0A - (mB * x0B) + y0B) / (mA - mB);
            double intersectionY = (mA * (intersectionX - x0A)) + y0A;
            double massX = (x0A + x0B) / 2;
            double massY = (y0A + y0B) / 2;
            switch (intersectionPoint) {
                case Up: {
                    if (intersectionY < massY) {
                        if (mA > 0 && mB < 0) {
                            return true;
                        }
                    }
                    break;
                }
                case Down: {
                    if (intersectionY > massY) {
                        if (mA < 0 && mB > 0) {
                            return true;
                        }
                    }

                    break;
                }
                case Left: {
                    if (intersectionX < massX) {

                        return true;
                    }
                    break;
                }
                case Right: {
                    if (intersectionX > massX) {
                        return true;
                    }
                    break;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
