from ..estimation.targetModel import TargetModel

from wpimath.geometry import Pose3d, Translation3d

import math


class VisionTargetSim:
    def __init__(self, pose: Pose3d, model: TargetModel, id: int = -1):
        self.pose: Pose3d = pose
        self.model: TargetModel = model
        self.fiducialId: int = id
        self.objDetClassId: int = -1
        self.objDetConf: float = -1.0

    def __lt__(self, right) -> bool:
        return self.pose.translation().norm() < right.pose.translation().norm()

    def __eq__(self, other) -> bool:
        # Use 1 inch and 1 degree tolerance
        return (
            abs(self.pose.translation().X() - other.GetPose().translation().X())
            < 0.0254
            and abs(self.pose.translation().Y() - other.GetPose().translation().Y())
            < 0.0254
            and abs(self.pose.translation().Z() - other.GetPose().translation().Z())
            < 0.0254
            and abs(self.pose.rotation().X() - other.GetPose().rotation().X())
            < math.radians(1)
            and abs(self.pose.rotation().Y() - other.GetPose().rotation().Y())
            < math.radians(1)
            and abs(self.pose.rotation().Z() - other.GetPose().rotation().Z())
            < math.radians(1)
            and self.model.GetIsPlanar() == other.GetModel().GetIsPlanar()
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
        return self.model.GetFieldVertices(self.pose)
