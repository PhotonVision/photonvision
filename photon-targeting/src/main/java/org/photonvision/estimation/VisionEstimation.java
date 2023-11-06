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
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opencv.core.Point;
import org.photonvision.targeting.PNPResults;
import org.photonvision.targeting.PhotonTrackedTarget;
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
     *     PNPResults} are present before utilizing them.
     */
    public static PNPResults estimateCamPosePNP(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            List<PhotonTrackedTarget> visTags,
            AprilTagFieldLayout tagLayout,
            TargetModel tagModel) {
        if (tagLayout == null
                || visTags == null
                || tagLayout.getTags().size() == 0
                || visTags.size() == 0) {
            return new PNPResults();
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
        if (knownTags.size() == 0 || corners.size() == 0 || corners.size() % 4 != 0) {
            return new PNPResults();
        }
        Point[] points = OpenCVHelp.cornersToPoints(corners);

        // single-tag pnp
        if (knownTags.size() == 1) {
            var camToTag =
                    OpenCVHelp.solvePNP_SQUARE(cameraMatrix, distCoeffs, tagModel.vertices, points);
            if (!camToTag.isPresent) return new PNPResults();
            var bestPose = knownTags.get(0).pose.transformBy(camToTag.best.inverse());
            var altPose = new Pose3d();
            if (camToTag.ambiguity != 0)
                altPose = knownTags.get(0).pose.transformBy(camToTag.alt.inverse());

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
            for (var tag : knownTags) objectTrls.addAll(tagModel.getFieldVertices(tag.pose));
            var camToOrigin = OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, points);
            if (!camToOrigin.isPresent) return new PNPResults();
            return new PNPResults(
                    camToOrigin.best.inverse(),
                    camToOrigin.alt.inverse(),
                    camToOrigin.ambiguity,
                    camToOrigin.bestReprojErr,
                    camToOrigin.altReprojErr);
        }
    }
}
