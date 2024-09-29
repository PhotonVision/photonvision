"""
C++ bindings for photonlib
"""
from __future__ import annotations
import typing
from wpimath.geometry import Pose3d
from wpimath.geometry import Transform3d
from wpimath.geometry import Translation3d
__all__ = ['MultiTargetPNPResult', 'PhotonCamera', 'PhotonCameraSim', 'PhotonPipelineMetadata', 'PhotonPipelineResult', 'PhotonTrackedTarget', 'Pose3d', 'TargetModel', 'Transform3d', 'Translation3d', 'VisionSystemSim', 'VisionTargetSim']
class MultiTargetPNPResult:
    @property
    def fiducialIDsUsed(self) -> list[int]:
        ...
class PhotonCamera:
    def GetDriverMode(self) -> bool:
        ...
    def GetLatestResult(self) -> PhotonPipelineResult:
        ...
    def __init__(self, arg0: str) -> None:
        ...
class PhotonCameraSim:
    def __init__(self, arg0: PhotonCamera) -> None:
        ...
class PhotonPipelineMetadata:
    @property
    def captureTimestampMicros(self) -> int:
        ...
    @property
    def publishTimestampMicros(self) -> int:
        ...
    @property
    def sequenceID(self) -> int:
        ...
class PhotonPipelineResult:
    def __repr__(self) -> str:
        ...
    @property
    def metadata(self) -> PhotonPipelineMetadata:
        ...
    @property
    def multitagResult(self) -> MultiTargetPNPResult | None:
        ...
    @property
    def targets(self) -> list[PhotonTrackedTarget]:
        ...
class PhotonTrackedTarget:
    def __repr__(self) -> str:
        ...
    @property
    def pitch(self) -> float:
        ...
    @property
    def yaw(self) -> float:
        ...
class TargetModel:
    kAprilTag16h5: typing.ClassVar[TargetModel]  # value = <photonlibpy._photonlibpy.TargetModel object>
    kAprilTag36h11: typing.ClassVar[TargetModel]  # value = <photonlibpy._photonlibpy.TargetModel object>
    @property
    def IsPlaner(self) -> bool:
        ...
    @property
    def IsSpherical(self) -> bool:
        ...
    @property
    def Vertices(self) -> list[Translation3d]:
        ...
class VisionSystemSim:
    def AddCamera(self, arg0: PhotonCameraSim, arg1: Transform3d) -> None:
        ...
    def AddVisionTargets(self, type: str, targets: list[VisionTargetSim]) -> None:
        ...
    def Update(self, arg0: Pose3d) -> None:
        ...
    def __init__(self, visionSystemName: str) -> None:
        ...
class VisionTargetSim:
    @typing.overload
    def __init__(self, pose: Pose3d, model: TargetModel) -> None:
        """
        Create a simulated target at a given pose
        """
    @typing.overload
    def __init__(self, pose: Pose3d, model: TargetModel, fiducial_id: int) -> None:
        """
        Create a simulated AprilTag at a given pose
        """
