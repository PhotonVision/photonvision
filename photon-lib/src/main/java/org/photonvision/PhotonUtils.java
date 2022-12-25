/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision;

import edu.wpi.first.math.geometry.*;

public final class PhotonUtils {
    private PhotonUtils() {
        // Utility class
    }

    /**
     * Algorithm from https://docs.limelightvision.io/en/latest/cs_estimating_distance.html Estimates
     * range to a target using the target's elevation. This method can produce more stable results
     * than SolvePNP when well tuned, if the full 6d robot pose is not required. Note that this method
     * requires the camera to have 0 roll (not be skewed clockwise or CCW relative to the floor), and
     * for there to exist a height differential between goal and camera. The larger this differential,
     * the more accurate the distance estimate will be.
     *
     * <p>Units can be converted using the {@link edu.wpi.first.math.util.Units} class.
     *
     * @param cameraHeightMeters The physical height of the camera off the floor in meters.
     * @param targetHeightMeters The physical height of the target off the floor in meters. This
     *     should be the height of whatever is being targeted (i.e. if the targeting region is set to
     *     top, this should be the height of the top of the target).
     * @param cameraPitchRadians The pitch of the camera from the horizontal plane in radians.
     *     Positive values up.
     * @param targetPitchRadians The pitch of the target in the camera's lens in radians. Positive
     *     values up.
     * @return The estimated distance to the target in meters.
     */
    public static double calculateDistanceToTargetMeters(
            double cameraHeightMeters,
            double targetHeightMeters,
            double cameraPitchRadians,
            double targetPitchRadians) {
        return (targetHeightMeters - cameraHeightMeters)
                / Math.tan(cameraPitchRadians + targetPitchRadians);
    }

    /**
     * Estimate the {@link Translation2d} of the target relative to the camera.
     *
     * @param targetDistanceMeters The distance to the target in meters.
     * @param yaw The observed yaw of the target.
     * @return The target's camera-relative translation.
     */
    public static Translation2d estimateCameraToTargetTranslation(
            double targetDistanceMeters, Rotation2d yaw) {
        return new Translation2d(
                yaw.getCos() * targetDistanceMeters, yaw.getSin() * targetDistanceMeters);
    }

    /**
     * Estimate the position of the robot in the field.
     *
     * @param cameraHeightMeters The physical height of the camera off the floor in meters.
     * @param targetHeightMeters The physical height of the target off the floor in meters. This
     *     should be the height of whatever is being targeted (i.e. if the targeting region is set to
     *     top, this should be the height of the top of the target).
     * @param cameraPitchRadians The pitch of the camera from the horizontal plane in radians.
     *     Positive values up.
     * @param targetPitchRadians The pitch of the target in the camera's lens in radians. Positive
     *     values up.
     * @param targetYaw The observed yaw of the target. Note that this *must* be CCW-positive, and
     *     Photon returns CW-positive.
     * @param gyroAngle The current robot gyro angle, likely from odometry.
     * @param fieldToTarget A Pose2d representing the target position in the field coordinate system.
     * @param cameraToRobot The position of the robot relative to the camera. If the camera was
     *     mounted 3 inches behind the "origin" (usually physical center) of the robot, this would be
     *     Transform2d(3 inches, 0 inches, 0 degrees).
     * @return The position of the robot in the field.
     */
    public static Pose2d estimateFieldToRobot(
            double cameraHeightMeters,
            double targetHeightMeters,
            double cameraPitchRadians,
            double targetPitchRadians,
            Rotation2d targetYaw,
            Rotation2d gyroAngle,
            Pose2d fieldToTarget,
            Transform2d cameraToRobot) {
        return PhotonUtils.estimateFieldToRobot(
                PhotonUtils.estimateCameraToTarget(
                        PhotonUtils.estimateCameraToTargetTranslation(
                                PhotonUtils.calculateDistanceToTargetMeters(
                                        cameraHeightMeters, targetHeightMeters, cameraPitchRadians, targetPitchRadians),
                                targetYaw),
                        fieldToTarget,
                        gyroAngle),
                fieldToTarget,
                cameraToRobot);
    }

