from dataclasses import dataclass, field
from typing import TYPE_CHECKING, ClassVar

from wpimath.geometry import Transform3d

from ..packet import Packet

if TYPE_CHECKING:
    from .. import generated


@dataclass
class PnpResult:
    best: Transform3d = field(default_factory=Transform3d)
    alt: Transform3d = field(default_factory=Transform3d)
    ambiguity: float = 0.0
    bestReprojErr: float = 0.0
    altReprojErr: float = 0.0

    photonStruct: ClassVar["generated.PnpResultSerde"]


@dataclass
class MultiTargetPNPResult:
    _MAX_IDS = 32

    estimatedPose: PnpResult = field(default_factory=PnpResult)
    fiducialIDsUsed: list[int] = field(default_factory=list)

    def createFromPacket(self, packet: Packet) -> Packet:
        self.estimatedPose = PnpResult()
        self.estimatedPose.createFromPacket(packet)
        self.fiducialIDsUsed = []
        for _ in range(MultiTargetPNPResult._MAX_IDS):
            fidId = packet.decode16()
            if fidId >= 0:
                self.fiducialIDsUsed.append(fidId)
        return packet

    photonStruct: ClassVar["generated.MultiTargetPNPResultSerde"]
