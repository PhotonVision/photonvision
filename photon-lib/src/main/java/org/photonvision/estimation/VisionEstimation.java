/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

package org.photonvision.estimation;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

public class VisionEstimation {
    /** Get the visible {@link AprilTag}s according the tag layout using the visible tag IDs. */
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
     * Performs solvePNP using 3d-2d point correspondences to estimate the field-to-camera
     * transformation. If only one tag is visible, the result may have an alternate solution.
     *
     * <p><b>Note:</b> The returned transformation is from the field origin to the camera pose!
     * (Unless you only feed this one tag??)
     *
     * @param cameraMatrix The camera intrinsics matrix in standard opencv form
     * @param distCoeffs The camera distortion matrix in standard opencv form
     * @param visTags The visible tags reported by PV
     * @param tagLayout The known tag layout on the field
     * @return The transformation that maps the field origin to the camera pose
     */
    @Deprecated
    public static PNPResults estimateCamPosePNP(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            List<PhotonTrackedTarget> visTags,
            AprilTagFieldLayout tagLayout) {
        if (tagLayout == null
                || visTags == null
                || tagLayout.getTags().size() == 0
                || visTags.size() == 0) {
            return new PNPResults();
        }

        var corners = new ArrayList<TargetCorner>();
        for (var tag : visTags) corners.addAll(tag.getDetectedCorners());
        var knownTags = getVisibleLayoutTags(visTags, tagLayout);
        if (knownTags.size() == 0 || corners.size() == 0 || corners.size() % 4 != 0) {
            return new PNPResults();
        }

        // single-tag pnp
        if (visTags.size() == 1) {
            var camToTag =
                    OpenCVHelp.solvePNP_SQUARE(
                            cameraMatrix, distCoeffs, TargetModel.kTag16h5.vertices, corners);
            var bestPose = knownTags.get(0).pose.transformBy(camToTag.best.inverse());
            var altPose = new Pose3d();
            if (camToTag.ambiguity != 0)
                altPose = knownTags.get(0).pose.transformBy(camToTag.alt.inverse());

            var bestTagToCam = camToTag.best.inverse();
            SmartDashboard.putNumberArray(
                    "multiTagBest_internal",
                    new double[] {
                        bestTagToCam.getX(),
                        bestTagToCam.getY(),
                        bestTagToCam.getZ(),
                        bestTagToCam.getRotation().getQuaternion().getW(),
                        bestTagToCam.getRotation().getQuaternion().getX(),
                        bestTagToCam.getRotation().getQuaternion().getY(),
                        bestTagToCam.getRotation().getQuaternion().getZ()
                    });

            var o = new Pose3d();
            return new PNPResults(
                    new Transform3d(o, bestPose),
                    new Transform3d(o, altPose),
                    camToTag.ambiguity,
                    camToTag.bestReprojErr,
                    camToTag.altReprojErr);
        }
        // multi-tag pnp
        else {
            var objectTrls = new ArrayList<Translation3d>();
            for (var tag : knownTags) objectTrls.addAll(TargetModel.kTag16h5.getFieldVertices(tag.pose));
            var camToOrigin = OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, corners);
            return new PNPResults(
                    camToOrigin.best.inverse(),
                    camToOrigin.alt.inverse(),
                    camToOrigin.ambiguity,
                    camToOrigin.bestReprojErr,
                    camToOrigin.altReprojErr);
        }
    }

    /**
     * Performs solvePNP using 3d-2d point correspondences to estimate the field-to-camera
     * transformation. If only one tag is visible, the result may have an alternate solution.
     *
     * <p><b>Note:</b> The returned transformation is from the field origin to the camera pose!
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param corners The visible tag corners in the 2d image
     * @param knownTags The known tag field poses corresponding to the visible tag IDs
     * @return The transformation that maps the field origin to the camera pose
     */
    public static PNPResults estimateCamPoseSqpnp(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            List<TargetCorner> corners,
            List<AprilTag> knownTags) {
        if (knownTags == null
                || corners == null
                || corners.size() != knownTags.size() * 4
                || knownTags.size() == 0) {
            return new PNPResults();
        }
        var objectTrls = new ArrayList<Translation3d>();
        for (var tag : knownTags) objectTrls.addAll(TargetModel.kTag16h5.vertices);
        var camToOrigin = OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, corners);
        // var camToOrigin = OpenCVHelp.solveTagsPNPRansac(prop, objectTrls, corners);
        return new PNPResults(
                camToOrigin.best.inverse(),
                camToOrigin.alt.inverse(),
                camToOrigin.ambiguity,
                camToOrigin.bestReprojErr,
                camToOrigin.altReprojErr);
    }

    /**
     * The best estimated transformation (Rotation-translation composition) that maps a set of
     * translations onto another with point correspondences, and its RMSE.
     */
    public static class SVDResults {
        public final RotTrlTransform3d trf;

        /** If the result is invalid, this value is -1 */
        public final double rmse;

        public SVDResults() {
            this(new RotTrlTransform3d(), -1);
        }

        public SVDResults(RotTrlTransform3d trf, double rmse) {
            this.trf = trf;
            this.rmse = rmse;
        }
    }
}
