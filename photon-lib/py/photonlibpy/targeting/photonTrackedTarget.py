from dataclasses import dataclass, field
from wpimath.geometry import Transform3d
from ..packet import Packet
from .TargetCorner import TargetCorner


@dataclass
class PhotonTrackedTarget:
    yaw: float = 0.0
    pitch: float = 0.0
    area: float = 0.0
    skew: float = 0.0
    fiducialId: int = -1
    bestCameraToTarget: Transform3d = field(default_factory=Transform3d)
    altCameraToTarget: Transform3d = field(default_factory=Transform3d)
    minAreaRectCorners: list[TargetCorner] | None = None
    detectedCorners: list[TargetCorner] | None = None
    poseAmbiguity: float = 0.0

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

    def getMinAreaRectCorners(self) -> list[TargetCorner] | None:
        return self.minAreaRectCorners

    def getDetectedCorners(self) -> list[TargetCorner] | None:
        return self.detectedCorners

    def getBestCameraToTarget(self) -> Transform3d:
        return self.bestCameraToTarget

    def getAlternateCameraToTarget(self) -> Transform3d:
        return self.altCameraToTarget

    def _decodeTargetList(self, packet: Packet, numTargets: int) -> list[TargetCorner]:
        retList = []
        for _ in range(numTargets):
            cx = packet.decodeDouble()
            cy = packet.decodeDouble()
            retList.append(TargetCorner(cx, cy))
        return retList

    photonStruct: "PhotonTrackedTargetSerde" = None
