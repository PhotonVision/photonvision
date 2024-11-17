import numpy as np
from robotpy_apriltag import AprilTag, AprilTagFieldLayout
from wpimath.geometry import Pose3d, Transform3d, Translation3d

from ..targeting import PhotonTrackedTarget, PnpResult, TargetCorner
from . import OpenCVHelp, TargetModel


class VisionEstimation:
    @staticmethod
    def getVisibleLayoutTags(
        visTags: list[PhotonTrackedTarget], layout: AprilTagFieldLayout
    ) -> list[AprilTag]:
        """Get the visible :class:`.AprilTag`s which are in the tag layout using the visible tag IDs."""
        retVal: list[AprilTag] = []
        for tag in visTags:
            id = tag.getFiducialId()
            maybePose = layout.getTagPose(id)
            if maybePose:
                aprilTag = AprilTag()
                aprilTag.ID = id
                aprilTag.pose = maybePose
                retVal.append(aprilTag)
        return retVal

    @staticmethod
    def estimateCamPosePNP(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        visTags: list[PhotonTrackedTarget],
        layout: AprilTagFieldLayout,
        tagModel: TargetModel,
    ) -> PnpResult | None:
        """Performs solvePNP using 3d-2d point correspondences of visible AprilTags to estimate the
        field-to-camera transformation. If only one tag is visible, the result may have an alternate
        solution.

        **Note:** The returned transformation is from the field origin to the camera pose!

        With only one tag: {@link OpenCVHelp#solvePNP_SQUARE}

        With multiple tags: {@link OpenCVHelp#solvePNP_SQPNP}

        :param cameraMatrix: The camera intrinsics matrix in standard opencv form
        :param distCoeffs:   The camera distortion matrix in standard opencv form
        :param visTags:      The visible tags reported by PV. Non-tag targets are automatically excluded.
        :param tagLayout:    The known tag layout on the field

        :returns: The transformation that maps the field origin to the camera pose. Ensure the {@link
                  PnpResult} are present before utilizing them.
        """
        if len(visTags) == 0:
            return None

        corners: list[TargetCorner] = []
        knownTags: list[AprilTag] = []

        # ensure these are AprilTags in our layout
        for tgt in visTags:
            id = tgt.getFiducialId()
            maybePose = layout.getTagPose(id)
            if maybePose:
                tag = AprilTag()
                tag.ID = id
                tag.pose = maybePose
                knownTags.append(tag)
                currentCorners = tgt.getDetectedCorners()
                if currentCorners:
                    corners += currentCorners

        if len(knownTags) == 0 or len(corners) == 0 or len(corners) % 4 != 0:
            return None

        points = OpenCVHelp.cornersToPoints(corners)

        # single-tag pnp
        if len(knownTags) == 1:
            camToTag = OpenCVHelp.solvePNP_Square(
                cameraMatrix, distCoeffs, tagModel.getVertices(), points
            )
            if not camToTag:
                return None

            bestPose = knownTags[0].pose.transformBy(camToTag.best.inverse())
            altPose = Pose3d()
            if camToTag.ambiguity != 0:
                altPose = knownTags[0].pose.transformBy(camToTag.alt.inverse())

            o = Pose3d()
            result = PnpResult(
                best=Transform3d(o, bestPose),
                alt=Transform3d(o, altPose),
                ambiguity=camToTag.ambiguity,
                bestReprojErr=camToTag.bestReprojErr,
                altReprojErr=camToTag.altReprojErr,
            )
            return result
        # multi-tag pnp
        else:
            objectTrls: list[Translation3d] = []
            for tag in knownTags:
                verts = tagModel.getFieldVertices(tag.pose)
                objectTrls += verts

            ret = OpenCVHelp.solvePNP_SQPNP(
                cameraMatrix, distCoeffs, objectTrls, points
            )
            if ret:
                # Invert best/alt transforms
                ret.best = ret.best.inverse()
                ret.alt = ret.alt.inverse()

            return ret
