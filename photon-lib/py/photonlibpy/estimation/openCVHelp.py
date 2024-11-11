import math
from typing import Any, Tuple

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
        return trl.rotateBy(EDN_TO_NWU)

    @staticmethod
    def rotationEDNToNWU(rot: Rotation3d) -> Rotation3d:
        return -EDN_TO_NWU + (rot + EDN_TO_NWU)

    @staticmethod
    def tVecToTranslation(tvecInput: np.ndarray) -> Translation3d:
        return OpenCVHelp.translationEDNToNWU(Translation3d(tvecInput))

    @staticmethod
    def rVecToRotation(rvecInput: np.ndarray) -> Rotation3d:
        return OpenCVHelp.rotationEDNToNWU(Rotation3d(rvecInput))

    @staticmethod
    def solvePNP_Square(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        modelTrls: list[Translation3d],
        imagePoints: np.ndarray,
    ) -> PnpResult | None:
        modelTrls = OpenCVHelp.reorderCircular(modelTrls, True, -1)
        imagePoints = np.array(OpenCVHelp.reorderCircular(imagePoints, True, -1))
        objectMat = np.array(OpenCVHelp.translationToTVec(modelTrls))

        alt: Transform3d | None = None
        reprojectionError : cv.typing.MatLike | None = None
        best : Transform3d = Transform3d()
        alt: Transform3d | None = None

        for tries in range(2):
            retval, rvecs, tvecs, reprojectionError = cv.solvePnPGeneric(
                objectMat,
                imagePoints,
                cameraMatrix,
                distCoeffs,
                flags=cv.SOLVEPNP_IPPE_SQUARE,
            )

            best = Transform3d(
                OpenCVHelp.tVecToTranslation(tvecs[0]),
                OpenCVHelp.rVecToRotation(rvecs[0]),
            )
            if len(tvecs) > 1:
                alt = Transform3d(
                    OpenCVHelp.tVecToTranslation(tvecs[1]),
                    OpenCVHelp.rVecToRotation(rvecs[1]),
                )

            if reprojectionError is not None and not math.isnan(reprojectionError[0, 0]):
                break
            else:
                pt = imagePoints[0]
                pt[0, 0] -= 0.001
                pt[0, 1] -= 0.001
                imagePoints[0] = pt

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
