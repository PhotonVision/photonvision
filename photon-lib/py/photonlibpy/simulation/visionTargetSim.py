import math
from typing import overload

from wpimath.geometry import Pose3d, Translation3d

from ..estimation.targetModel import TargetModel


class VisionTargetSim:
    """Describes a vision target located somewhere on the field that your vision system can detect."""

    @overload
    def __init__(self, pose: Pose3d, model: TargetModel) -> None: ...
    @overload
    def __init__(self, pose: Pose3d, model: TargetModel, id: int) -> None: ...
    @overload
    def __init__(
        self, pose: Pose3d, model: TargetModel, objDetClassId: int, objDetConf: float
    ) -> None: ...

    def __init__(
        self,
        pose: Pose3d,
        model: TargetModel,
        id: int = -1,
        objDetClassId: int = -1,
        objDetConf: float = -1.0,
    ):
        """
        :param pose:          Pose3d of the target in field-relative coordinates
        :param model:         TargetModel which describes the shape of the target
        :param id:            The ID of this fiducial tag, or -1 if not a fiducial
        :param objDetClassId: The object detection class ID of this target, or -1 to
                              exclude from object detection
        :param objDetConf:    The object detection confidence, or -1.0 to compute from
                              target area in the camera's field of view
        """
        self.pose: Pose3d = pose
        self.model: TargetModel = model
        self.fiducialId: int = id
        self.objDetClassId: int = objDetClassId
        self.objDetConf: float = objDetConf

    def __lt__(self, right) -> bool:
        return self.pose.translation().norm() < right.pose.translation().norm()

    def __eq__(self, other) -> bool:
        # Use 1 inch and 1 degree tolerance
        return (
            abs(self.pose.translation().X() - other.getPose().translation().X())
            < 0.0254
            and abs(self.pose.translation().Y() - other.getPose().translation().Y())
            < 0.0254
            and abs(self.pose.translation().Z() - other.getPose().translation().Z())
            < 0.0254
            and abs(self.pose.rotation().X() - other.getPose().rotation().X())
            < math.radians(1)
            and abs(self.pose.rotation().Y() - other.getPose().rotation().Y())
            < math.radians(1)
            and abs(self.pose.rotation().Z() - other.getPose().rotation().Z())
            < math.radians(1)
            and self.model.getIsPlanar() == other.getModel().getIsPlanar()
        )

    def setPose(self, newPose: Pose3d) -> None:
        self.pose = newPose

    def setModel(self, newModel: TargetModel) -> None:
        self.model = newModel

    def getPose(self) -> Pose3d:
        return self.pose

    def getModel(self) -> TargetModel:
        return self.model

    def getFieldVertices(self) -> list[Translation3d]:
        """This target's vertices offset from its field pose."""
        return self.model.getFieldVertices(self.pose)
