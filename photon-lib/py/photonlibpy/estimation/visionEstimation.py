from . import OpenCVHelp, TargetModel
from ..targeting import PhotonTrackedTarget, PnpResult, TargetCorner

from wpimath.geometry import Pose3d, Transform3d, Translation3d

from robotpy_apriltag import AprilTag, AprilTagFieldLayout

import numpy as np


class VisionEstimation:
    @staticmethod
    def getVisibleLayoutTags(
        visTags: list[PhotonTrackedTarget], layout: AprilTagFieldLayout
    ) -> list[AprilTag]:
        retVal: list[AprilTag] = []
        for tag in visTags:
            id = tag.getFiducialId()
            maybePose = layout.getTagPose(id)
            if maybePose:
                tag = AprilTag()
                tag.ID = id
                tag.pose = maybePose
                retVal.append(tag)
        return retVal

    @staticmethod
    def estimateCamPosePNP(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        visTags: list[PhotonTrackedTarget],
        layout: AprilTagFieldLayout,
        tagModel: TargetModel,
    ) -> PnpResult | None:
        if len(visTags) == 0:
            return None

        corners: list[TargetCorner] = []
        knownTags: list[AprilTag] = []

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
