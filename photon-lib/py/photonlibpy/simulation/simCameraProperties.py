import logging
import math
import typing

import cv2 as cv
import numpy as np
from wpimath.geometry import Rotation2d, Rotation3d, Translation3d
from wpimath.units import hertz, seconds

from ..estimation import RotTrlTransform3d


class SimCameraProperties:
    """Calibration and performance values for this camera.

    The resolution will affect the accuracy of projected(3d to 2d) target corners and similarly
    the severity of image noise on estimation(2d to 3d).

    The camera intrinsics and distortion coefficients describe the results of calibration, and how
    to map between 3d field points and 2d image points.

    The performance values (framerate/exposure time, latency) determine how often results should
    be updated and with how much latency in simulation. High exposure time causes motion blur which
    can inhibit target detection while moving. Note that latency estimation does not account for
    network latency and the latency reported will always be perfect.
    """

    def __init__(self):
        """Default constructor which is the same as {@link #PERFECT_90DEG}"""
        self.resWidth: int = -1
        self.resHeight: int = -1
        self.camIntrinsics: np.ndarray = np.zeros((3, 3))  # [3,3]
        self.distCoeffs: np.ndarray = np.zeros((8, 1))  # [8,1]
        self.avgErrorPx: float = 0.0
        self.errorStdDevPx: float = 0.0
        self.frameSpeed: seconds = 0.0
        self.exposureTime: seconds = 0.0
        self.avgLatency: seconds = 0.0
        self.latencyStdDev: seconds = 0.0
        self.viewplanes: list[np.ndarray] = []  # [3,1]

        self.setCalibrationFromFOV(960, 720, fovDiag=Rotation2d(math.radians(90.0)))

    def setCalibrationFromFOV(
        self, width: int, height: int, fovDiag: Rotation2d
    ) -> None:
        if fovDiag.degrees() < 1.0 or fovDiag.degrees() > 179.0:
            fovDiag = Rotation2d.fromDegrees(max(min(fovDiag.degrees(), 179.0), 1.0))
            logging.error("Requested invalid FOV! Clamping between (1, 179) degrees...")

        resDiag = math.sqrt(width * width + height * height)
        diagRatio = math.tan(fovDiag.radians() / 2.0)
        fovWidth = Rotation2d(math.atan((diagRatio * (width / resDiag)) * 2))
        fovHeight = Rotation2d(math.atan(diagRatio * (height / resDiag)) * 2)

        # assume no distortion
        newDistCoeffs = np.zeros((8, 1))

        # assume centered principal point (pixels)
        cx = width / 2.0 - 0.5
        cy = height / 2.0 - 0.5

        # use given fov to determine focal point (pixels)
        fx = cx / math.tan(fovWidth.radians() / 2.0)
        fy = cy / math.tan(fovHeight.radians() / 2.0)

        # create camera intrinsics matrix
        newCamIntrinsics = np.array([[fx, 0.0, cx], [0.0, fy, cy], [0.0, 0.0, 1.0]])

        self.setCalibrationFromIntrinsics(
            width, height, newCamIntrinsics, newDistCoeffs
        )

    def setCalibrationFromIntrinsics(
        self,
        width: int,
        height: int,
        newCamIntrinsics: np.ndarray,
        newDistCoeffs: np.ndarray,
    ) -> None:

        self.resWidth = width
        self.resHeight = height
        self.camIntrinsics = newCamIntrinsics
        self.distCoeffs = newDistCoeffs

        # left, right, up, and down view planes
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
        """
        :param fps: The average frames per second the camera should process at. :strong:`Exposure time limits
                    FPS if set!`
        """

        self.frameSpeed = max(1.0 / fps, self.exposureTime)

    def setExposureTime(self, newExposureTime: seconds):
        """
        :param newExposureTime: The amount of time the "shutter" is open for one frame. Affects motion
                               blur. **Frame speed(from FPS) is limited to this!**
        """

        self.exposureTime = newExposureTime
        self.frameSpeed = max(self.frameSpeed, self.exposureTime)

    def setAvgLatency(self, newAvgLatency: seconds):
        """
        :param newAvgLatency: The average latency (from image capture to data published) in milliseconds
                             a frame should have
        """
        self.vgLatency = newAvgLatency

    def setLatencyStdDev(self, newLatencyStdDev: seconds):
        """
        :param latencyStdDevMs: The standard deviation in milliseconds of the latency
        """
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

    def getContourAreaPercent(self, points: np.ndarray) -> float:
        """The percentage(0 - 100) of this camera's resolution the contour takes up in pixels of the
        image.

        :param points: Points of the contour
        """

        return cv.contourArea(cv.convexHull(points)) / self.getResArea() * 100.0

    def getPixelYaw(self, pixelX: float) -> Rotation2d:
        """The yaw from the principal point of this camera to the pixel x value. Positive values left."""
        fx = self.camIntrinsics[0, 0]
        # account for principal point not being centered
        cx = self.camIntrinsics[0, 2]
        xOffset = cx - pixelX
        return Rotation2d(fx, xOffset)

    def getPixelPitch(self, pixelY: float) -> Rotation2d:
        """The pitch from the principal point of this camera to the pixel y value. Pitch is positive down.

        Note that this angle is naively computed and may be incorrect. See {@link
        #getCorrectedPixelRot(Point)}.
        """

        fy = self.camIntrinsics[1, 1]
        # account for principal point not being centered
        cy = self.camIntrinsics[1, 2]
        yOffset = cy - pixelY
        return Rotation2d(fy, -yOffset)

    def getPixelRot(self, point: cv.typing.Point2f) -> Rotation3d:
        """Finds the yaw and pitch to the given image point. Yaw is positive left, and pitch is positive
        down.

        Note that pitch is naively computed and may be incorrect. See {@link
        #getCorrectedPixelRot(Point)}.
        """

        return Rotation3d(
            0.0,
            self.getPixelPitch(point[1]).radians(),
            self.getPixelYaw(point[0]).radians(),
        )

    def getCorrectedPixelRot(self, point: cv.typing.Point2f) -> Rotation3d:
        """Gives the yaw and pitch of the line intersecting the camera lens and the given pixel
        coordinates on the sensor. Yaw is positive left, and pitch positive down.

        The pitch traditionally calculated from pixel offsets do not correctly account for non-zero
        values of yaw because of perspective distortion (not to be confused with lens distortion)-- for
        example, the pitch angle is naively calculated as:

        <pre>pitch = arctan(pixel y offset / focal length y)</pre>

        However, using focal length as a side of the associated right triangle is not correct when the
        pixel x value is not 0, because the distance from this pixel (projected on the x-axis) to the
        camera lens increases. Projecting a line back out of the camera with these naive angles will
        not intersect the 3d point that was originally projected into this 2d pixel. Instead, this
        length should be:

        <pre>focal length y ⟶ (focal length y / cos(arctan(pixel x offset / focal length x)))</pre>

        :returns: Rotation3d with yaw and pitch of the line projected out of the camera from the given
                  pixel (roll is zero).
        """

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
        # sum of FOV left and right principal point
        left = self.getPixelYaw(0)
        right = self.getPixelYaw(self.resWidth)
        return left - right

    def getVertFOV(self) -> Rotation2d:
        # sum of FOV above and below principal point
        above = self.getPixelPitch(0)
        below = self.getPixelPitch(self.resHeight)
        return below - above

    def getDiagFOV(self) -> Rotation2d:
        return Rotation2d(
            math.hypot(self.getHorizFOV().radians(), self.getVertFOV().radians())
        )

    def getVisibleLine(
        self, camRt: RotTrlTransform3d, a: Translation3d, b: Translation3d
    ) -> typing.Tuple[float | None, float | None]:
        """Determines where the line segment defined by the two given translations intersects the camera's
        frustum/field-of-vision, if at all.

        The line is parametrized so any of its points <code>p = t * (b - a) + a</code>. This method
        returns these values of t, minimum first, defining the region of the line segment which is
        visible in the frustum. If both ends of the line segment are visible, this simply returns {0,
        1}. If, for example, point b is visible while a is not, and half of the line segment is inside
        the camera frustum, {0.5, 1} would be returned.

        :param camRt: The change in basis from world coordinates to camera coordinates. See {@link
                      RotTrlTransform3d#makeRelativeTo(Pose3d)}.
        :param a:     The initial translation of the line
        :param b:     The final translation of the line

        :returns: A Pair of Doubles. The values may be null:

                  - {Double, Double} : Two parametrized values(t), minimum first, representing which
                  segment of the line is visible in the camera frustum.
                  - {Double, null} : One value(t) representing a single intersection point. For example,
                  the line only intersects the intersection of two adjacent viewplanes.
                  - {null, null} : No values. The line segment is not visible in the camera frustum.
        """

        # translations relative to the camera
        relA = camRt.applyTranslation(a)
        relB = camRt.applyTranslation(b)

        # check if both ends are behind camera
        if relA.X() <= 0.0 and relB.X() <= 0.0:
            return (None, None)

        av = np.array([relA.X(), relA.Y(), relA.Z()])
        bv = np.array([relB.X(), relB.Y(), relB.Z()])
        abv = bv - av

        aVisible = True
        bVisible = True

        # check if the ends of the line segment are visible
        for normal in self.viewplanes:
            aVisibility = av.dot(normal)
            if aVisibility < 0:
                aVisible = False

            bVisibility = bv.dot(normal)
            if bVisibility < 0:
                bVisible = False
            # both ends are outside at least one of the same viewplane
            if aVisibility <= 0 and bVisibility <= 0:
                return (None, None)

        # both ends are inside frustum
        if aVisible and bVisible:
            return (0.0, 1.0)

        # parametrized (t=0 at a, t=1 at b) intersections with viewplanes
        intersections = [float("nan"), float("nan"), float("nan"), float("nan")]

        # Optionally 3x1 vector
        ipts: typing.List[np.ndarray | None] = [None, None, None, None]

        # find intersections
        for i, normal in enumerate(self.viewplanes):

            # // we want to know the value of t when the line intercepts this plane
            # // parametrized: v = t * ab + a, where v lies on the plane
            # // we can find the projection of a onto the plane normal
            # // a_projn = normal.times(av.dot(normal) / normal.dot(normal));
            a_projn = (av.dot(normal) / normal.dot(normal)) * normal

            # // this projection lets us determine the scalar multiple t of ab where
            # // (t * ab + a) is a vector which lies on the plane
            if abs(abv.dot(normal)) < 1.0e-5:
                continue
            intersections[i] = a_projn.dot(a_projn) / -(abv.dot(a_projn))

            # // vector a to the viewplane
            apv = intersections[i] * abv
            # av + apv = intersection point
            intersectpt = av + apv
            ipts[i] = intersectpt

            # // discard intersections outside the camera frustum
            for j in range(1, len(self.viewplanes)):
                if j == 0:
                    continue
                oi = (i + j) % len(self.viewplanes)
                onormal = self.viewplanes[oi]
                # if the dot of the intersection point with any plane normal is negative, it is outside
                if intersectpt.dot(onormal) < 0:
                    intersections[i] = float("nan")
                    ipts[i] = None
                    break

            # // discard duplicate intersections
            if ipts[i] is None:
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

        # determine visible segment (minimum and maximum t)
        inter1 = float("nan")
        inter2 = float("nan")
        for inter in intersections:
            if not math.isnan(inter):
                if math.isnan(inter1):
                    inter1 = inter
                else:
                    inter2 = inter

        # // two viewplane intersections
        if not math.isnan(inter2):
            max_ = max(inter1, inter2)
            min_ = min(inter1, inter2)
            if aVisible:
                min_ = 0
            if bVisible:
                max_ = 1
            return (min_, max_)
        # // one viewplane intersection
        elif not math.isnan(inter1):
            if aVisible:
                return (0, inter1)
            if bVisible:
                return (inter1, 1)
            return (inter1, None)
        # no intersections
        else:
            return (None, None)

    def estPixelNoise(self, points: np.ndarray) -> np.ndarray:
        """Returns these points after applying this camera's estimated noise."""
        assert points.shape[1] == 1, points.shape
        assert points.shape[2] == 2, points.shape
        if self.avgErrorPx == 0 and self.errorStdDevPx == 0:
            return points

        noisyPts: list[list] = []
        for p in points:
            # // error pixels in random direction
            error = np.random.normal(self.avgErrorPx, self.errorStdDevPx, 1)[0]
            errorAngle = np.random.uniform(-math.pi, math.pi)
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
        """
        :returns: Noisy estimation of a frame's processing latency
        """

        return max(
            float(np.random.normal(self.avgLatency, self.latencyStdDev, 1)[0]),
            0.0,
        )

    def estSecUntilNextFrame(self) -> seconds:
        """
        :returns: Estimate how long until the next frame should be processed in milliseconds
        """
        # // exceptional processing latency blocks the next frame
        return self.frameSpeed + max(0.0, self.estLatency() - self.frameSpeed)

    @classmethod
    def PERFECT_90DEG(cls) -> typing.Self:
        """960x720 resolution, 90 degree FOV, "perfect" lagless camera"""
        return cls()

    @classmethod
    def PI4_LIFECAM_320_240(cls) -> typing.Self:
        prop = cls()
        prop.setCalibrationFromIntrinsics(
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
        prop.setCalibrationFromIntrinsics(
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
        prop.setCalibrationFromIntrinsics(
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
        prop.setCalibrationFromIntrinsics(
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
        prop.setCalibrationFromIntrinsics(
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

    @classmethod
    def OV9281_640_480(cls) -> typing.Self:
        prop = cls()
        prop.setCalibrationFromIntrinsics(
            640,
            480,
            newCamIntrinsics=np.array(
                [
                    [627.1573807284262, 0, 307.79423851611824],
                    [0, 626.6621595938243, 219.02625533911998],
                    [0, 0, 1],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.054834081023049625,
                        -0.15994111706817074,
                        -0.0017587106009926158,
                        -0.0014671022483263552,
                        0.049742166267499596,
                        0,
                        0,
                        0,
                    ],
                ]
            ),
        )
        prop.setCalibError(0.25, 0.05)
        prop.setFPS(30.0)
        prop.setAvgLatency(60.0e-3)
        prop.setLatencyStdDev(20.0e-3)
        return prop

    @classmethod
    def OV9281_800_600(cls) -> typing.Self:
        prop = cls()
        prop.setCalibrationFromIntrinsics(
            800,
            600,
            newCamIntrinsics=np.array(
                [
                    [783.9467259105329, 0, 384.7427981451478],
                    [0, 783.3276994922804, 273.7828191739],
                    [0, 0, 1],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.054834081023049625,
                        -0.15994111706817074,
                        -0.0017587106009926158,
                        -0.0014671022483263552,
                        0.049742166267499596,
                        0,
                        0,
                        0,
                    ],
                ]
            ),
        )
        prop.setCalibError(0.25, 0.05)
        prop.setFPS(25.0)
        prop.setAvgLatency(60.0e-3)
        prop.setLatencyStdDev(20.0e-3)
        return prop

    @classmethod
    def OV9281_1280_720(cls) -> typing.Self:
        prop = cls()
        prop.setCalibrationFromIntrinsics(
            1280,
            720,
            newCamIntrinsics=np.array(
                [
                    [940.7360710926395, 0, 615.5884770322365],
                    [0, 939.9932393907364, 328.53938300868],
                    [0, 0, 1],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.054834081023049625,
                        -0.15994111706817074,
                        -0.0017587106009926158,
                        -0.0014671022483263552,
                        0.049742166267499596,
                        0,
                        0,
                        0,
                    ],
                ]
            ),
        )
        prop.setCalibError(0.25, 0.05)
        prop.setFPS(15.0)
        prop.setAvgLatency(60.0e-3)
        prop.setLatencyStdDev(20.0e-3)
        return prop

    @classmethod
    def OV9281_1920_1080(cls) -> typing.Self:
        prop = cls()
        prop.setCalibrationFromIntrinsics(
            1920,
            1080,
            newCamIntrinsics=np.array(
                [
                    [1411.1041066389591, 0, 923.3827155483548],
                    [0, 1409.9898590861046, 492.80907451301994],
                    [0, 0, 1],
                ]
            ),
            newDistCoeffs=np.array(
                [
                    [
                        0.054834081023049625,
                        -0.15994111706817074,
                        -0.0017587106009926158,
                        -0.0014671022483263552,
                        0.049742166267499596,
                        0,
                        0,
                        0,
                    ],
                ]
            ),
        )
        prop.setCalibError(0.25, 0.05)
        prop.setFPS(10.0)
        prop.setAvgLatency(60.0e-3)
        prop.setLatencyStdDev(20.0e-3)
        return prop
