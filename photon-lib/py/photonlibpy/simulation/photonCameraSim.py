from .simCameraProperties import SimCameraProperties, PERFECT_90DEG
from .visionTargetSim import VisionTargetSim
from ..photonCamera import PhotonCamera
from ..targeting import PhotonPipelineResult
from ..estimation.cameraTargetRelation import CameraTargetRelation

from wpimath.geometry import Pose3d
from wpimath.units import meters, seconds

import cscore as cs
import numpy as np

import typing


class PhotonCameraSim:
    def __init__(
        self,
        camera: PhotonCamera,
        props: SimCameraProperties | None = None,
        minTargetAreaPercent: float | None = None,
        maxSightRange: meters | None = None,
    ):
        self.videoSimRaw: cs.CvSource
        self.videoSimFrameRaw: np.ndarray
        self.videoSimRawEnabled: bool = True
        self.videoSimWireframeEnabled: bool = False
        self.videoSimWireframeResolution: float = 0.1
        self.videoSimProcessed: cs.CvSource
        self.videoSimFrameProcessed: np.ndarray
        self.videoSimProcEnabled: bool = True

        self.cam: PhotonCamera = camera
        self.prop = props if props else PERFECT_90DEG()

        self.minTargetAreaPercent: float = 0.0
        self.maxSightRange: float = 1.0e99

        if minTargetAreaPercent is not None and maxSightRange is not None:
            self.minTargetAreaPercent = minTargetAreaPercent
            self.maxSightRange = maxSightRange
        # TODO Initialise video stream stuff

    def getCamera(self) -> PhotonCamera:
        return self.cam

    def getMinTargetAreaPercent(self) -> float:
        return self.minTargetAreaPercent

    def getMinTargetAreaPixels(self) -> float:
        return self.minTargetAreaPercent / 100.0 * self.prop.getResArea()

    def getMaxSightRange(self) -> meters:
        return self.maxSightRange

    def getVideoSimRaw(self) -> cs.CvSource:
        return self.videoSimRaw

    def getVideoSimFrameRaw(self) -> np.ndarray:
        return self.videoSimFrameRaw

    def canSeeTargetPose(self, camPose: Pose3d, target: VisionTargetSim) -> bool:
        rel = CameraTargetRelation(camPose, target.getPose())
        return (
            abs(rel.camToTargYaw.degrees()) < self.prop.getHorizFOV().degrees() / 2.0
            and abs(rel.camToTargPitch.degrees())
            < self.prop.getVertFOV().degrees() / 2.0
            and not target.getModel().getIsPlanar()
            or abs(rel.targtoCamAngle.degrees()) < 90
            and rel.camToTarg.translation().norm() <= self.maxSightRange
        )

    def canSeeCorner(self, points: list[tuple[float, float]]) -> bool:
        for pt in points:
            if not (
                abs((max(pt[0], min(pt[0], self.prop.getResWidth()))) - pt[0]) < 1e-4
            ) or not (
                abs((max(pt[1], min(pt[1], self.prop.getResHeight()))) - pt[1]) < 1e-4
            ):
                return False

        return True

    def consumeNextEntryTime(self) -> float | None:
        now = wpilib.Timer.getFPGATimestamp() * 1e6
        timestamp = 0
        iter = 0
        while now >= self.nextNtEntryTime:
            timestamp = int(self.nextNtEntryTime)
            frameTime = int(self.prop.estSecUntilNextFrame() * 1e6)
            self.nextNtEntryTime += frameTime

            iter += 1
            if iter > 50:
                timestamp = now
                self.nextNtEntryTime = now + frameTime

        if timestamp != 0:
            return timestamp

        return None

    def setMinTargetAreaPercent(self, areaPercent: float) -> None:
        self.minTargetAreaPercent = areaPercent

    def setMinTargetAreaPixels(self, areaPx: float) -> None:
        self.minTargetAreaPercent = areaPx / self.prop.getResArea() * 100.0

    def setMaxSightRange(self, range: meters) -> None:
        self.maxSightRange = range

    def enableRawStream(self, enabled: bool) -> None:
        self.videoSimRawEnabled = enabled

    def enableDrawWireframe(self, enabled: bool) -> None:
        self.videoSimWireframeEnabled = enabled

    def setWireframeResolution(self, resolution: float) -> None:
        self.videoSimWireframeResolution = resolution

    def enableProcessedStream(self, enabled: bool) -> None:
        self.videoSimProcEnabled = enabled

    def process(self,
        latency: seconds, cameraPose: Pose3d, targets: list[VisionTargetSim]
    ) -> PhotonPipelineResult:
        raise Exception("Not yet implemented")

    def submitProcessedFrame(
        self, result: PhotonPipelineResult, receiveTimestamp: float | None
    ):
        raise Exception("Not yet implemented")
