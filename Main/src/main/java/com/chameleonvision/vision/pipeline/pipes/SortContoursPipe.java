package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.vision.enums.SortMode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.RotatedRect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SortContoursPipe implements Pipe<List<RotatedRect>, List<RotatedRect>> {

    private final Comparator<RotatedRect> SortByCentermostComparator = Comparator.comparingDouble(this::calcCenterDistance);

    private static final Comparator<RotatedRect> SortByLargestComparator = (rect1, rect2) -> Double.compare(rect2.size.area(), rect1.size.area());
    private static final Comparator<RotatedRect> SortBySmallestComparator = SortByLargestComparator.reversed();

    private static final Comparator<RotatedRect> SortByHighestComparator = (rect1, rect2) -> Double.compare(rect2.center.y, rect1.center.y);
    private static final Comparator<RotatedRect> SortByLowestComparator = SortByHighestComparator.reversed();

    private static final Comparator<RotatedRect> SortByLeftmostComparator = Comparator.comparingDouble(rect -> rect.center.x);
    private static final Comparator<RotatedRect> SortByRightmostComparator = SortByLeftmostComparator.reversed();


    private SortMode sort;
    private CaptureStaticProperties camProps;

    private List<RotatedRect> sortedContours = new ArrayList<>();

    public SortContoursPipe(SortMode sort, CaptureStaticProperties camProps) {
        this.sort = sort;
        this.camProps = camProps;
    }

    public void setConfig(SortMode sort, CaptureStaticProperties camProps) {
        this.sort = sort;
        this.camProps = camProps;
    }

    @Override
    public Pair<List<RotatedRect>, Long> run(List<RotatedRect> input) {
        long processStartNanos = System.nanoTime();

        sortedContours.clear();
        sortedContours.addAll(input);

        switch (sort) {
            case Largest:
                sortedContours.sort(SortByLargestComparator);
                break;
            case Smallest:
                sortedContours.sort(SortBySmallestComparator);
                break;
            case Highest:
                sortedContours.sort(SortByHighestComparator);
                break;
            case Lowest:
                sortedContours.sort(SortByLowestComparator);
                break;
            case Leftmost:
                sortedContours.sort(SortByLeftmostComparator);
                break;
            case Rightmost:
                sortedContours.sort(SortByRightmostComparator);
                break;
            case Centermost:
                sortedContours.sort(SortByCentermostComparator);
                break;
            default:
                break;
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(sortedContours, processTime);
    }

    private double calcCenterDistance(RotatedRect rect) {
        return FastMath.sqrt(FastMath.pow(camProps.centerX - rect.center.x, 2) + FastMath.pow(camProps.centerY - rect.center.y, 2));
    }
}
