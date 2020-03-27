package com.chameleonvision._2.vision.pipeline.pipes;

import com.chameleonvision._2.vision.camera.CaptureStaticProperties;
import com.chameleonvision._2.vision.pipeline.Pipe;
import com.chameleonvision.common.util.math.MathUtils;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class FilterContoursPipe implements Pipe<List<MatOfPoint>, List<MatOfPoint>> {

    private DoubleCouple area;
    private DoubleCouple ratio;
    private DoubleCouple extent;
    private CaptureStaticProperties camProps;

    private List<MatOfPoint> filteredContours = new ArrayList<>();

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

    @Override
    public Pair<List<MatOfPoint>, Long> run(List<MatOfPoint> input) {
        long processStartNanos = System.nanoTime();

        filteredContours.clear();

        if (input.size() > 0) {
            for (MatOfPoint Contour : input) {
                try {
                    double contourArea = Imgproc.contourArea(Contour);
                    double AreaRatio = (contourArea / camProps.imageArea) * 100;
                    double minArea = (MathUtils.sigmoid(area.getFirst()));
                    double maxArea = (MathUtils.sigmoid(area.getFirst()));
                    if (AreaRatio < minArea || AreaRatio > maxArea) {
                        continue;
                    }
                    var rect = Imgproc.minAreaRect(new MatOfPoint2f(Contour.toArray()));
                    double minExtent = (extent.getFirst() * rect.size.area()) / 100;
                    double maxExtent = (extent.getSecond() * rect.size.area()) / 100;
                    if (contourArea <= minExtent || contourArea >= maxExtent) {
                        continue;
                    }
                    Rect bb = Imgproc.boundingRect(Contour);
                    double aspectRatio = ((double)bb.width / bb.height);
                    if (aspectRatio < ratio.getFirst() || aspectRatio > ratio.getSecond()) {
                        continue;
                    }
                    filteredContours.add(Contour);
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
