package com.chameleonvision._2.vision.pipeline.pipes;

import com.chameleonvision._2.vision.camera.CaptureStaticProperties;
import com.chameleonvision._2.vision.pipeline.Pipe;
import com.chameleonvision.common.util.math.MathUtils;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.opencv.Contour;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class FilterContoursPipe implements Pipe<List<MatOfPoint>, List<Contour>> {

    private DoubleCouple area;
    private DoubleCouple ratio;
    private DoubleCouple extent;
    private CaptureStaticProperties camProps;

    private List<Contour> filteredContours = new ArrayList<>();

    public FilterContoursPipe(DoubleCouple area, DoubleCouple ratio, DoubleCouple extent, CaptureStaticProperties camProps) {
        this.area = area;
        this.ratio = ratio;
        this.extent = extent;
        this.camProps = camProps;
    }

    public void setConfig(DoubleCouple area, DoubleCouple ratio, DoubleCouple extent, CaptureStaticProperties camProps) {
        this.area = area;
        this.ratio = ratio;
        this.extent = extent;
        this.camProps = camProps;
    }

    private void filterContour(MatOfPoint contourPoints) {

        Contour realContour = new Contour(contourPoints);

        // Area Filtering
        double contourArea = realContour.getArea();
        double areaRatio = (contourArea / camProps.imageArea) * 100;
        double minArea = (MathUtils.sigmoid(area.getFirst()));
        double maxArea = (MathUtils.sigmoid(area.getSecond()));
        if (areaRatio < minArea || areaRatio > maxArea) {
            return;
        }

        // TargetFillPercentage filtering
        RotatedRect minAreaRect = realContour.getMinAreaRect();
        double minExtent = (extent.getFirst() * minAreaRect.size.area()) / 100;
        double maxExtent = (extent.getSecond() * minAreaRect.size.area()) / 100;
        if (contourArea <= minExtent || contourArea >= maxExtent) {
            return;
        }

        // AspectRatio filtering
        Rect boundingRect = realContour.getBoundingRect();
        double aspectRatio = ((double)boundingRect.width / boundingRect.height);
        if (aspectRatio < ratio.getFirst() || aspectRatio > ratio.getSecond()) {
            return;
        }

        filteredContours.add(realContour);
    }

    @Override
    public Pair<List<Contour>, Long> run(List<MatOfPoint> input) {
        long processStartNanos = System.nanoTime();

        filteredContours.clear();

        if (input.size() > 0) {
            for (MatOfPoint contour : input) {
                try {
                    filterContour(contour);
                } catch (Exception e) {
                    System.err.println("Error while filtering contours");
                    e.printStackTrace();
                }
            }
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(filteredContours, processTime);
    }
}
