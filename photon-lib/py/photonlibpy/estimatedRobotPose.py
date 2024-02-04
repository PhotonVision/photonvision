from dataclasses import dataclass
from typing import TYPE_CHECKING

from wpimath.geometry import Pose3d

from .photonTrackedTarget import PhotonTrackedTarget

if TYPE_CHECKING:
    from .photonPoseEstimator import PoseStrategy


@dataclass
class EstimatedRobotPose:
    """An estimated pose based on pipeline result"""

    estimatedPose: Pose3d
    """The estimated pose"""

    timestampSeconds: float
    """The estimated time the frame used to derive the robot pose was taken"""

    targetsUsed: list[PhotonTrackedTarget]
    """A list of the targets used to compute this pose"""

    strategy: "PoseStrategy"
    """The strategy actually used to produce this pose"""
