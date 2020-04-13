package com.chameleonvision._2.vision.pipeline.pipes;

import com.chameleonvision._2.vision.camera.CaptureStaticProperties;
import com.chameleonvision._2.vision.enums.SortMode;
import com.chameleonvision._2.vision.pipeline.Pipe;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipeline;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;

public class SortContoursPipe
        implements Pipe<
                List<StandardCVPipeline.TrackedTarget>, List<StandardCVPipeline.TrackedTarget>> {

    private final Comparator<StandardCVPipeline.TrackedTarget> SortByCentermostComparator =
            Comparator.comparingDouble(this::calcSquareCenterDistance);

    private static final Comparator<StandardCVPipeline.TrackedTarget> SortByLargestComparator =
            (rect1, rect2) ->
                    Double.compare(rect2.minAreaRect.size.area(), rect1.minAreaRect.size.area());
    private static final Comparator<StandardCVPipeline.TrackedTarget> SortBySmallestComparator =
            SortByLargestComparator.reversed();

    private static final Comparator<StandardCVPipeline.TrackedTarget> SortByHighestComparator =
            (rect1, rect2) -> Double.compare(rect1.minAreaRect.center.y, rect2.minAreaRect.center.y);
    private static final Comparator<StandardCVPipeline.TrackedTarget> SortByLowestComparator =
            SortByHighestComparator.reversed();

    public static final Comparator<StandardCVPipeline.TrackedTarget> SortByLeftmostComparator =
            Comparator.comparingDouble(target -> target.minAreaRect.center.x);
    private static final Comparator<StandardCVPipeline.TrackedTarget> SortByRightmostComparator =
            SortByLeftmostComparator.reversed();

    private SortMode sort;
    private CaptureStaticProperties camProps;
    private int maxTargets;

    private List<StandardCVPipeline.TrackedTarget> sortedContours = new ArrayList<>();

    public SortContoursPipe(SortMode sort, CaptureStaticProperties camProps, int maxTargets) {
        this.sort = sort;
        this.camProps = camProps;
        this.maxTargets = maxTargets;
    }

    public void setConfig(SortMode sort, CaptureStaticProperties camProps, int maxTargets) {
        this.sort = sort;
        this.camProps = camProps;
        this.maxTargets = maxTargets;
    }

    @Override
    public Pair<List<StandardCVPipeline.TrackedTarget>, Long> run(
            List<StandardCVPipeline.TrackedTarget> input) {
        long processStartNanos = System.nanoTime();

        sortedContours.clear();

        if (input.size() > 0) {
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
        }

        var sublistedContors =
                new ArrayList<>(sortedContours.subList(0, Math.min(input.size(), maxTargets - 1)));
        sortedContours
                .subList(Math.min(input.size(), maxTargets - 1), sortedContours.size())
                .forEach(StandardCVPipeline.TrackedTarget::release);
        sortedContours.clear();
        sortedContours = new ArrayList<>();

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(sublistedContors, processTime);
    }

    private double calcSquareCenterDistance(StandardCVPipeline.TrackedTarget rect) {
        return FastMath.sqrt(
                FastMath.pow(camProps.centerX - rect.minAreaRect.center.x, 2)
                        + FastMath.pow(camProps.centerY - rect.minAreaRect.center.y, 2));
    }
}
