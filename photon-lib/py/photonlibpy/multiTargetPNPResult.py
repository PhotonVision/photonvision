from wpimath.geometry import Transform3d
from photonlibpy.packet import Packet

class PNPResult:

    _NUM_BYTES_IN_FLOAT = 8
    PACK_SIZE_BYTES = 1 + (_NUM_BYTES_IN_FLOAT * 7 * 2) + (_NUM_BYTES_IN_FLOAT* 3)

    def __init__(self):
        self.isPresent = False
        self.best = Transform3d()
        self.alt = Transform3d()
        self.ambiguity = 0.0
        self.bestReprojError = 0.0
        self.altReprojError = 0.0

    def createFromPacket(self, packet:Packet) -> Packet:
        self.isPresent = packet.decodeBoolean()
        self.best = packet.decodeTransform()
        self.alt = packet.decodeTransform()
        self.bestReprojError = packet.decodeDouble()
        self.altReprojError = packet.decodeDouble()
        self.ambiguity = packet.decodeDouble()
        return packet

    def __str__(self) -> str:
        return f"PNPResult {{isPresent={self.isPresent}, best={self.best}, bestReprojError={self.bestReprojError}, alt={self.alt}, altReprojError={self.altReprojError}, ambiguity={self.ambiguity}}}"
    

class MultiTargetPNPResult:

    MAX_IDS = 32
    # pnpresult + MAX_IDS possible targets (arbitrary upper limit that should never be hit, ideally)
    PACK_SIZE_BYTES = PNPResult.PACK_SIZE_BYTES + (1 * MAX_IDS)

    def __init__(self):
        self.estimatedPose = PNPResult()
        self.fiducialIDsUsed = []

    def createFromPacket(self, packet:Packet) -> Packet:
        self.estimatedPose = PNPResult()
        self.estimatedPose.createFromPacket(packet)
        self.fiducialIDsUsed = []
        for _ in range(MultiTargetPNPResult.MAX_IDS):
            fidId = packet.decode16()
            if(fidId >= 0):
                self.fiducialIDsUsed.append(fidId)
        return packet

    def __str__(self) -> str:
        return f"MultiTargetPNPResult {{estimatedPose={self.estimatedPose},fiducialIDsUsed={self.fiducialIDsUsed}}}"
