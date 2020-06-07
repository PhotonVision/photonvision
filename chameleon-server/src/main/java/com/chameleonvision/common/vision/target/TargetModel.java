package com.chameleonvision.common.vision.target;

import com.chameleonvision.common.vision.opencv.Releasable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;

public class TargetModel implements Releasable {

    @JsonIgnore private final MatOfPoint3f realWorldTargetCoordinates;
    @JsonIgnore private final MatOfPoint3f visualizationBoxBottom = new MatOfPoint3f();
    @JsonIgnore private final MatOfPoint3f visualizationBoxTop = new MatOfPoint3f();

    public final List<Point3> realWorldCoordinatesArray;
    public final double boxHeight;

    public TargetModel(MatOfPoint3f realWorldTargetCoordinates, double boxHeight) {
        this.realWorldTargetCoordinates = realWorldTargetCoordinates;
        this.realWorldCoordinatesArray = realWorldTargetCoordinates.toList();
        this.boxHeight = boxHeight;

        var bottomList = realWorldTargetCoordinates.toList();
        var topList = new ArrayList<Point3>();
        for (var c : bottomList) {
            topList.add(new Point3(c.x, c.y, c.z + boxHeight));
        }

        this.visualizationBoxBottom.fromList(bottomList);
        this.visualizationBoxTop.fromList(topList);
    }

    @JsonCreator
    public TargetModel(
            @JsonProperty(value = "realWorldCoordinatesArray") List<Point3> points,
            @JsonProperty(value = "boxHeight") double boxHeight) {
        this(listToMat(points), boxHeight);
    }

    private static MatOfPoint3f listToMat(List<Point3> points) {
        var mat = new MatOfPoint3f();
        mat.fromList(points);
        return mat;
    }

    public MatOfPoint3f getRealWorldTargetCoordinates() {
        return realWorldTargetCoordinates;
    }

    public MatOfPoint3f getVisualizationBoxBottom() {
        return visualizationBoxBottom;
    }

    public MatOfPoint3f getVisualizationBoxTop() {
        return visualizationBoxTop;
    }

    public static TargetModel get2020Target() {
        return get2020Target(0);
    }

    public static TargetModel get2020TargetInnerPort() {
        return get2020Target(2d * 12d + 5.25); // Inches, TODO switch to meters
    }

    public static TargetModel get2020Target(double offset) {
        var corners =
                List.of(
                        new Point3(-19.625, 0, offset),
                        new Point3(-9.819867, -17, offset),
                        new Point3(9.819867, -17, offset),
                        new Point3(19.625, 0, offset));
        return new TargetModel(corners, 12); // TODO switch to meters
    }

    public static TargetModel get2019Target() {
        var corners =
                List.of(
                        new Point3(-5.936, 2.662, 0),
                        new Point3(-7.313, -2.662, 0),
                        new Point3(7.313, -2.662, 0),
                        new Point3(5.936, 2.662, 0));
        return new TargetModel(corners, 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetModel)) return false;
        TargetModel that = (TargetModel) o;
        return Double.compare(that.boxHeight, boxHeight) == 0
                && Objects.equals(realWorldCoordinatesArray, that.realWorldCoordinatesArray);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realWorldCoordinatesArray, boxHeight);
    }

    @Override
    public void release() {
        realWorldTargetCoordinates.release();
        visualizationBoxBottom.release();
        visualizationBoxTop.release();
    }
}
