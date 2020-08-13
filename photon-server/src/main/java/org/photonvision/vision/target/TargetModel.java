/*
 * Copyright (C) 2020 Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.wpilibj.util.Units;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.photonvision.vision.opencv.Releasable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TargetModel implements Releasable {

    @JsonIgnore
    private final MatOfPoint3f realWorldTargetCoordinates;
    @JsonIgnore
    private final MatOfPoint3f visualizationBoxBottom = new MatOfPoint3f();
    @JsonIgnore
    private final MatOfPoint3f visualizationBoxTop = new MatOfPoint3f();

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
        return get2020Target(Units.inchesToMeters(2d * 12d + 5.25));
    }

    public static TargetModel get2020Target(double offsetMeters) {
        var corners =
            List.of(
                new Point3(Units.inchesToMeters(-19.625), 0, offsetMeters),
                new Point3(Units.inchesToMeters(-9.819867), Units.inchesToMeters(-17), offsetMeters),
                new Point3(Units.inchesToMeters(9.819867), Units.inchesToMeters(-17), offsetMeters),
                new Point3(Units.inchesToMeters(19.625), 0, offsetMeters));
        return new TargetModel(corners, Units.inchesToMeters(12));
    }

    public static TargetModel get2019Target() {
        var corners =
            List.of(
                new Point3(Units.inchesToMeters(-5.936),Units.inchesToMeters( 2.662), 0),
                new Point3(Units.inchesToMeters(-7.313),Units.inchesToMeters( -2.662), 0),
                new Point3(Units.inchesToMeters(7.313), Units.inchesToMeters(-2.662), 0),
                new Point3(Units.inchesToMeters(5.936), Units.inchesToMeters(2.662), 0));
        return new TargetModel(corners, 0.1);
    }

    public static TargetModel getCircleTarget(double radius) {
        var corners =
            List.of(
                new Point3(-radius / 2, -radius / 2, -radius / 2),
                new Point3(-radius / 2, radius / 2, -radius / 2),
                new Point3(radius / 2, radius / 2, -radius / 2),
                new Point3(radius / 2, -radius / 2, -radius / 2));
        return new TargetModel(corners, 0);
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
