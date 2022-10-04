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

import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.geometry.CoordinateSystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.util.WPIUtilJNI;
import java.util.Arrays;
import java.util.List;
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

    public static double map(
            double value, double in_min, double in_max, double out_min, double out_max) {
        return (value - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static int map(int value, int inMin, int inMax, int outMin, int outMax) {
        return (int) Math.floor(map((double) value, inMin, inMax, outMin, outMax) + 0.5);
    }

    public static long wpiNanoTime() {
        return microsToNanos(WPIUtilJNI.now());
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

        if (list.size() == 0) {
            return Double.NaN;
        }
        if (list.size() == 1) {
            return list.get(0); // always return single value for n = 1
        }

        // Sort array.  We avoid a third copy here by just creating the
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

    public static Pose3d EDNtoNWU(final Pose3d pose) {
        // Change of basis matrix from EDN to NWU. Each column vector is one of the
        // old basis vectors mapped to its representation in the new basis.
        //
        // E (+X) -> N (-Y), D (+Y) -> W (-Z), N (+Z) -> U (+X)
        var R = new MatBuilder<>(Nat.N3(), Nat.N3()).fill(0, 0, 1, -1, 0, 0, 0, -1, 0);

        // https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
        double w = Math.sqrt(1.0 + R.get(0, 0) + R.get(1, 1) + R.get(2, 2)) / 2.0;
        double x = (R.get(2, 1) - R.get(1, 2)) / (4.0 * w);
        double y = (R.get(0, 2) - R.get(2, 0)) / (4.0 * w);
        double z = (R.get(1, 0) - R.get(0, 1)) / (4.0 * w);
        var rotationQuat = new Rotation3d(new Quaternion(w, x, y, z));

        return new Pose3d(
                pose.getTranslation().rotateBy(rotationQuat), pose.getRotation().rotateBy(rotationQuat));
    }

    // TODO: Refactor into new pipe?
    public static Pose3d convertOpenCVtoPhotonPose(Transform3d cameraToTarget3d) {
        // CameraToTarget _should_ be in opencv-land EDN
        return CoordinateSystem.convert(
                new Pose3d(cameraToTarget3d), CoordinateSystem.EDN(), CoordinateSystem.NWU());
    }

    // Apply an extra rotation so that at zero pose, X ls left, Y is up, and Z is towards the camera
    // to a camera facing along the +X axis of the field parallel with the ground plane
    // So we need a 180 flip about X axis
    private static final Vector<N3> APRILTAG_AXIS = VecBuilder.fill(1, 0, 0);
    private static final double APRILTAG_ANGLE = Units.degreesToRadians(180);
    private static final Rotation3d APRILTAG_TRANSFORM =
            new Rotation3d(APRILTAG_AXIS, APRILTAG_ANGLE);

    public static Pose3d convertApriltagtoOpencv(Pose3d pose) {
        var ocvRotation = APRILTAG_TRANSFORM.rotateBy(pose.getRotation());
        return new Pose3d(pose.getTranslation(), ocvRotation);
    }

    public static void rotationToOpencvRvec(Rotation3d rotation, Mat rvecOutput) {
        var angle = rotation.getAngle();
        var axis = rotation.getAxis().times(angle);
        rvecOutput.put(0, 0, axis.getData());
    }
}
