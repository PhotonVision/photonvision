import math
from typing import overload

from wpimath import Pose3d, Translation3d

from ..estimation.targetModel import TargetModel


class VisionTargetSim:
    """Describes a vision target located somewhere on the field that your vision system can detect."""

    @overload
    def __init__(self, pose: Pose3d, model: TargetModel) -> None:
        """
        Describes a retro-reflective/colored shape vision target located somewhere on the field that
        your vision system can detect.

        :param pose:  Pose3d of the target in field-relative coordinates
        :param model: TargetModel which describes the shape of the target
        """
        ...

    @overload
    def __init__(self, pose: Pose3d, model: TargetModel, id: int) -> None:
        """
        Describes a fiducial tag located somewhere on the field that your vision system can detect.

        :param pose:  Pose3d of the tag in field-relative coordinates
        :param model: TargetModel which describes the geometry of the target (tag)
        :param id:    The ID of this fiducial tag
        """
        ...

    @overload
    def __init__(
        self, pose: Pose3d, model: TargetModel, objDetClassId: int, objDetConf: float
    ) -> None:
        """
        Describes an object-detection vision target located somewhere on the field that your vision
        system can detect.

        :param pose:          Pose3d of the target in field-relative coordinates
        :param model:         TargetModel which describes the shape of the target
        :param objDetClassId: The object detection class ID, or -1 to exclude from object detection
        :param objDetConf:    The object detection confidence, or -1.0 to compute from target area
                              in the camera's field of view
        """
        ...

    def __init__(
        self,
        pose: Pose3d,
        model: TargetModel,
        *args,
        **kwargs,
    ):
        if kwargs:
            raise TypeError(
                f"VisionTargetSim does not accept keyword arguments: {list(kwargs.keys())}"
            )

        self.pose: Pose3d = pose
        self.model: TargetModel = model

        if len(args) == 0:
            # VisionTargetSim(pose, model)
            self.fiducialId: int = -1
            self.objDetClassId: int = -1
            self.objDetConf: float = -1.0
        elif len(args) == 1:
            # VisionTargetSim(pose, model, id)
            self.fiducialId = args[0]
            self.objDetClassId = -1
            self.objDetConf = -1.0
        elif len(args) == 2:
            # VisionTargetSim(pose, model, objDetClassId, objDetConf)
            self.fiducialId = -1
            self.objDetClassId = args[0]
            self.objDetConf = args[1]
        else:
            raise ValueError(
                f"VisionTargetSim takes 2-4 arguments, got {2 + len(args)}"
            )

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
