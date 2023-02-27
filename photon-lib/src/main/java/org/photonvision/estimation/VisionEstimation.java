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

package org.photonvision.estimation;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.targeting.TargetCorner;

public class VisionEstimation {
    public static final TargetModel kTagModel =
            new TargetModel(Units.inchesToMeters(6), Units.inchesToMeters(6));

    /**
     * Performs solvePNP using 3d-2d point correspondences to estimate the field-to-camera
     * transformation. If only one tag is visible, the result may have an alternate solution.
     *
     * <p><b>Note:</b> The returned transformation is from the field origin to the camera pose!
     * (Unless you only feed this one tag??)
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param corners The visible tag corners in the 2d image
     * @param knownTags The known tag field poses corresponding to the visible tag IDs
     * @return The transformation that maps the field origin to the camera pose
     */
    @Deprecated
    public static PNPResults estimateCamPosePNP(
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
        // single-tag pnp
        if (corners.size() == 4) {
            var camToTag =
                    OpenCVHelp.solvePNP_SQUARE(
                            cameraMatrix, distCoeffs, kTagModel.getFieldVertices(knownTags.get(0).pose), corners);
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
            for (var tag : knownTags) objectTrls.addAll(kTagModel.getFieldVertices(tag.pose));
            var camToOrigin = OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, corners);
            // var camToOrigin = OpenCVHelp.solveTagsPNPRansac(prop, objectTrls, corners);
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
        for (var tag : knownTags) objectTrls.addAll(kTagModel.getFieldVertices(tag.pose));
        var camToOrigin = OpenCVHelp.solvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls, corners);
        // var camToOrigin = OpenCVHelp.solveTagsPNPRansac(prop, objectTrls, corners);
        return new PNPResults(
                camToOrigin.best.inverse(),
                camToOrigin.alt.inverse(),
                camToOrigin.ambiguity,
                camToOrigin.bestReprojErr,
                camToOrigin.altReprojErr);
    }

    // /**
    // * The best estimated transformation to the target, and possibly an alternate
    // * transformation
    // * depending on the solvePNP method. If an alternate solution is present, the
    // * ambiguity value
    // * represents the ratio of reprojection error in the best solution to the
    // * alternate (best / alternate).
    // */
    // public static class PNPResults {
    // public final Transform3d best;
    // public final double bestReprojErr;

    // /**
    // * Alternate, ambiguous solution from solvepnp. This may be empty
    // * if no alternate solution is found.
    // */
    // public final Transform3d alt;
    // /** If no alternate solution is found, this is 0 */
    // public final double altReprojErr;

    // /** If no alternate solution is found, this is 0 */
    // public final double ambiguity;

    // public PNPResults() {
    // this(new Transform3d(), new Transform3d(), 0, 0, 0);
    // }

    // public PNPResults(
    // Transform3d best, Transform3d alt,
    // double ambiguity, double bestReprojErr, double altReprojErr) {
    // this.best = best;
    // this.alt = alt;
    // this.ambiguity = ambiguity;
    // this.bestReprojErr = bestReprojErr;
    // this.altReprojErr = altReprojErr;
    // }
    // }

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
