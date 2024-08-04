from dataclasses import dataclass, field
from typing import Optional

from .multiTargetPNPResult import MultiTargetPNPResult
from .photonTrackedTarget import PhotonTrackedTarget


@dataclass
class PhotonPipelineMetadata:
    # Image capture and NT publish timestamp, in microseconds and in the coprocessor timebase. As
    # reported by WPIUtilJNI::now.
    captureTimestampMicros: int = -1
    publishTimestampMicros: int = -1

    # Mirror of the heartbeat entry -- monotonically increasing
    sequenceID: int = -1

    photonStruct: "PhotonPipelineMetadataSerde" = None


@dataclass
class PhotonPipelineResult:
    # Since we don't trust NT time sync, keep track of when we got this packet into robot code
    ntRecieveTimestampMicros: int = -1

    targets: list[PhotonTrackedTarget] = field(default_factory=list)
    metadata: PhotonPipelineMetadata = field(default_factory=PhotonPipelineMetadata)
    multiTagResult: Optional[MultiTargetPNPResult] = None

    def getLatencyMillis(self) -> float:
        return (
            self.metadata.publishTimestampMicros - self.metadata.captureTimestampMicros
        ) / 1e3

    def getTimestampSeconds(self) -> float:
        """
        Returns the estimated time the frame was taken, in the recieved system's time base. This is
        calculated as (NT recieve time (robot base) - (publish timestamp, coproc timebase - capture
        timestamp, coproc timebase))
        """
        # TODO - we don't trust NT4 to correctly latency-compensate ntRecieveTimestampMicros
        return (
            self.ntRecieveTimestampMicros
            - (
                self.metadata.publishTimestampMicros
                - self.metadata.captureTimestampMicros
            )
        ) / 1e6

    def getTargets(self) -> list[PhotonTrackedTarget]:
        return self.targets

    def hasTargets(self) -> bool:
        return len(self.targets) > 0

    photonStruct: "PhotonPipelineResultSerde" = None
