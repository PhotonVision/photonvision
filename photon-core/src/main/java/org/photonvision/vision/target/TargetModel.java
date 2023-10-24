/*
 * Copyright (C) Photon Vision.
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
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.CornerDetectionPipe;
import org.photonvision.vision.pipe.impl.SolvePNPPipe;

/**
 * A model representing the vertices of targets with known shapes. The vertices are in the EDN
 * coordinate system. When creating a TargetModel, the vertices must be supplied in a certain order
 * to ensure correct correspondence with corners detected in 2D for use with SolvePNP. For planar
 * targets, we expect the target's Z-axis to point towards the camera.
 *
 * <p>{@link SolvePNPPipe} expects 3d object points to correspond to the {@link CornerDetectionPipe}
 * implementation. The 2d corner detection finds the 4 extreme corners (bottom-left, bottom-right,
 * top-right, top-left). To match our expectations, this means the model vertices would look like:
 *
 * <ul>
 *   <li>(+x, +y, 0)
 *   <li>(-x, +y, 0)
 *   <li>(-x, -y, 0)
 *   <li>(+x, -y, 0)
 * </ul>
 *
 * <p>AprilTag models are currently only used for drawing on the output stream.
 */
public enum TargetModel implements Releasable {
  k2016HighGoal(
      List.of(
          new Point3(Units.inchesToMeters(10), Units.inchesToMeters(6), 0),
          new Point3(Units.inchesToMeters(-10), Units.inchesToMeters(6), 0),
          new Point3(Units.inchesToMeters(-10), Units.inchesToMeters(-6), 0),
          new Point3(Units.inchesToMeters(10), Units.inchesToMeters(-6), 0)),
      Units.inchesToMeters(6)),
  k2019DualTarget(
      List.of(
          new Point3(Units.inchesToMeters(7.313), Units.inchesToMeters(2.662), 0),
          new Point3(Units.inchesToMeters(-7.313), Units.inchesToMeters(2.662), 0),
          new Point3(Units.inchesToMeters(-5.936), Units.inchesToMeters(-2.662), 0),
          new Point3(Units.inchesToMeters(5.936), Units.inchesToMeters(-2.662), 0)),
      0.1),
  k2020HighGoalOuter(
      List.of(
          new Point3(Units.inchesToMeters(9.819867), Units.inchesToMeters(8.5), 0),
          new Point3(Units.inchesToMeters(-9.819867), Units.inchesToMeters(8.5), 0),
          new Point3(Units.inchesToMeters(-19.625), Units.inchesToMeters(-8.5), 0),
          new Point3(Units.inchesToMeters(19.625), Units.inchesToMeters(-8.5), 0)),
      Units.inchesToMeters(12)),
  kCircularPowerCell7in(
      List.of(
          new Point3(
              -Units.inchesToMeters(7) / 2,
              -Units.inchesToMeters(7) / 2,
              -Units.inchesToMeters(7) / 2),
          new Point3(
              -Units.inchesToMeters(7) / 2,
              Units.inchesToMeters(7) / 2,
              -Units.inchesToMeters(7) / 2),
          new Point3(
              Units.inchesToMeters(7) / 2,
              Units.inchesToMeters(7) / 2,
              -Units.inchesToMeters(7) / 2),
          new Point3(
              Units.inchesToMeters(7) / 2,
              -Units.inchesToMeters(7) / 2,
              -Units.inchesToMeters(7) / 2)),
      0),
  k2022CircularCargoBall(
      List.of(
          new Point3(
              -Units.inchesToMeters(9.5) / 2,
              -Units.inchesToMeters(9.5) / 2,
              -Units.inchesToMeters(9.5) / 2),
          new Point3(
              -Units.inchesToMeters(9.5) / 2,
              Units.inchesToMeters(9.5) / 2,
              -Units.inchesToMeters(9.5) / 2),
          new Point3(
              Units.inchesToMeters(9.5) / 2,
              Units.inchesToMeters(9.5) / 2,
              -Units.inchesToMeters(9.5) / 2),
          new Point3(
              Units.inchesToMeters(9.5) / 2,
              -Units.inchesToMeters(9.5) / 2,
              -Units.inchesToMeters(9.5) / 2)),
      0),
  k200mmAprilTag( // Corners of the tag's inner black square (excluding white border)
      List.of(
          new Point3(Units.inchesToMeters(3.25), Units.inchesToMeters(3.25), 0),
          new Point3(-Units.inchesToMeters(3.25), Units.inchesToMeters(3.25), 0),
          new Point3(-Units.inchesToMeters(3.25), -Units.inchesToMeters(3.25), 0),
          new Point3(Units.inchesToMeters(3.25), -Units.inchesToMeters(3.25), 0)),
      Units.inchesToMeters(3.25 * 2)),
  kAruco6in_16h5( // Corners of the tag's inner black square (excluding white border)
      List.of(
          new Point3(Units.inchesToMeters(3), Units.inchesToMeters(3), 0),
          new Point3(Units.inchesToMeters(3), -Units.inchesToMeters(3), 0),
          new Point3(-Units.inchesToMeters(3), -Units.inchesToMeters(3), 0),
          new Point3(Units.inchesToMeters(3), -Units.inchesToMeters(3), 0)),
      Units.inchesToMeters(3 * 2)),
  k6in_16h5( // Corners of the tag's inner black square (excluding white border)
      List.of(
          new Point3(Units.inchesToMeters(3), Units.inchesToMeters(3), 0),
          new Point3(-Units.inchesToMeters(3), Units.inchesToMeters(3), 0),
          new Point3(-Units.inchesToMeters(3), -Units.inchesToMeters(3), 0),
          new Point3(Units.inchesToMeters(3), -Units.inchesToMeters(3), 0)),
      Units.inchesToMeters(3 * 2));

