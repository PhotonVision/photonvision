from photonlibpy.multiTargetPNPResult import MultiTargetPNPResult
from photonlibpy.packet import Packet
from photonlibpy.photonTrackedTarget import PhotonTrackedTarget

class PhotonPipelineResult:
    def __init__(self):
        self.latencyMillis = -0.0
        self.timestampSec = -1.0
        self.targets:list[PhotonTrackedTarget] = []
        self.multiTagResult = MultiTargetPNPResult()
        
    def populateFromPacket(self, packet:Packet) -> Packet:
        self.targets = []
        self.latencyMillis = packet.decodeDouble()
        self.multiTagResult = MultiTargetPNPResult()
        self.multiTagResult.createFromPacket(packet)
        targetCount = packet.decode8()
        for _ in range(targetCount):
            target = PhotonTrackedTarget()
            target.createFromPacket(packet)
            self.targets.append(target)

        return packet
    
    def setTimestampSeconds(self, timestampSec:float) -> None:
        self.timestampSec = timestampSec
        
    def getLatencyMillis(self) -> float:
        return self.latencyMillis
    
    def getTimestamp(self) -> float:
        return self.timestampSec
    
    def getTargets(self) -> list[PhotonTrackedTarget]:
        return self.targets