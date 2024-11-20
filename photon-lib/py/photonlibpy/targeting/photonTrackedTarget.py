from dataclasses import dataclass, field
from typing import TYPE_CHECKING, ClassVar

from wpimath.geometry import Transform3d

from ..packet import Packet
from .TargetCorner import TargetCorner

if TYPE_CHECKING:
    from ..generated.PhotonTrackedTargetSerde import PhotonTrackedTargetSerde


@dataclass
class PhotonTrackedTarget:
    yaw: float = 0.0
    pitch: float = 0.0
    area: float = 0.0
    skew: float = 0.0
    fiducialId: int = -1
    bestCameraToTarget: Transform3d = field(default_factory=Transform3d)
    altCameraToTarget: Transform3d = field(default_factory=Transform3d)
    minAreaRectCorners: list[TargetCorner] = field(default_factory=list[TargetCorner])
    detectedCorners: list[TargetCorner] = field(default_factory=list[TargetCorner])
    poseAmbiguity: float = 0.0
    objDetectId: int = -1
    objDetectConf: float = 0.0

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

    def getMinAreaRectCorners(self) -> list[TargetCorner]:
        return self.minAreaRectCorners

    def getDetectedCorners(self) -> list[TargetCorner]:
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

    photonStruct: ClassVar["PhotonTrackedTargetSerde"]
