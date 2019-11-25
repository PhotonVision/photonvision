package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class SpeckleRejectPipe implements Pipe<List<MatOfPoint>, List<MatOfPoint>> {

    private final double minPercentOfAvg;

    private List<MatOfPoint> despeckledContours = new ArrayList<>();

    public SpeckleRejectPipe(double minPercentOfAvg) {
        this.minPercentOfAvg = minPercentOfAvg;
    }

    @Override
    public Pair<List<MatOfPoint>, Long> run(List<MatOfPoint> input) {
        long processStartNanos = System.nanoTime();

        double averageArea = 0.0;

        for (MatOfPoint c : input) {
            averageArea += Imgproc.contourArea(c);
        }

        averageArea /= input.size();

        double minAllowedArea = minPercentOfAvg / 100.0 * averageArea;

        for (MatOfPoint c : input) {
            if (Imgproc.contourArea(c) >= minAllowedArea) {
                despeckledContours.add(c);
            }
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(despeckledContours, processTime);
    }
}
