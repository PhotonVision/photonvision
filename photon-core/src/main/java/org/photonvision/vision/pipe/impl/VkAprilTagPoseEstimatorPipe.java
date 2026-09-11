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

package org.photonvision.vision.pipe.impl;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams;
import org.photonvision.vkapriltag.VkAprilTagPoseEstimatorJNI;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.util.Nat;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagPoseEstimate;
import org.wpilib.vision.apriltag.AprilTagPoseEstimator.Config;

/**
 * EXPERIMENTAL. A {@link SingleTagPoseEstimator} backed by {@code vkapriltag}'s native {@code
 * PoseEstimator} - a from-scratch, allocation-free rewrite of libapriltag's {@code
 * apriltag_pose.c} - instead of WPILib's own CPU/JNI implementation. Measured ~58x faster than
 * unmodified libapriltag per tag on Mali-G610/RK3588; that figure is NOT a comparison against
 * WPILib's own estimator (what {@link AprilTagPoseEstimatorPipe} actually uses today), so the
 * real-world benefit here is unverified until benchmarked - see {@code
 * VkAprilTagPipelineSettings#poseEstimatorBackend}'s Javadoc.
 *
 * <p>Replicates {@link AprilTagPoseEstimatorPipe#process}'s exact undistortion contract: the
 * detection's 4 corners are undistorted before being handed to the native estimator, but the
 * original (not re-derived) homography is passed through unchanged - matching what WPILib's own
 * estimator consumes today.
 *
 * <p>Only replaces single-tag pose estimation. {@code MultiTargetPNPPipe} (multi-tag bundle
 * solve) is unaffected - vkapriltag's PoseEstimator has no multi-tag capability.
 */
public class VkAprilTagPoseEstimatorPipe
        extends CVPipe<AprilTagDetection, AprilTagPoseEstimate, AprilTagPoseEstimatorPipeParams>
        implements SingleTagPoseEstimator {
    private static final Logger logger =
            new Logger(VkAprilTagPoseEstimatorPipe.class, LogGroup.VisionModule);

    private static final Cleaner cleaner = Cleaner.create();

    // Matches VkAprilTagJNI.detect()'s wire format (leading tag count, then 22 doubles/tag) - the
    // native pose estimator's estimatePoses() is built to consume that format directly, so a
    // single detection here is packed the same way rather than inventing a second layout.
    private static final int DOUBLES_PER_DETECTION = 22;

    private Cleanable cleanable;
    private long nativeHandle = 0;

    private final MatOfPoint2f cornersMat = new MatOfPoint2f();

    @Override
    public void setParams(AprilTagPoseEstimatorPipeParams newParams) {
        if (this.params == null || !this.params.config().equals(newParams.config())) {
            releaseNative();
            Config c = newParams.config();
            long handle = VkAprilTagPoseEstimatorJNI.create(c.fx, c.fy, c.cx, c.cy, c.tagSize, 0);
            if (handle == 0) {
                logger.error(
                        "VkAprilTagPoseEstimatorJNI.create() failed: "
                                + VkAprilTagPoseEstimatorJNI.getLastError()
                                + " - pose estimates will be zeroed until params change again");
            } else {
                nativeHandle = handle;
                final long handleToClean = handle;
                cleanable = cleaner.register(this, () -> VkAprilTagPoseEstimatorJNI.destroy(handleToClean));
            }
        }
        super.setParams(newParams);
    }

    @Override
    protected AprilTagPoseEstimate process(AprilTagDetection in) {
        if (nativeHandle == 0) {
            logger.error("VkAprilTagPoseEstimatorPipe used with no live native pose estimator");
            return new AprilTagPoseEstimate(new Transform3d(), new Transform3d(), 0, 0);
        }

        // Same undistort-then-build-a-corrected-detection contract AprilTagPoseEstimatorPipe uses:
        // undistort corners, but leave the homography as-is (see this class's Javadoc).
        Point[] corners = new Point[4];
        for (int i = 0; i < 4; i++) {
            corners[i] = new Point(in.getCornerX(i), in.getCornerY(i));
        }
        cornersMat.fromArray(corners);
        Calib3d.undistortImagePoints(
                cornersMat,
                cornersMat,
                params.calibration().getCameraIntrinsicsMat(),
                params.calibration().getDistCoeffsMat());
        Point[] undistorted = cornersMat.toArray();

        double[] flat = new double[1 + DOUBLES_PER_DETECTION];
        flat[0] = 1; // one detection
        flat[1] = in.getId();
        flat[2] = in.getHamming();
        flat[3] = in.getDecisionMargin();
        flat[4] = in.getCenterX();
        flat[5] = in.getCenterY();
        for (int i = 0; i < 4; i++) {
            flat[6 + i * 2] = undistorted[i].x;
            flat[6 + i * 2 + 1] = undistorted[i].y;
        }
        System.arraycopy(in.getHomography(), 0, flat, 14, 9);

        double[] poses = VkAprilTagPoseEstimatorJNI.estimatePoses(nativeHandle, flat);
        if (poses == null) {
            logger.error(
                    "VkAprilTagPoseEstimatorJNI.estimatePoses() failed: "
                            + VkAprilTagPoseEstimatorJNI.getLastError());
            return new AprilTagPoseEstimate(new Transform3d(), new Transform3d(), 0, 0);
        }

        int perPose = VkAprilTagPoseEstimatorJNI.DOUBLES_PER_POSE;
        boolean valid1 = isValid(poses, 0);
        if (!valid1) {
            logger.error("vkapriltag PoseEstimator produced no valid pose for tag " + in.getId());
            return new AprilTagPoseEstimate(new Transform3d(), new Transform3d(), 0, 0);
        }

        Transform3d pose1 = readPose(poses, 0);
        double error1 = poses[12];

        boolean valid2 = isValid(poses, perPose);
        Transform3d pose2 = valid2 ? readPose(poses, perPose) : pose1;
        double error2 = valid2 ? poses[perPose + 12] : error1;

        return new AprilTagPoseEstimate(pose1, pose2, error1, error2);
    }

    private static boolean isValid(double[] poses, int base) {
        return poses[base + 13] != 0.0;
    }

    private static Transform3d readPose(double[] poses, int base) {
        double[] rFlat = new double[9];
        System.arraycopy(poses, base, rFlat, 0, 9);
        var rotation = new Rotation3d(new Matrix<>(Nat.N3(), Nat.N3(), rFlat));
        var translation = new Translation3d(poses[base + 9], poses[base + 10], poses[base + 11]);
        return new Transform3d(translation, rotation);
    }

    private void releaseNative() {
        if (cleanable != null) {
            cleanable.clean();
            cleanable = null;
        }
        nativeHandle = 0;
    }

    @Override
    public void release() {
        releaseNative();
        cornersMat.release();
    }
}
