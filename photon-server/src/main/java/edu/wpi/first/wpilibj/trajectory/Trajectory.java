/*----------------------------------------------------------------------------*/
/* Copyright (c) 2015-2020 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.trajectory;

// This is a stub from WPILib
public class Trajectory {
    public static class State {
        /**
         *
         * Linearly interpolates between two values.
         *
         * @param startValue The start value.
         * @param endValue   The end value.
         * @param t          The fraction for interpolation.
         * @return The interpolated value.
         */
        @SuppressWarnings("ParameterName")
        public static double lerp(double startValue, double endValue, double t) {
            return startValue + (endValue - startValue) * t;
        }
    }
}
