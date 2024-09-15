###############################################################################
## Copyright (C) Photon Vision.
###############################################################################
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <https://www.gnu.org/licenses/>.
###############################################################################

from dataclasses import dataclass
from typing import TYPE_CHECKING

from wpimath.geometry import Pose3d

from .targeting.photonTrackedTarget import PhotonTrackedTarget

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
