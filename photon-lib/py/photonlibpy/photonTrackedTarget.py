from wpimath.geometry import Transform3d
from photonlibpy.packet import Packet


class TargetCorner:
    def __init__(self, x:float, y:float):
        self.x = x
        self.y = y

class PhotonTrackedTarget:

    _MAX_CORNERS = 8
    _NUM_BYTES_IN_FLOAT = 8
    _PACK_SIZE_BYTES = _NUM_BYTES_IN_FLOAT * (5 + 7 + 2 * 4 + 1 + 7 + 2 * _MAX_CORNERS)

    def __init__(self, yaw:float=0, pitch:float=0, area:float=0, skew:float=0, 
                 id:int=0, pose:Transform3d=Transform3d(), altPose: Transform3d=Transform3d(), 
                 ambiguity:float=0, 
                 minAreaRectCorners: list[TargetCorner]|None = None, 
                 detectedCorners: list[TargetCorner]|None = None):
        self.yaw = yaw
        self.pitch = pitch
        self.area = area
        self.skew = skew
        self.fiducialId  = id
        self.bestCameraToTarget  = pose
        self.altCameraToTarget  = altPose
        self.minAreaRectCorners = minAreaRectCorners
        self.detectedCorners = detectedCorners
        self.poseAmbiguity = ambiguity

    def getYaw(self) -> float:
        return self.yaw
    
    def getPitch(self) -> float:
        return self.pitch
    
    def getArea(self) -> float:
        return self.area
    
    def getSkew(self) -> float:
        return self.skew
    
    def getFiducialId(self) -> int:
        return self.fiducialId
    
    def getPoseAmbiguity(self) -> float:
        return self.poseAmbiguity
    
    def getMinAreaRectCorners(self) -> list[TargetCorner]|None:
        return self.minAreaRectCorners
    
    def getDetectedCorners(self) -> list[TargetCorner]|None:
        return self.detectedCorners
    
    def getBestCameraToTarget(self) -> Transform3d:
        return self.bestCameraToTarget
    
    def getAlternateCameraToTarget(self) -> Transform3d:
        return self.altCameraToTarget
    
    def _decodeTargetList(self, packet:Packet, numTargets:int) -> list[TargetCorner]:
        retList = []
        for _ in range(numTargets):
            cx = packet.decodeDouble()
            cy = packet.decodeDouble()
            retList.append(TargetCorner(cx, cy))
        return retList

    def createFromPacket(self, packet:Packet) -> Packet:
        self.yaw = packet.decodeDouble()
        self.pitch = packet.decodeDouble()
        self.area = packet.decodeDouble()
        self.skew = packet.decodeDouble()
        self.fiducialId = packet.decode32()

        self.bestCameraToTarget = packet.decodeTransform()
        self.altCameraToTarget  = packet.decodeTransform()

        self.poseAmbiguity = packet.decodeDouble()

        self.minAreaRectCorners = self._decodeTargetList(packet, 4) # always four
        numCorners = packet.decode8()
        self.detectedCorners = self._decodeTargetList(packet, numCorners)
        return packet
    
    def __str__(self) -> str:
        return f"PhotonTrackedTarget{{yaw={self.yaw},pitch={self.pitch},area={self.area},skew={self.skew},fiducialId={self.fiducialId},bestCameraToTarget={self.bestCameraToTarget}}}"