    /**
     * Estimates a {@link Transform2d} that maps the camera position to the target position, using the
     * robot's gyro. Note that the gyro angle provided *must* line up with the field coordinate system
     * -- that is, it should read zero degrees when pointed towards the opposing alliance station, and
     * increase as the robot rotates CCW.
     *
     * @param cameraToTargetTranslation A Translation2d that encodes the x/y position of the target
     *     relative to the camera.
     * @param fieldToTarget A Pose2d representing the target position in the field coordinate system.
     * @param gyroAngle The current robot gyro angle, likely from odometry.
     * @return A Transform2d that takes us from the camera to the target.
     */
    public static Transform2d estimateCameraToTarget(
            Translation2d cameraToTargetTranslation, Pose2d fieldToTarget, Rotation2d gyroAngle) {
        // This pose maps our camera at the origin out to our target, in the robot
        // reference frame
        // The translation part of this Transform2d is from the above step, and the
        // rotation uses our robot's
        // gyro.
        return new Transform2d(
                cameraToTargetTranslation, gyroAngle.times(-1).minus(fieldToTarget.getRotation()));
    }

    /**
     * Estimates the pose of the robot in the field coordinate system, given the position of the
     * target relative to the camera, the target relative to the field, and the robot relative to the
     * camera.
     *
     * @param cameraToTarget The position of the target relative to the camera.
     * @param fieldToTarget The position of the target in the field.
     * @param cameraToRobot The position of the robot relative to the camera. If the camera was
     *     mounted 3 inches behind the "origin" (usually physical center) of the robot, this would be
     *     Transform2d(3 inches, 0 inches, 0 degrees).
     * @return The position of the robot in the field.
     */
    public static Pose2d estimateFieldToRobot(
            Transform2d cameraToTarget, Pose2d fieldToTarget, Transform2d cameraToRobot) {
        return estimateFieldToCamera(cameraToTarget, fieldToTarget).transformBy(cameraToRobot);
    }

    /**
     * Estimates the pose of the camera in the field coordinate system, given the position of the
     * target relative to the camera, and the target relative to the field. This *only* tracks the
     * position of the camera, not the position of the robot itself.
     *
     * @param cameraToTarget The position of the target relative to the camera.
     * @param fieldToTarget The position of the target in the field.
     * @return The position of the camera in the field.
     */
    public static Pose2d estimateFieldToCamera(Transform2d cameraToTarget, Pose2d fieldToTarget) {
        var targetToCamera = cameraToTarget.inverse();
        return fieldToTarget.transformBy(targetToCamera);
    }

    /**
     * Estimates the pose of the robot in the field coordinate system, given the pose of the fiducial
     * tag, the robot relative to the camera, and the target relative to the camera.
     *
     * @param fieldRelativeTagPose Pose3D the field relative pose of the target
     * @param cameraToRobot Transform3D of the robot relative to the camera. Origin of the robot is
     *     defined as the center.
     * @param cameraToTarget Transform3D of the target relative to the camera, returned by
     *     PhotonVision
     * @return Transform3d Robot position relative to the field
     */
    public static Pose3d estimateFieldToRobotAprilTag(
            Transform3d cameraToTarget, Pose3d fieldRelativeTagPose, Transform3d cameraToRobot) {
        return fieldRelativeTagPose.plus(cameraToTarget.inverse()).plus(cameraToRobot);
    }

    /**
     * Returns the yaw between your robot and a target.
     *
     * @param robotPose Current pose of the robot
     * @param targetPose Pose of the target on the field
     * @return double Yaw to the target
     */
    public static Rotation2d getYawToPose(Pose2d robotPose, Pose2d targetPose) {
        Translation2d relativeTrl = targetPose.relativeTo(robotPose).getTranslation();
        return new Rotation2d(relativeTrl.getX(), relativeTrl.getY());
    }

    /**
     * Returns the distance between two poses
     *
     * @param robotPose Pose of the robot
     * @param targetPose Pose of the target
     * @return
     */
    public static double getDistanceToPose(Pose2d robotPose, Pose2d targetPose) {
        return robotPose.getTranslation().getDistance(targetPose.getTranslation());
    }
}
