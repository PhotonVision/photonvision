// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.math.geometry;

import java.util.Objects;

/**
 * A change in distance along a 3D arc since the last pose update. We can use ideas from
 * differential calculus to create new Pose3ds from a Twist3d and vise versa.
 *
 * <p>A Twist can be used to represent a difference between two poses.
 */
@SuppressWarnings("MemberName")
public class Twist3d {
  /** Linear "dx" component. */
  public double dx;

  /** Linear "dy" component. */
  public double dy;

  /** Linear "dz" component. */
  public double dz;

  /** Rotation vector x component (radians). */
  public double rx;

  /** Rotation vector y component (radians). */
  public double ry;

  /** Rotation vector z component (radians). */
  public double rz;

  public Twist3d() {}

  /**
   * Constructs a Twist3d with the given values.
   *
   * @param dx Change in x direction relative to robot.
   * @param dy Change in y direction relative to robot.
   * @param dz Change in z direction relative to robot.
   * @param rx Rotation vector x component.
   * @param ry Rotation vector y component.
   * @param rz Rotation vector z component.
   */
  public Twist3d(double dx, double dy, double dz, double rx, double ry, double rz) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.rx = rx;
    this.ry = ry;
    this.rz = rz;
  }

  @Override
  public String toString() {
    return String.format(
        "Twist3d(dX: %.2f, dY: %.2f, dZ: %.2f, rX: %.2f, rY: %.2f, rZ: %.2f)",
        dx, dy, dz, rx, ry, rz);
  }

  /**
   * Checks equality between this Twist3d and another object.
   *
   * @param obj The other object.
   * @return Whether the two objects are equal or not.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Twist3d) {
      return Math.abs(((Twist3d) obj).dx - dx) < 1E-9
          && Math.abs(((Twist3d) obj).dy - dy) < 1E-9
          && Math.abs(((Twist3d) obj).dz - dz) < 1E-9
          && Math.abs(((Twist3d) obj).rx - rx) < 1E-9
          && Math.abs(((Twist3d) obj).ry - ry) < 1E-9
          && Math.abs(((Twist3d) obj).rz - rz) < 1E-9;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dx, dy, dz, rx, ry, rz);
  }
}