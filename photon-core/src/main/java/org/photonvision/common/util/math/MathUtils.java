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

package org.photonvision.common.util.math;

import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.CoordinateSystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTablesJNI;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class MathUtils {
    MathUtils() {}

    public static double toSlope(Number angle) {
        return Math.atan(Math.toRadians(angle.doubleValue() - 90));
    }

    public static int safeDivide(int quotient, int divisor) {
        if (divisor == 0) {
            return 0;
        } else {
            return quotient / divisor;
        }
    }

    public static double roundTo(double value, int to) {
        double toMult = Math.pow(10, to);
        return (double) Math.round(value * toMult) / toMult;
    }

    public static double nanosToMillis(long nanos) {
        return nanos / 1000000.0;
    }

    public static double nanosToMillis(double nanos) {
        return nanos / 1000000.0;
    }

    public static long millisToNanos(long millis) {
        return millis * 1000000;
    }

    public static long microsToNanos(long micros) {
        return micros * 1000;
    }

    public static long nanosToMicros(long nanos) {
        return nanos / 1000;
    }

    /**
     * Constrain a value to only take on certain values. Pick the next-highest allowed value in the
     * array if in-between.
     *
     * @param value value to quantize
     * @param allowableSteps sorted array of the allowed values
     * @return quantized value
     */
    public static int quantize(int value, int[] allowableSteps) {
        for (int step : allowableSteps) {
            if (value <= step) {
                return step;
            }
        }
        return allowableSteps[allowableSteps.length - 1];
    }

    public static double map(
            double value, double in_min, double in_max, double out_min, double out_max) {
        return (value - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static int map(int value, int inMin, int inMax, int outMin, int outMax) {
        return (int) Math.floor(map((double) value, inMin, inMax, outMin, outMax) + 0.5);
    }

    public static long wpiNanoTime() {
        return microsToNanos(NetworkTablesJNI.now());
    }

    /**
     * Get the value of the nTh percentile of a list
     *
     * @param list The list to evaluate
     * @param p The percentile, in [0,100]
     * @return
     */
    public static double getPercentile(List<Double> list, double p) {
        if ((p > 100) || (p <= 0)) {
            throw new IllegalArgumentException("invalid quantile value: " + p);
        }

        if (list.isEmpty()) {
            return Double.NaN;
        }
        if (list.size() == 1) {
            return list.get(0); // always return single value for n = 1
        }

        // Sort array. We avoid a third copy here by just creating the
        // list directly.
        double[] sorted = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            sorted[i] = list.get(i);
        }
        Arrays.sort(sorted);

        return evaluateSorted(sorted, p);
    }

    private static double evaluateSorted(final double[] sorted, final double p) {
        double n = sorted.length;
        double pos = p * (n + 1) / 100;
        double fpos = Math.floor(pos);
        int intPos = (int) fpos;
        double dif = pos - fpos;

        if (pos < 1) {
            return sorted[0];
        }
        if (pos >= n) {
            return sorted[sorted.length - 1];
        }
        double lower = sorted[intPos - 1];
        double upper = sorted[intPos];
        return lower + dif * (upper - lower);
    }

    /**
     * Linearly interpolates between two values.
     *
     * @param startValue The start value.
     * @param endValue The end value.
     * @param t The fraction for interpolation.
     * @return The interpolated value.
     */
    @SuppressWarnings("ParameterName")
    public static double lerp(double startValue, double endValue, double t) {
        return startValue + (endValue - startValue) * t;
    }

    /**
     * OpenCV uses the EDN coordinate system, but WPILib uses NWU. Converts a camera-to-target
     * transformation from EDN to NWU.
     *
     * <p>Note: The detected target's rvec and tvec perform a rotation-translation transformation
     * which converts points in the target's coordinate system to the camera's. This means applying
     * the transformation to the target point (0,0,0) for example would give the target's center
     * relative to the camera. Conveniently, if we make a translation-rotation transformation out of
     * these components instead, we get the transformation from the camera to the target.
     *
     * @param cameraToTarget3d A camera-to-target Transform3d in EDN.
     * @return A camera-to-target Transform3d in NWU.
     */
    public static Transform3d convertOpenCVtoPhotonTransform(Transform3d cameraToTarget3d) {
        // TODO: Refactor into new pipe?
        return CoordinateSystem.convert(
                cameraToTarget3d, CoordinateSystem.EDN(), CoordinateSystem.NWU());
    }

    /*
     * From the AprilTag repo:
     * "The coordinate system has the origin at the camera center. The z-axis points from the camera
     * center out the camera lens. The x-axis is to the right in the image taken by the camera, and
     * y is down. The tag's coordinate frame is centered at the center of the tag, with x-axis to the
     * right, y-axis down, and z-axis into the tag."
     *
     * This means our detected transformation will be in EDN. Our subsequent uses of the transformation,
     * however, assume the tag's z-axis point away from the tag instead of into it. This means we
     * have to correct the transformation's rotation.
     */
    private static final Rotation3d APRILTAG_BASE_ROTATION =
            new Rotation3d(VecBuilder.fill(0, 1, 0), Units.degreesToRadians(180));

    /**
     * AprilTag returns a camera-to-tag transform in EDN, but the tag's z-axis points into the tag
     * instead of away from it and towards the camera. This means we have to correct the
     * transformation's rotation.
     *
     * @param pose The Transform3d with translation and rotation directly from the {@link
     *     AprilTagPoseEstimate}.
     */
    public static Transform3d convertApriltagtoOpenCV(Transform3d pose) {
        var ocvRotation = APRILTAG_BASE_ROTATION.rotateBy(pose.getRotation());
        return new Transform3d(pose.getTranslation(), ocvRotation);
    }

    public static Pose3d convertArucotoOpenCV(Transform3d pose) {
        var ocvRotation =
                APRILTAG_BASE_ROTATION.rotateBy(
                        new Rotation3d(VecBuilder.fill(0, 0, 1), Units.degreesToRadians(180))
                                .rotateBy(pose.getRotation()));
        return new Pose3d(pose.getTranslation(), ocvRotation);
    }

    public static void rotationToOpencvRvec(Rotation3d rotation, Mat rvecOutput) {
        var angle = rotation.getAngle();
        var axis = rotation.getAxis().times(angle);
        rvecOutput.put(0, 0, axis.getData());
    }

    /**
     * Convert an Opencv rvec+tvec pair to a Pose3d.
     *
     * @param rVec Axis-angle rotation vector, where norm(rVec) is the angle about a unit vector in
     *     the direction of rVec
     * @param tVec 3D translation
     * @return Pose3d representing the same rigid transform
     */
    public static Pose3d opencvRTtoPose3d(Mat rVec, Mat tVec) {
        Translation3d translation =
                new Translation3d(tVec.get(0, 0)[0], tVec.get(1, 0)[0], tVec.get(2, 0)[0]);
        Rotation3d rotation =
                new Rotation3d(
                        VecBuilder.fill(rVec.get(0, 0)[0], rVec.get(1, 0)[0], rVec.get(2, 0)[0]),
                        Core.norm(rVec));

        return new Pose3d(translation, rotation);
    }
}
