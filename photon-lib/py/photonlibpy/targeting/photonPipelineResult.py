from dataclasses import dataclass, field
from typing import TYPE_CHECKING, ClassVar, Optional

from .multiTargetPNPResult import MultiTargetPNPResult
from .photonTrackedTarget import PhotonTrackedTarget

if TYPE_CHECKING:
    from ..generated.PhotonPipelineMetadataSerde import PhotonPipelineMetadataSerde
    from ..generated.PhotonPipelineResultSerde import PhotonPipelineResultSerde


@dataclass
class PhotonPipelineMetadata:
    # Image capture and NT publish timestamp, in microseconds and in the coprocessor timebase. As
    # reported by WPIUtilJNI::now.
    captureTimestampMicros: int = -1
    publishTimestampMicros: int = -1

    # Mirror of the heartbeat entry -- monotonically increasing
    sequenceID: int = -1

    timeSinceLastPong: int = -1

    photonStruct: ClassVar["PhotonPipelineMetadataSerde"]


@dataclass
class PhotonPipelineResult:
    targets: list[PhotonTrackedTarget] = field(default_factory=list)
    # Python users beware! We don't currently run a Time Sync Server, so these timestamps are in
    # an arbitrary timebase. This is not true in C++ or Java.
    metadata: PhotonPipelineMetadata = field(default_factory=PhotonPipelineMetadata)
    multitagResult: Optional[MultiTargetPNPResult] = None

    def getLatencyMillis(self) -> float:
        return (
            self.metadata.publishTimestampMicros - self.metadata.captureTimestampMicros
        ) / 1e3

    def getTimestampSeconds(self) -> float:
        """
        Returns the estimated time the frame was captured, in the same time base as
        ``wpilib.Timer.getFPGATimestamp()``.

        :returns: The timestamp in seconds.
        """
        return self.metadata.captureTimestampMicros / 1e6

    def getTargets(self) -> list[PhotonTrackedTarget]:
        return self.targets

    def hasTargets(self) -> bool:
        return len(self.targets) > 0

    def getBestTarget(self) -> Optional[PhotonTrackedTarget]:
        """
        Returns the best target in this pipeline result. If there are no targets, this method will
        return null. The best target is determined by the target sort mode in the PhotonVision UI.
        """
        if not self.hasTargets():
            return None
        return self.getTargets()[0]

    photonStruct: ClassVar["PhotonPipelineResultSerde"]
