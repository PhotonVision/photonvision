from .simCameraProperties import SimCameraProperties, PERFECT_90DEG
from .visionTargetSim import VisionTargetSim
from ..photonCamera import PhotonCamera
from ..targeting import PhotonPipelineResult

from wpimath.geometry import Pose3d
from wpimath.units import meters, seconds

import cscore as cs
import numpy as np

import typing

class PhotonCameraSim:
    def __init__(self, camera: PhotonCamera, props: SimCameraProperties | None = None, minTargetAreaPercent: float | None = None, maxSightRange: meters | None = None):
        self.videoSimRaw: cs.CvSource
        self.videoSimFrameRaw: np.ndarray
        self.videoSimRawEnabled: bool = True
        self.videoSimWireframeEnabled: bool = False
        self.videoSimWireframeResolution: float = 0.1
        self.videoSimProcessed: cs.CvSource
        self.videoSimFrameProcessed: np.ndarray
        self.videoSimProcEnabled: bool = True

        self.cam: PhotonCamera = camera
        self.props = props if props else PERFECT_90DEG()

        self.minTargetAreaPercent: float = 0.0
        self.maxSightRange: float = 1.e99

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
        raise "Not yet implemented"

    def canSeeCorner(self, points: list[tuple[float, float]]) -> bool:
        raise "Not yet implemented"
  
    def consumeNextEntryTime(self)-> float | None:
        raise "Not yet implemented"

    def setMinTargetAreaPercent(self, areaPercent: float) -> None:
        self.minTargetAreaPercent = areaPercent

    def setMinTargetAreaPixels(self, areaPx: float) -> None:
        self.minTargetAreaPercent = areaPx / self.prop.getResArea() * 100.

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
    
    def process(latency: seconds, cameraPose: Pose3d, targets: list[VisionTargetSim]) -> PhotonPipelineResult:
        raise "Not yet implemented"

    def submitProcessedFrame(self, result: PhotonPipelineResult, receiveTimestamp: float | None):
        raise "Not yet implemented"
