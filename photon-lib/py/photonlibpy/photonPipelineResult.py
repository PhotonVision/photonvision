from dataclasses import dataclass, field

from photonlibpy.multiTargetPNPResult import MultiTargetPNPResult
from photonlibpy.packet import Packet
from photonlibpy.photonTrackedTarget import PhotonTrackedTarget


@dataclass
class PhotonPipelineResult:
    latencyMillis: float = -1.0
    timestampSec: float = -1.0
    targets: list[PhotonTrackedTarget] = field(default_factory=list)
    multiTagResult: MultiTargetPNPResult = field(default_factory=MultiTargetPNPResult)

    def populateFromPacket(self, packet: Packet) -> Packet:
        self.targets = []
        self.latencyMillis = packet.decodeDouble()
        targetCount = packet.decode8()

        for _ in range(targetCount):
            target = PhotonTrackedTarget()
            target.createFromPacket(packet)
            self.targets.append(target)

        self.multiTagResult = MultiTargetPNPResult()
        self.multiTagResult.createFromPacket(packet)

        return packet

    def setTimestampSeconds(self, timestampSec: float) -> None:
        self.timestampSec = timestampSec

    def getLatencyMillis(self) -> float:
        return self.latencyMillis

    def getTimestamp(self) -> float:
        return self.timestampSec

    def getTargets(self) -> list[PhotonTrackedTarget]:
        return self.targets

    def hasTargets(self) -> bool:
        return len(self.targets) > 0
