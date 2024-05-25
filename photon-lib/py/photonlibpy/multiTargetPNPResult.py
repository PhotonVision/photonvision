from dataclasses import dataclass, field
from wpimath.geometry import Transform3d
from photonlibpy.packet import Packet


@dataclass
class PNPResult:
    _NUM_BYTES_IN_FLOAT = 8
    PACK_SIZE_BYTES = 1 + (_NUM_BYTES_IN_FLOAT * 7 * 2) + (_NUM_BYTES_IN_FLOAT * 3)

    isPresent: bool = False
    best: Transform3d = field(default_factory=Transform3d)
    alt: Transform3d = field(default_factory=Transform3d)
    ambiguity: float = 0.0
    bestReprojError: float = 0.0
    altReprojError: float = 0.0

    def createFromPacket(self, packet: Packet) -> Packet:
        self.isPresent = packet.decodeBoolean()

        if not self.isPresent:
            return packet

        self.best = packet.decodeTransform()
        self.alt = packet.decodeTransform()
        self.bestReprojError = packet.decodeDouble()
        self.altReprojError = packet.decodeDouble()
        self.ambiguity = packet.decodeDouble()
        return packet


@dataclass
class MultiTargetPNPResult:
    _MAX_IDS = 32
    # pnpresult + MAX_IDS possible targets (arbitrary upper limit that should never be hit, ideally)
    _PACK_SIZE_BYTES = PNPResult.PACK_SIZE_BYTES + (1 * _MAX_IDS)

    estimatedPose: PNPResult = field(default_factory=PNPResult)
    fiducialIDsUsed: list[int] = field(default_factory=list)

    def createFromPacket(self, packet: Packet) -> Packet:
        self.estimatedPose = PNPResult()
        self.estimatedPose.createFromPacket(packet)
        self.fiducialIDsUsed = []
        for _ in range(MultiTargetPNPResult._MAX_IDS):
            fidId = packet.decode16()
            if fidId >= 0:
                self.fiducialIDsUsed.append(fidId)
        return packet
