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

package org.photonvision.estimation;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ejml.simple.SimpleMatrix;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.photonvision.jni.ConstrainedSolvepnpJni;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.PnpResult;
import org.photonvision.targeting.TargetCorner;

public class VisionEstimation {
    /** Get the visible {@link AprilTag}s which are in the tag layout using the visible tag IDs. */
    public static List<AprilTag> getVisibleLayoutTags(
            List<PhotonTrackedTarget> visTags, AprilTagFieldLayout tagLayout) {
        return visTags.stream()
                .map(
                        t -> {
                            int id = t.getFiducialId();
                            var maybePose = tagLayout.getTagPose(id);
                            return maybePose.map(pose3d -> new AprilTag(id, pose3d)).orElse(null);
                        })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Performs solvePNP using 3d-2d point correspondences of visible AprilTags to estimate the
     * field-to-camera transformation. If only one tag is visible, the result may have an alternate
     * solution.
     *
     * <p><b>Note:</b> The returned transformation is from the field origin to the camera pose!
     *
     * <p>With only one tag: {@link OpenCVHelp#solvePNP_SQUARE}
     *
     * <p>With multiple tags: {@link OpenCVHelp#solvePNP_SQPNP}
     *
     * @param cameraMatrix The camera intrinsics matrix in standard opencv form
     * @param distCoeffs The camera distortion matrix in standard opencv form
     * @param visTags The visible tags reported by PV. Non-tag targets are automatically excluded.
     * @param tagLayout The known tag layout on the field
     * @return The transformation that maps the field origin to the camera pose. Ensure the {@link
     *     PnpResult} are present before utilizing them.
     */
    public static Optional<PnpResult> estimateCamPosePNP(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N8, N1> distCoeffs,
            List<PhotonTrackedTarget> visTags,
            AprilTagFieldLayout tagLayout,
            TargetModel tagModel) {
        if (tagLayout == null
                || visTags == null
                || tagLayout.getTags().isEmpty()
                || visTags.isEmpty()) {
            return Optional.empty();
        }

        var corners = new ArrayList<TargetCorner>();
        var knownTags = new ArrayList<AprilTag>();
        // ensure these are AprilTags in our layout
        for (var tgt : visTags) {
            int id = tgt.getFiducialId();
            tagLayout
                    .getTagPose(id)
                    .ifPresent(
                            pose -> {
                                knownTags.add(new AprilTag(id, pose));
                                corners.addAll(tgt.getDetectedCorners());
                            });
        }
        if (knownTags.isEmpty() || corners.isEmpty() || corners.size() % 4 != 0) {
            return Optional.empty();
        }
        Point[] points = OpenCVHelp.cornersToPoints(corners);

        // single-tag pnp
        if (knownTags.size() == 1) {
            var camToTag =
                    OpenCVHelp.solvePNP_SQUARE(cameraMatrix, distCoeffs, tagModel.vertices, points);
            if (!camToTag.isPresent()) return Optional.empty();
            var bestPose = knownTags.get(0).pose.transformBy(camToTag.get().best.inverse());
            var altPose = new Pose3d();
            if (camToTag.get().ambiguity != 0)
                altPose = knownTags.get(0).pose.transformBy(camToTag.get().alt.inverse());

            var o = new Pose3d();
            return Optional.of(
                    new PnpResult(
                            new Transform3d(o, bestPose),
                            new Transform3d(o, altPose),
                            camToTag.get().ambiguity,
                            camToTag.get().bestReprojErr,
                            camToTag.get().altReprojErr));
        }
        // multi-tag pnp
        else {
            var objectTrls = new ArrayList<Translation3d>();
            for (var tag : knownTags) objectTrls.addAll(tagModel.getFieldVertices(tag.pose));
            var camToOrigin = OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, points);
            if (camToOrigin.isEmpty()) return Optional.empty();
            return Optional.of(
                    new PnpResult(
                            camToOrigin.get().best.inverse(),
                            camToOrigin.get().alt.inverse(),
                            camToOrigin.get().ambiguity,
                            camToOrigin.get().bestReprojErr,
                            camToOrigin.get().altReprojErr));
        }
    }

    /**
     * Performs constrained solvePNP using 3d-2d point correspondences of visible AprilTags to
     * estimate the field-to-camera transformation.
     *
     * <p><b>Note:</b> The returned transformation is from the field origin to the robot drivebase
     *
     * @param cameraMatrix The camera intrinsics matrix in standard opencv form
     * @param distCoeffs The camera distortion matrix in standard opencv form
     * @param visTags The visible tags reported by PV. Non-tag targets are automatically excluded.
     * @param robot2camera The {@link Transform3d} from the robot odometry frame to the camera optical
     *     frame
     * @param robotPoseSeed An initial guess at robot pose, refined via optimizaiton. Better guesses
     *     will converge faster.
     * @param tagLayout The known tag layout on the field
     * @param tagModel The physical size of the AprilTags
     * @param headingFree If heading is completely free, or if our measured gyroθ is taken into
     *     account
     * @param gyroθ If headingFree is false, the best estimate at robot yaw. Excursions from this are
     *     penalized in our cost function.
     * @param gyroErrorScaleFac A relative weight for gyro heading excursions against tag corner
     *     reprojection error.
     * @return The transformation that maps the field origin to the camera pose. Ensure the {@link
     *     PnpResult} are present before utilizing them.
     */
    public static Optional<PnpResult> estimateRobotPoseConstrainedSolvepnp(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N8, N1> distCoeffs,
            List<PhotonTrackedTarget> visTags,
            Transform3d robot2camera,
            Pose3d robotPoseSeed,
            AprilTagFieldLayout tagLayout,
            TargetModel tagModel,
            boolean headingFree,
            Rotation2d gyroθ,
            double gyroErrorScaleFac) {
        if (tagLayout == null
                || visTags == null
                || tagLayout.getTags().isEmpty()
                || visTags.isEmpty()) {
            return Optional.empty();
        }

        var corners = new ArrayList<TargetCorner>();
        var knownTags = new ArrayList<AprilTag>();
        // ensure these are AprilTags in our layout
        for (var tgt : visTags) {
            int id = tgt.getFiducialId();
            tagLayout
                    .getTagPose(id)
                    .ifPresent(
                            pose -> {
                                knownTags.add(new AprilTag(id, pose));
                                corners.addAll(tgt.getDetectedCorners());
                            });
        }
        if (knownTags.isEmpty() || corners.isEmpty() || corners.size() % 4 != 0) {
            return Optional.empty();
        }
        Point[] points = OpenCVHelp.cornersToPoints(corners);

        // Undistort
        {
            MatOfPoint2f temp = new MatOfPoint2f();
            MatOfDouble cameraMatrixMat = new MatOfDouble();
            MatOfDouble distCoeffsMat = new MatOfDouble();
            OpenCVHelp.matrixToMat(cameraMatrix.getStorage()).assignTo(cameraMatrixMat);
            OpenCVHelp.matrixToMat(distCoeffs.getStorage()).assignTo(distCoeffsMat);

            temp.fromArray(points);
            Calib3d.undistortImagePoints(temp, temp, cameraMatrixMat, distCoeffsMat);
            points = temp.toArray();

            temp.release();
            cameraMatrixMat.release();
            distCoeffsMat.release();
        }

        // Rotate from wpilib to opencv camera CS
        var robot2cameraBase =
                MatBuilder.fill(Nat.N4(), Nat.N4(), 0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 1);
        var robotToCamera = robot2camera.toMatrix().times(robot2cameraBase);

        // Where we saw corners
        var point_observations = new SimpleMatrix(2, points.length);
        for (int i = 0; i < points.length; i++) {
            point_observations.set(0, i, points[i].x);
            point_observations.set(1, i, points[i].y);
        }

        // Affine corner locations
        var objectTrls = new ArrayList<Translation3d>();
        for (var tag : knownTags) objectTrls.addAll(tagModel.getFieldVertices(tag.pose));
        var field2points = new SimpleMatrix(4, points.length);
        for (int i = 0; i < objectTrls.size(); i++) {
            field2points.set(0, i, objectTrls.get(i).getX());
            field2points.set(1, i, objectTrls.get(i).getY());
            field2points.set(2, i, objectTrls.get(i).getZ());
            field2points.set(3, i, 1);
        }

        // fx fy cx cy
        double[] cameraCal =
                new double[] {
                    cameraMatrix.get(0, 0),
                    cameraMatrix.get(1, 1),
                    cameraMatrix.get(0, 2),
                    cameraMatrix.get(1, 2),
                };

        var guess2 = robotPoseSeed.toPose2d();

        var ret =
                ConstrainedSolvepnpJni.do_optimization(
                        headingFree,
                        knownTags.size(),
                        cameraCal,
                        robotToCamera.getData(),
                        new double[] {
                            guess2.getX(), guess2.getY(), guess2.getRotation().getRadians(),
                        },
                        field2points.getDDRM().getData(),
                        point_observations.getDDRM().getData(),
                        gyroθ.getRadians(),
                        gyroErrorScaleFac);

        if (ret == null) {
            return Optional.empty();
        } else {
            var pnpresult = new PnpResult();
            pnpresult.best = new Transform3d(new Transform2d(ret[0], ret[1], new Rotation2d(ret[2])));
            return Optional.of(pnpresult);
        }
    }
}
