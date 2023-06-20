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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
     * @param visTags The visible tags reported by PV
     * @param tagLayout The known tag layout on the field
     * @return The transformation that maps the field origin to the camera pose, or an empty Optional
     *     if estimation fails.
     */
    public static Optional<PNPResults> estimateCamPosePNP(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            List<PhotonTrackedTarget> visTags,
            AprilTagFieldLayout tagLayout) {
        if (tagLayout == null
                || visTags == null
                || tagLayout.getTags().size() == 0
                || visTags.size() == 0) {
            return Optional.empty();
        }

        var corners = new ArrayList<TargetCorner>();
        for (var tag : visTags) corners.addAll(tag.getDetectedCorners());
        var knownTags = getVisibleLayoutTags(visTags, tagLayout);
        if (knownTags.size() == 0 || corners.size() == 0 || corners.size() % 4 != 0) {
            return Optional.empty();
        }

        // single-tag pnp
        if (visTags.size() == 1) {
            var camToTag =
                    OpenCVHelp.solvePNP_SQUARE(
                                    cameraMatrix, distCoeffs, TargetModel.kTag16h5.vertices, corners)
                            .orElse(null);
            if (camToTag == null) return Optional.empty();
            var bestPose = knownTags.get(0).pose.transformBy(camToTag.best.inverse());
            var altPose = new Pose3d();
            if (camToTag.ambiguity != 0)
                altPose = knownTags.get(0).pose.transformBy(camToTag.alt.inverse());

            var o = new Pose3d();
            return Optional.of(
                    new PNPResults(
                            new Transform3d(o, bestPose),
                            new Transform3d(o, altPose),
                            camToTag.ambiguity,
                            camToTag.bestReprojErr,
                            camToTag.altReprojErr));
        }
        // multi-tag pnp
        else {
            var objectTrls = new ArrayList<Translation3d>();
            for (var tag : knownTags) objectTrls.addAll(TargetModel.kTag16h5.getFieldVertices(tag.pose));
            var camToOrigin =
                    OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, corners).orElse(null);
            if (camToOrigin == null) return Optional.empty();
            return Optional.of(
                    new PNPResults(
                            camToOrigin.best.inverse(),
                            camToOrigin.alt.inverse(),
                            camToOrigin.ambiguity,
                            camToOrigin.bestReprojErr,
                            camToOrigin.altReprojErr));
        }
    }
}
