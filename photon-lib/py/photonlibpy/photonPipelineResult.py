from dataclasses import dataclass, field

from photonlibpy.multiTargetPNPResult import MultiTargetPNPResult
from photonlibpy.packet import Packet
from photonlibpy.photonTrackedTarget import PhotonTrackedTarget


@dataclass
class PhotonPipelineResult:
    # Image capture and NT publish timestamp, in microseconds and in the coprocessor timebase. As
    # reported by WPIUtilJNI::now.
    captureTimestampMicros: int = -1
    publishTimestampMicros: int = -1

    # Mirror of the heartbeat entry -- monotonically increasing
    sequenceID: int = -1

    # Since we don't trust NT time sync, keep track of when we got this packet into robot code
    ntRecieveTimestampMicros: int = -1

    targets: list[PhotonTrackedTarget] = field(default_factory=list)
    multiTagResult: MultiTargetPNPResult = field(default_factory=MultiTargetPNPResult)

    def populateFromPacket(self, packet: Packet) -> Packet:
        self.targets = []

        self.sequenceID = packet.decodei64()
        self.captureTimestampMicros = packet.decodei64()
        self.publishTimestampMicros = packet.decodei64()

        targetCount = packet.decode8()

        for _ in range(targetCount):
            target = PhotonTrackedTarget()
            target.createFromPacket(packet)
            self.targets.append(target)

        self.multiTagResult = MultiTargetPNPResult()
        self.multiTagResult.createFromPacket(packet)

        return packet

    def getLatencyMillis(self) -> float:
        return (self.publishTimestampMicros - self.captureTimestampMicros) / 1e3

    def getTimestampSeconds(self) -> float:
        """
        Returns the estimated time the frame was taken, in the recieved system's time base. This is
        calculated as (NT recieve time (robot base) - (publish timestamp, coproc timebase - capture
        timestamp, coproc timebase))
        """
        # TODO - we don't trust NT4 to correctly latency-compensate ntRecieveTimestampMicros
        return (
            self.ntRecieveTimestampMicros
            - (self.publishTimestampMicros - self.captureTimestampMicros)
        ) / 1e6

    def getTargets(self) -> list[PhotonTrackedTarget]:
        return self.targets

    def hasTargets(self) -> bool:
        return len(self.targets) > 0
