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

package edu.wpi.first.wpilibj.util;

/** Utility class that converts between commonly used units in FRC. */
public final class Units {
    private static final double kInchesPerFoot = 12.0;
    private static final double kMetersPerInch = 0.0254;
    private static final double kSecondsPerMinute = 60;

    /** Utility class, so constructor is private. */
    private Units() {
        throw new UnsupportedOperationException("This is a utility class!");
    }

    /**
    * Converts given meters to feet.
    *
    * @param meters The meters to convert to feet.
    * @return Feet converted from meters.
    */
    public static double metersToFeet(double meters) {
        return metersToInches(meters) / kInchesPerFoot;
    }

    /**
    * Converts given feet to meters.
    *
    * @param feet The feet to convert to meters.
    * @return Meters converted from feet.
    */
    public static double feetToMeters(double feet) {
        return inchesToMeters(feet * kInchesPerFoot);
    }

    /**
    * Converts given meters to inches.
    *
    * @param meters The meters to convert to inches.
    * @return Inches converted from meters.
    */
    public static double metersToInches(double meters) {
        return meters / kMetersPerInch;
    }

    /**
    * Converts given inches to meters.
    *
    * @param inches The inches to convert to meters.
    * @return Meters converted from inches.
    */
    public static double inchesToMeters(double inches) {
        return inches * kMetersPerInch;
    }

    /**
    * Converts given degrees to radians.
    *
    * @param degrees The degrees to convert to radians.
    * @return Radians converted from degrees.
    */
    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    /**
    * Converts given radians to degrees.
    *
    * @param radians The radians to convert to degrees.
    * @return Degrees converted from radians.
    */
    public static double radiansToDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /**
    * Converts rotations per minute to radians per second.
    *
    * @param rpm The rotations per minute to convert to radians per second.
    * @return Radians per second converted from rotations per minute.
    */
    public static double rotationsPerMinuteToRadiansPerSecond(double rpm) {
        return rpm * Math.PI / (kSecondsPerMinute / 2);
    }

    /**
    * Converts radians per second to rotations per minute.
    *
    * @param radiansPerSecond The radians per second to convert to from rotations per minute.
    * @return Rotations per minute converted from radians per second.
    */
    public static double radiansPerSecondToRotationsPerMinute(double radiansPerSecond) {
        return radiansPerSecond * (kSecondsPerMinute / 2) / Math.PI;
    }
}
