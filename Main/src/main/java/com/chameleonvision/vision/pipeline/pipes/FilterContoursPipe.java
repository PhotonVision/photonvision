package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.util.MathHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class FilterContoursPipe implements Pipe<List<MatOfPoint>, List<MatOfPoint>> {

    private List<Number> area;
    private List<Number> ratio;
    private List<Number> extent;
    private CaptureStaticProperties camProps;

    private List<MatOfPoint> filteredContours = new ArrayList<>();

    public FilterContoursPipe(List<Number> area, List<Number> ratio, List<Number> extent, CaptureStaticProperties camProps) {
        this.area = area;
        this.ratio = ratio;
        this.extent = extent;
        this.camProps = camProps;
    }

    public void setConfig(List<Number> area, List<Number> ratio, List<Number> extent, CaptureStaticProperties camProps) {
        this.area = area;
        this.ratio = ratio;
        this.extent = extent;
        this.camProps = camProps;
    }

    @Override
    public Pair<List<MatOfPoint>, Long> run(List<MatOfPoint> input) {
        long processStartNanos = System.nanoTime();

        filteredContours.clear();

        for (MatOfPoint Contour : input) {
            try {
                double contourArea = Imgproc.contourArea(Contour);
                double AreaRatio = (contourArea / camProps.imageArea) * 100;
                double minArea = (MathHandler.sigmoid(area.get(0)));
                double maxArea = (MathHandler.sigmoid(area.get(1)));
                if (AreaRatio < minArea || AreaRatio > maxArea) {
                    continue;
                }
                var rect = Imgproc.minAreaRect(new MatOfPoint2f(Contour.toArray()));
                double minExtent = (extent.get(0).doubleValue() * rect.size.area()) / 100;
                double maxExtent = (extent.get(1).doubleValue() * rect.size.area()) / 100;
                if (contourArea <= minExtent || contourArea >= maxExtent) {
                    continue;
                }
                Rect bb = Imgproc.boundingRect(Contour);
                double aspectRatio = ((double)bb.width / bb.height);
                if (aspectRatio < ratio.get(0).doubleValue() || aspectRatio > ratio.get(1).doubleValue()) {
                    continue;
                }
                filteredContours.add(Contour);
            } catch (Exception e) {
                System.err.println("Error while filtering contours");
                e.printStackTrace();
            }
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(filteredContours, processTime);
    }
}
