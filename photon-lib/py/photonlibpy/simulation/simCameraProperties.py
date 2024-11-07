from wpimath.geometry import (
    Rotation2d,
    Rotation3d,
    Translation3d,
    Transform3d,
)
from wpimath.units import seconds, hertz

import logging
import math
import numpy as np
import typing

import cv2 as cv


class SimCameraProperties:

    def __init__(self, path: str | None = None, width: int = 0, height: int = 0):
        self.resWidth: int = -1
        self.resHeight: int = -1
        self.camIntrinsics: np.ndarray = np.zeros((3, 3))  # [3,3]
        self.distCoeffs: np.ndarray = np.zeros((8, 1))  # [8,1]
        self.avgErrorPx: float
        self.errorStdDevPx: float
        self.frameSpeed: seconds = 0.0
        self.exposureTime: seconds = 0.0
        self.avgLatency: seconds = 0.0
        self.latencyStdDev: seconds = 0.0
        self.viewplanes: list[np.ndarray]  # [3,1]

        if path is None:
            self.setCalibration(960, 720, fovDiag=Rotation2d(math.radians(90.0)))
        else:
            raise Exception("not yet implemented")

    def setCalibration(
        self,
        width: int,
        height: int,
        *,
        fovDiag: Rotation2d | None = None,
        newCamIntrinsics: np.ndarray | None = None,
        newDistCoeffs: np.ndarray | None = None
    ):
        # Should be an inverted XOR on the args to differentiate between the signatures

        has_fov_args = fovDiag is not None
        has_matrix_args = newCamIntrinsics is not None and newDistCoeffs is not None

        if (has_fov_args and has_matrix_args) or (
            not has_matrix_args and not has_fov_args
        ):
            raise Exception("not a correct function sig")

        if has_fov_args:
            if fovDiag.degrees() < 1.0 or fovDiag.degrees() > 179.0:
                fovDiag = Rotation2d.fromDegrees(
                    max(min(fovDiag.degrees(), 179.0), 1.0)
                )
                logging.error(
                    "Requested invalid FOV! Clamping between (1, 179) degrees..."
                )

            resDiag = math.sqrt(width * width + height * height)
            diagRatio = math.tan(fovDiag.radians() / 2.0)
            fovWidth = Rotation2d(math.atan((diagRatio * (width / resDiag)) * 2))
            fovHeight = Rotation2d(math.atan(diagRatio * (height / resDiag)) * 2)

            newDistCoeffs = np.zeros((8, 1))

            cx = width / 2.0 - 0.5
            cy = height / 2.0 - 0.5

            fx = cx / math.tan(fovWidth.radians() / 2.0)
            fy = cy / math.tan(fovHeight.radians() / 2.0)

            newCamIntrinsics = np.array([[fx, 0.0, cx], [0.0, fy, cy], [0.0, 0.0, 1.0]])

        # really convince python we are doing the right thing
        assert newCamIntrinsics is not None
        assert newDistCoeffs is not None

        self.resWidth = width
        self.resHeight = height
        self.camIntrinsics = newCamIntrinsics
        self.distCoeffs = newDistCoeffs

        p = [
            Translation3d(
                1.0,
                Rotation3d(
                    0.0,
                    0.0,
                    (self.getPixelYaw(0) + Rotation2d(math.pi / 2.0)).radians(),
                ),
            ),
            Translation3d(
                1.0,
                Rotation3d(
                    0.0,
                    0.0,
                    (self.getPixelYaw(width) + Rotation2d(math.pi / 2.0)).radians(),
                ),
            ),
            Translation3d(
                1.0,
                Rotation3d(
                    0.0,
                    0.0,
                    (self.getPixelPitch(0) + Rotation2d(math.pi / 2.0)).radians(),
                ),
            ),
            Translation3d(
                1.0,
                Rotation3d(
                    0.0,
                    0.0,
                    (self.getPixelPitch(height) + Rotation2d(math.pi / 2.0)).radians(),
                ),
            ),
        ]

        self.viewplanes = []

        for i in p:
            self.viewplanes.append(np.array([i.X(), i.Y(), i.Z()]))

    def setCalibError(self, newAvgErrorPx: float, newErrorStdDevPx: float):
        self.avgErrorPx = newAvgErrorPx
        self.errorStdDevPx = newErrorStdDevPx

    def setFPS(self, fps: hertz):
        self.frameSpeed = max(1.0 / fps, self.exposureTime)

    def setExposureTime(self, newExposureTime: seconds):
        self.exposureTime = newExposureTime
        self.frameSpeed = max(self.frameSpeed, self.exposureTime)

    def setAvgLatency(self, newAvgLatency: seconds):
        self.vgLatency = newAvgLatency

    def setLatencyStdDev(self, newLatencyStdDev: seconds):
        self.latencyStdDev = newLatencyStdDev

    def getResWidth(self) -> int:
        return self.resWidth

    def getResHeight(self) -> int:
        return self.resHeight

    def getResArea(self) -> int:
        return self.resWidth * self.resHeight

    def getAspectRatio(self) -> float:
        return 1.0 * self.resWidth / self.resHeight

    def getIntrinsics(self) -> np.ndarray:
        return self.camIntrinsics

    def getDistCoeffs(self) -> np.ndarray:
        return self.distCoeffs

    def getFPS(self) -> hertz:
        return 1.0 / self.frameSpeed

    def getFrameSpeed(self) -> seconds:
        return self.frameSpeed

    def getExposureTime(self) -> seconds:
        return self.exposureTime

    def getAverageLatency(self) -> seconds:
        return self.avgLatency

    def getLatencyStdDev(self) -> seconds:
        return self.latencyStdDev

    def getContourAreaPercent(self, points: list[typing.Tuple[float, float]]) -> float:
        return (
            cv.contourArea(cv.convexHull(np.array(points))) / self.getResArea() * 100.0
        )

    def getPixelYaw(self, pixelX: float) -> Rotation2d:
        fx = self.camIntrinsics[0, 0]
        cx = self.camIntrinsics[0, 2]
        xOffset = cx - pixelX
        return Rotation2d(fx, xOffset)

    def getPixelPitch(self, pixelY: float) -> Rotation2d:
        fy = self.camIntrinsics[1, 1]
        cy = self.camIntrinsics[1, 2]
        yOffset = cy - pixelY
        return Rotation2d(fy, -yOffset)

    def getPixelRot(self, point: typing.Tuple[int, int]) -> Rotation3d:
        return Rotation3d(
            0.0,
            self.getPixelPitch(point[0]).radians(),
            self.getPixelYaw(point[1]).radians(),
        )

    def getCorrectedPixelRot(self, point: typing.Tuple[float, float]) -> Rotation3d:
        fx = self.camIntrinsics[0, 0]
        cx = self.camIntrinsics[0, 2]
        xOffset = cx - point[0]

        fy = self.camIntrinsics[1, 1]
        cy = self.camIntrinsics[1, 2]
        yOffset = cy - point[1]

        yaw = Rotation2d(fx, xOffset)
        pitch = Rotation2d(fy / math.cos(math.atan(xOffset / fx)), -yOffset)
        return Rotation3d(0.0, pitch.radians(), yaw.radians())

    def getHorizFOV(self) -> Rotation2d:
        left = self.getPixelYaw(0)
        right = self.getPixelYaw(self.resWidth)
        return left - right

    def getVertFOV(self) -> Rotation2d:
        above = self.getPixelPitch(0)
        below = self.getPixelPitch(self.resHeight)
        return below - above

    def getDiagFOV(self) -> Rotation2d:
        return Rotation2d(
            math.hypot(self.getHorizFOV().radians(), self.getVertFOV().radians())
        )

    def getVisibleLine(
        self, camRt: Transform3d, a: Translation3d, b: Translation3d
    ) -> typing.Tuple[float | None, float | None]:
        # Original header has camRt: RotTrlTransform3d
        relA = camRt + Transform3d(a, Rotation3d())
        relB = camRt + Transform3d(b, Rotation3d())

        if relA.X() <= 0.0 and relB.X() <= 0.0:
            return (None, None)

        av = np.array([relA.X(), relA.Y(), relA.Z()])
        bv = np.array([relB.X(), relB.Y(), relB.Z()])
        abv = bv - av

        aVisible = True
        bVisible = True

        for normal in self.viewplanes:
            aVisibility = av.dot(normal)
            if aVisibility < 0:
                aVisible = False

            bVisibility = bv.dot(normal)
            if bVisibility < 0:
                bVisible = False
            if aVisibility <= 0 and bVisibility <= 0:
                return (None, None)

        if aVisible and bVisible:
            return (0.0, 1.0)

        intersections = [float("nan"), float("nan"), float("nan"), float("nan")]

        # Optionally 3x1 vector
        ipts: typing.List[np.ndarray | None] = [None, None, None, None]

        for i, normal in enumerate(self.viewplanes):
            a_projn = (av.dot(normal) / normal.dot(normal)) * normal

            if abs(abv.dot(normal)) < 1.0e-5:
                continue
            intersections[i] = a_projn.dot(a_projn) / -(abv.dot(a_projn))

            apv = intersections[i] * abv
            intersectpt = av + apv
            ipts[i] = intersectpt

            for j in range(1, len(self.viewplanes)):
                if j == 0:
                    continue
                oi = (i + j) % len(self.viewplanes)
                onormal = self.viewplanes[oi]
                if intersectpt.dot(onormal) < 0:
                    intersections[i] = float("nan")
                    ipts[i] = None
                    break

            if not ipts[i]:
                continue

            for j in range(i - 1, 0 - 1):
                oipt = ipts[j]
                if not oipt:
                    continue

                diff = oipt - intersectpt
                if abs(diff).max() < 1e-4:
                    intersections[i] = float("nan")
                    ipts[i] = None
                    break

        inter1 = float("nan")
        inter2 = float("nan")
        for inter in intersections:
            if not math.isnan(inter):
                if math.isnan(inter1):
                    inter1 = inter
                else:
                    inter2 = inter

        if not math.isnan(inter2):
            max_ = max(inter1, inter2)
            min_ = min(inter1, inter2)
            if aVisible:
                min_ = 0
            if bVisible:
                max_ = 1
            return (min_, max_)
        elif not math.isnan(inter1):
            if aVisible:
                return (0, inter1)
            if bVisible:
                return (inter1, 1)
            return (inter1, None)
        else:
            return (None, None)

    def estPixelNoise(self, points: np.ndarray) -> np.ndarray:
        assert points.shape[1] == 1, points.shape
        assert points.shape[2] == 2, points.shape
        if self.avgErrorPx == 0 and self.errorStdDevPx == 0:
            return points

        noisyPts: list[list] = []
        for p in points:
            error = (
                self.avgErrorPx + np.random.normal(0.0, 1.0, 1)[0] * self.errorStdDevPx
            )
            errorAngle = np.random.uniform(0.0, 1.0) * 2.0 * math.pi - math.pi
            noisyPts.append(
                [
                    [
                        float(p[0, 0] + error * math.cos(errorAngle)),
                        float(p[0, 1] + error * math.sin(errorAngle)),
                    ]
                ]
            )
        retval = np.array(noisyPts, dtype=np.float32)
        assert points.shape == retval.shape, retval
        return retval

    def estLatency(self) -> seconds:
        return max(
            float(self.avgLatency + np.random.normal(0.0, 1.0, 1) * self.latencyStdDev),
            0.0,
        )

    def estSecUntilNextFrame(self) -> seconds:
        return self.frameSpeed + max(0.0, self.estLatency() - self.frameSpeed)


    @classmethod
    def PERFECT_90DEG(cls) -> typing.Self:
        return cls()


    @classmethod
    def PI4_LIFECAM_320_240(cls) -> typing.Self:
        prop = cls()
        prop.setCalibration(
            320,
            240,
            newCamIntrinsics=np.array(
                [
                    [328.2733242048587, 0.0, 164.8190261141906],
                    [0.0, 318.0609794305216, 123.8633838438093],
                    [0.0, 0.0, 1.0],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.09957946553445934,
                        -0.9166265114485799,
                        0.0019519890627236526,
                        -0.0036071725380870333,
                        1.5627234622420942,
                        0,
                        0,
                        0,
                    ]
                ]
            ),
        )
        prop.setCalibError(0.21, 0.0124)
        prop.setFPS(30.0)
        prop.setAvgLatency(30.0e-3)
        prop.setLatencyStdDev(10.0e-3)
        return prop


    @classmethod
    def PI4_LIFECAM_640_480(cls) -> typing.Self:
        prop = cls()
        prop.setCalibration(
            640,
            480,
            newCamIntrinsics=np.array(
                [
                    [669.1428078983059, 0.0, 322.53377249329213],
                    [0.0, 646.9843137061716, 241.26567383784163],
                    [0.0, 0.0, 1.0],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.12788470750464645,
                        -1.2350335805796528,
                        0.0024990767286192732,
                        -0.0026958287600230705,
                        2.2951386729115537,
                        0,
                        0,
                        0,
                    ]
                ]
            ),
        )
        prop.setCalibError(0.26, 0.046)
        prop.setFPS(15.0)
        prop.setAvgLatency(65.0e-3)
        prop.setLatencyStdDev(15.0e-3)
        return prop


    @classmethod
    def LL2_640_480(cls) -> typing.Self:
        prop = cls()
        prop.setCalibration(
            640,
            480,
            newCamIntrinsics=np.array(
                [
                    [511.22843367007755, 0.0, 323.62049380211096],
                    [0.0, 514.5452336723849, 261.8827920543568],
                    [0.0, 0.0, 1.0],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.1917469998873756,
                        -0.5142936883324216,
                        0.012461562046896614,
                        0.0014084973492408186,
                        0.35160648971214437,
                        0,
                        0,
                        0,
                    ]
                ]
            ),
        )
        prop.setCalibError(0.25, 0.05)
        prop.setFPS(15.0)
        prop.setAvgLatency(35.0e-3)
        prop.setLatencyStdDev(8.0e-3)
        return prop


    @classmethod
    def LL2_960_720(cls) -> typing.Self:
        prop = cls()
        prop.setCalibration(
            960,
            720,
            newCamIntrinsics=np.array(
                [
                    [769.6873145148892, 0.0, 486.1096609458122],
                    [0.0, 773.8164483705323, 384.66071662358354],
                    [0.0, 0.0, 1.0],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.189462064814501,
                        -0.49903003669627627,
                        0.007468423590519429,
                        0.002496885298683693,
                        0.3443122090208624,
                        0,
                        0,
                        0,
                    ]
                ]
            ),
        )
        prop.setCalibError(0.35, 0.10)
        prop.setFPS(10.0)
        prop.setAvgLatency(50.0e-3)
        prop.setLatencyStdDev(15.0e-3)
        return prop


    @classmethod
    def LL2_1280_720(cls) -> typing.Self:
        prop = cls()
        prop.setCalibration(
            1280,
            720,
            newCamIntrinsics=np.array(
                [
                    [1011.3749416937393, 0.0, 645.4955139388737],
                    [0.0, 1008.5391755084075, 508.32877656020196],
                    [0.0, 0.0, 1.0],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.13730101577061535,
                        -0.2904345656989261,
                        8.32475714507539e-4,
                        -3.694397782014239e-4,
                        0.09487962227027584,
                        0,
                        0,
                        0,
                    ]
                ]
            ),
        )
        prop.setCalibError(0.37, 0.06)
        prop.setFPS(7.0)
        prop.setAvgLatency(60.0e-3)
        prop.setLatencyStdDev(20.0e-3)
        return prop
