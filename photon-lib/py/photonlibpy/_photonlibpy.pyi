"""
C++ bindings for photonlib
"""
from __future__ import annotations
import robotpy_apriltag._apriltag
import typing
import wpimath.geometry._geometry
from wpimath.geometry import Pose3d
from wpimath.geometry import Transform3d
from wpimath.geometry import Translation3d
__all__ = ['MultiTargetPNPResult', 'PhotonCamera', 'PhotonCameraSim', 'PhotonPipelineMetadata', 'PhotonPipelineResult', 'PhotonTrackedTarget', 'Pose3d', 'TargetModel', 'Transform3d', 'Translation3d', 'VisionSystemSim', 'VisionTargetSim', 'print_t']
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
    def EnableDrawWireframe(self, enabled: bool) -> None:
        ...
    def __init__(self, arg0: PhotonCamera) -> None:
        ...
class PhotonPipelineMetadata:
    """
    Metadata about the frame that this result was constructed from
    """
    @property
    def captureTimestampMicros(self) -> int:
        """
        Timestamp (in the coprocessor timebase) that this frame was captured at
        """
    @property
    def publishTimestampMicros(self) -> int:
        """
        Timestamp (in the coprocessor timebase) that this frame was published to NetworkTables at
        """
    @property
    def sequenceID(self) -> int:
        """
        Number of frames processed since this VisionModule was started
        """
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
    def Vertices(self) -> list[wpimath.geometry._geometry.Translation3d]:
        ...
class VisionSystemSim:
    def AddAprilTags(self, layout: robotpy_apriltag._apriltag.AprilTagFieldLayout) -> None:
        ...
    def AddCamera(self, cameraSim: PhotonCameraSim, robotToCamera: wpimath.geometry._geometry.Transform3d) -> None:
        ...
    def AddVisionTargets(self, type: str, targets: list[VisionTargetSim]) -> None:
        ...
    def Update(self, arg0: wpimath.geometry._geometry.Pose3d) -> None:
        ...
    def __init__(self, visionSystemName: str) -> None:
        ...
class VisionTargetSim:
    @typing.overload
    def __init__(self, pose: wpimath.geometry._geometry.Pose3d, model: TargetModel) -> None:
        """
        Create a simulated target at a given pose
        """
    @typing.overload
    def __init__(self, pose: wpimath.geometry._geometry.Pose3d, model: TargetModel, fiducial_id: int) -> None:
        """
        Create a simulated AprilTag at a given pose
        """
def print_t(arg0: wpimath.geometry._geometry.Translation3d) -> None:
    """
    A function to print a pose
    """
