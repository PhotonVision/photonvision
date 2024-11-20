import math
from typing import Any

import cv2 as cv
import numpy as np
from wpimath.geometry import Rotation3d, Transform3d, Translation3d

from ..targeting import PnpResult, TargetCorner
from .rotTrlTransform3d import RotTrlTransform3d

NWU_TO_EDN = Rotation3d(np.array([[0, -1, 0], [0, 0, -1], [1, 0, 0]]))
EDN_TO_NWU = Rotation3d(np.array([[0, 0, 1], [-1, 0, 0], [0, -1, 0]]))


class OpenCVHelp:
    @staticmethod
    def getMinAreaRect(points: np.ndarray) -> cv.RotatedRect:
        return cv.RotatedRect(*cv.minAreaRect(points))

    @staticmethod
    def translationNWUtoEDN(trl: Translation3d) -> Translation3d:
        return trl.rotateBy(NWU_TO_EDN)

    @staticmethod
    def rotationNWUtoEDN(rot: Rotation3d) -> Rotation3d:
        return -NWU_TO_EDN + (rot + NWU_TO_EDN)

    @staticmethod
    def translationToTVec(translations: list[Translation3d]) -> np.ndarray:
        """Creates a new :class:`np.array` with these 3d translations. The opencv tvec is a vector with
        three elements representing {x, y, z} in the EDN coordinate system.

        :param translations: The translations to convert into a np.array
        """

        retVal: list[list] = []
        for translation in translations:
            trl = OpenCVHelp.translationNWUtoEDN(translation)
            retVal.append([trl.X(), trl.Y(), trl.Z()])
        return np.array(
            retVal,
            dtype=np.float32,
        )

    @staticmethod
    def rotationToRVec(rotation: Rotation3d) -> np.ndarray:
        """Creates a new :class:`.np.array` with this 3d rotation. The opencv rvec Mat is a vector with
        three elements representing the axis scaled by the angle in the EDN coordinate system. (angle =
        norm, and axis = rvec / norm)

        :param rotation: The rotation to convert into a np.array
        """

        retVal: list[np.ndarray] = []
        rot = OpenCVHelp.rotationNWUtoEDN(rotation)
        rotVec = rot.getQuaternion().toRotationVector()
        retVal.append(rotVec)
        return np.array(
            retVal,
            dtype=np.float32,
        )

    @staticmethod
    def avgPoint(points: np.ndarray) -> np.ndarray:
        x = 0.0
        y = 0.0
        for p in points:
            x += p[0, 0]
            y += p[0, 1]
        return np.array([[x / len(points), y / len(points)]])

    @staticmethod
    def pointsToTargetCorners(points: np.ndarray) -> list[TargetCorner]:
        corners = [TargetCorner(p[0, 0], p[0, 1]) for p in points]
        return corners

    @staticmethod
    def cornersToPoints(corners: list[TargetCorner]) -> np.ndarray:
        points = [[[c.x, c.y]] for c in corners]
        return np.array(points)

    @staticmethod
    def projectPoints(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        camRt: RotTrlTransform3d,
        objectTranslations: list[Translation3d],
    ) -> np.ndarray:
        objectPoints = OpenCVHelp.translationToTVec(objectTranslations)
        rvec = OpenCVHelp.rotationToRVec(camRt.getRotation())
        tvec = OpenCVHelp.translationToTVec(
            [
                camRt.getTranslation(),
            ]
        )

        pts, _ = cv.projectPoints(objectPoints, rvec, tvec, cameraMatrix, distCoeffs)
        return pts

    @staticmethod
    def reorderCircular(
        elements: list[Any] | np.ndarray, backwards: bool, shiftStart: int
    ) -> list[Any]:
        """Reorders the list, optionally indexing backwards and wrapping around to the last element after
        the first, and shifting all indices in the direction of indexing.

        e.g.

        ({1,2,3}, false, 1) == {2,3,1}

        ({1,2,3}, true, 0) == {1,3,2}

        ({1,2,3}, true, 1) == {3,2,1}

        :param elements:   list elements
        :param backwards:  If indexing should happen in reverse (0, size-1, size-2, ...)
        :param shiftStart: How much the initial index should be shifted (instead of starting at index 0,
                           start at shiftStart, negated if backwards)

        :returns: Reordered list
        """

        size = len(elements)
        reordered = []
        dir = -1 if backwards else 1
        for i in range(size):
            index = (i * dir + shiftStart * dir) % size
            if index < 0:
                index += size
            reordered.append(elements[index])
        return reordered

    @staticmethod
    def translationEDNToNWU(trl: Translation3d) -> Translation3d:
        """Convert a rotation delta from EDN to NWU. For example, if you have a rotation X,Y,Z {1, 0, 0}
        in EDN, this would be {0, -1, 0} in NWU.
        """

        return trl.rotateBy(EDN_TO_NWU)

    @staticmethod
    def rotationEDNToNWU(rot: Rotation3d) -> Rotation3d:
        """Convert a rotation delta from NWU to EDN. For example, if you have a rotation X,Y,Z {1, 0, 0}
        in NWU, this would be {0, 0, 1} in EDN.
        """

        return -EDN_TO_NWU + (rot + EDN_TO_NWU)

    @staticmethod
    def tVecToTranslation(tvecInput: np.ndarray) -> Translation3d:
        """Returns a new 3d translation from this :class:`.Mat`. The opencv tvec is a vector with three
        elements representing {x, y, z} in the EDN coordinate system.

        :param tvecInput: The tvec to create a Translation3d from
        """

        return OpenCVHelp.translationEDNToNWU(Translation3d(tvecInput))

    @staticmethod
    def rVecToRotation(rvecInput: np.ndarray) -> Rotation3d:
        """Returns a 3d rotation from this :class:`.Mat`. The opencv rvec Mat is a vector with three
        elements representing the axis scaled by the angle in the EDN coordinate system. (angle = norm,
        and axis = rvec / norm)

        :param rvecInput: The rvec to create a Rotation3d from
        """

        return OpenCVHelp.rotationEDNToNWU(Rotation3d(rvecInput))

    @staticmethod
    def solvePNP_Square(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        modelTrls: list[Translation3d],
        imagePoints: np.ndarray,
    ) -> PnpResult | None:
        """Finds the transformation(s) that map the camera's pose to the target's pose. The camera's pose
        relative to the target is determined by the supplied 3d points of the target's model and their
        associated 2d points imaged by the camera. The supplied model translations must be relative to
        the target's pose.

        For planar targets, there may be an alternate solution which is plausible given the 2d image
        points. This has an associated "ambiguity" which describes the ratio of reprojection error
        between the "best" and "alternate" solution.

        This method is intended for use with individual AprilTags, and will not work unless 4 points
        are provided.

        :param cameraMatrix: The camera intrinsics matrix in standard opencv form
        :param distCoeffs:   The camera distortion matrix in standard opencv form
        :param modelTrls:    The translations of the object corners. These should have the object pose as
                             their origin. These must come in a specific, pose-relative order (in NWU):

                             - Point 0: [0, -squareLength / 2, squareLength / 2]
                             - Point 1: [0, squareLength / 2, squareLength / 2]
                             - Point 2: [0, squareLength / 2, -squareLength / 2]
                             - Point 3: [0, -squareLength / 2, -squareLength / 2]
        :param imagePoints:  The projection of these 3d object points into the 2d camera image. The order
                             should match the given object point translations.

        :returns: The resulting transformation that maps the camera pose to the target pose and the
                  ambiguity if an alternate solution is available.
        """
        modelTrls = OpenCVHelp.reorderCircular(modelTrls, True, -1)
        imagePoints = np.array(OpenCVHelp.reorderCircular(imagePoints, True, -1))
        objectMat = np.array(OpenCVHelp.translationToTVec(modelTrls))

        alt: Transform3d | None = None
        reprojectionError: cv.typing.MatLike | None = None
        best: Transform3d = Transform3d()

        for tries in range(2):
            # calc rvecs/tvecs and associated reprojection error from image points
            retval, rvecs, tvecs, reprojectionError = cv.solvePnPGeneric(
                objectMat,
                imagePoints,
                cameraMatrix,
                distCoeffs,
                flags=cv.SOLVEPNP_IPPE_SQUARE,
            )

            # convert to wpilib coordinates
            best = Transform3d(
                OpenCVHelp.tVecToTranslation(tvecs[0]),
                OpenCVHelp.rVecToRotation(rvecs[0]),
            )
            if len(tvecs) > 1:
                alt = Transform3d(
                    OpenCVHelp.tVecToTranslation(tvecs[1]),
                    OpenCVHelp.rVecToRotation(rvecs[1]),
                )

            # check if we got a NaN result
            if reprojectionError is not None and not math.isnan(
                reprojectionError[0, 0]
            ):
                break
            else:
                pt = imagePoints[0]
                pt[0, 0] -= 0.001
                pt[0, 1] -= 0.001
                imagePoints[0] = pt

        # solvePnP failed
        if reprojectionError is None or math.isnan(reprojectionError[0, 0]):
            print("SolvePNP_Square failed!")
            return None

        if alt:
            return PnpResult(
                best=best,
                bestReprojErr=reprojectionError[0, 0],
                alt=alt,
                altReprojErr=reprojectionError[1, 0],
                ambiguity=reprojectionError[0, 0] / reprojectionError[1, 0],
            )
        else:
            # We have no alternative so set it to best as well
            return PnpResult(
                best=best,
                bestReprojErr=reprojectionError[0],
                alt=best,
                altReprojErr=reprojectionError[0],
            )

    @staticmethod
    def solvePNP_SQPNP(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        modelTrls: list[Translation3d],
        imagePoints: np.ndarray,
    ) -> PnpResult | None:
        """Finds the transformation that maps the camera's pose to the origin of the supplied object. An
        "object" is simply a set of known 3d translations that correspond to the given 2d points. If,
        for example, the object translations are given relative to close-right corner of the blue
        alliance(the default origin), a camera-to-origin transformation is returned. If the
        translations are relative to a target's pose, a camera-to-target transformation is returned.

        There must be at least 3 points to use this method. This does not return an alternate
        solution-- if you are intending to use solvePNP on a single AprilTag, see {@link
        #solvePNP_SQUARE} instead.

        :param cameraMatrix: The camera intrinsics matrix in standard opencv form
        :param distCoeffs:   The camera distortion matrix in standard opencv form
        :param objectTrls:   The translations of the object corners, relative to the field.
        :param imagePoints:  The projection of these 3d object points into the 2d camera image. The order
                             should match the given object point translations.

        :returns: The resulting transformation that maps the camera pose to the target pose. If the 3d
                  model points are supplied relative to the origin, this transformation brings the camera to
                  the origin.
        """

        objectMat = np.array(OpenCVHelp.translationToTVec(modelTrls))

        retval, rvecs, tvecs, reprojectionError = cv.solvePnPGeneric(
            objectMat, imagePoints, cameraMatrix, distCoeffs, flags=cv.SOLVEPNP_SQPNP
        )

        error = reprojectionError[0, 0]
        best = Transform3d(
            OpenCVHelp.tVecToTranslation(tvecs[0]), OpenCVHelp.rVecToRotation(rvecs[0])
        )

        if math.isnan(error):
            return None

        # We have no alternative so set it to best as well
        result = PnpResult(best=best, bestReprojErr=error, alt=best, altReprojErr=error)
        return result