  @JsonIgnore private MatOfPoint3f realWorldTargetCoordinates;
  @JsonIgnore private final MatOfPoint3f visualizationBoxBottom = new MatOfPoint3f();
  @JsonIgnore private final MatOfPoint3f visualizationBoxTop = new MatOfPoint3f();

  @JsonProperty("realWorldCoordinatesArray")
  private List<Point3> realWorldCoordinatesArray;

  @JsonProperty("boxHeight")
  private double boxHeight;

  TargetModel() {}

  TargetModel(MatOfPoint3f realWorldTargetCoordinates, double boxHeight) {
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
  TargetModel(
      @JsonProperty(value = "realWorldCoordinatesArray") List<Point3> points,
      @JsonProperty(value = "boxHeight") double boxHeight) {
    this(listToMat(points), boxHeight);
  }

  public List<Point3> getRealWorldCoordinatesArray() {
    return this.realWorldCoordinatesArray;
  }

  public double getBoxHeight() {
    return boxHeight;
  }

  public void setRealWorldCoordinatesArray(List<Point3> realWorldCoordinatesArray) {
    this.realWorldCoordinatesArray = realWorldCoordinatesArray;
  }

  public void setBoxHeight(double boxHeight) {
    this.boxHeight = boxHeight;
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

  //    public static TargetModel getCircleTarget(double Units.inchesToMeters(7)) {
  //        var corners =
  //            List.of(
  //                new Point3(-Units.inchesToMeters(7) / 2, -radius / 2, -radius / 2),
  //                new Point3(-Units.inchesToMeters(7) / 2, radius / 2, -radius / 2),
  //                new Point3(Units.inchesToMeters(7) / 2, radius / 2, -radius / 2),
  //                new Point3(Units.inchesToMeters(7) / 2, -radius / 2, -radius / 2));
  //        return new TargetModel(corners, 0);
  //    }

  @Override
  public void release() {
    realWorldTargetCoordinates.release();
    visualizationBoxBottom.release();
    visualizationBoxTop.release();
  }
}
