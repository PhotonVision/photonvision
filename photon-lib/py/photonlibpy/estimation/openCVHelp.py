from . import RotTrlTransform3d
from ..targeting import PnpResult, TargetCorner

from wpimath.geometry import Rotation3d, Transform3d, Translation3d

import cv2 as cv
import numpy as np
import math

from typing import Any, Tuple


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
        return np.array(retVal)

    @staticmethod
    def rotationToRVec(rotation: Rotation3d) -> np.ndarray:
        retVal: list[np.ndarray] = []
        rot = OpenCVHelp.rotationNWUtoEDN(rotation)
        rotVec = rot.getQuaternion().toRotationVector()
        retVal.append(rotVec)
        return np.array(retVal)

    @staticmethod
    def avgPoint(points: list[Tuple[float, float]]) -> Tuple[float, float]:
        x = 0.0
        y = 0.0
        for p in points:
            x += p[0]
            y += p[1]
        return (x / len(points), y / len(points))

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
    def reorderCircular(elements: list[Any], backwards: bool, shiftStart: int) -> list[Any]:
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
        return OpenCVHelp.translationEDNToNWU(
            Translation3d(tvecInput[0], tvecInput[1], tvecInput[2])
        )

    @staticmethod
    def rVecToRotation(rvecInput: np.ndarray) -> Rotation3d:
        return OpenCVHelp.rotationEDNToNWU(
            Rotation3d(rvecInput[0], rvecInput[1], rvecInput[2])
        )

    @staticmethod
    def solvePNP_SQPNP(
        cameraMatrix: np.ndarray,
        distCoeffs: np.ndarray,
        modelTrls: list[Translation3d],
        imagePoints: np.ndarray,
    ) -> PnpResult | None:
        modelTrls = OpenCVHelp.reorderCircular(modelTrls, True, -1)
        imagePoints = np.array(OpenCVHelp.reorderCircular(imagePoints, True, -1))
        objectMat = np.array(OpenCVHelp.translationToTVec(modelTrls))

        retval, rvecs, tvecs, reprojectionError = cv.solvePnPGeneric(
            objectMat, imagePoints, cameraMatrix, distCoeffs, flags=cv.SOLVEPNP_SQPNP
        )

        error = reprojectionError[0, 0]
        best = Transform3d(
            OpenCVHelp.tVecToTranslation(tvecs[0]), OpenCVHelp.rVecToRotation(rvecs[0])
        )

        if math.isnan(error):
            print("SolvePNP_Square failed!")
            return None

        # We have no alternative so set it to best as well
        result = PnpResult(
            best=best, bestReprojError=error, alt=best, altReprojError=error
        )
        return result
