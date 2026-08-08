import math

from wpimath import Pose3d, Rotation2d, Transform3d
from wpimath.units import meters


class CameraTargetRelation:
    def __init__(self, cameraPose: Pose3d, targetPose: Pose3d):
        self.camPose = cameraPose
        self.camToTarg = Transform3d(cameraPose, targetPose)
        self.camToTargDist = self.camToTarg.translation().norm()
        self.camToTargDistXY: meters = math.hypot(
            self.camToTarg.translation().x, self.camToTarg.translation().y
        )
        self.camToTargYaw = Rotation2d(self.camToTarg.x, self.camToTarg.y)
        self.camToTargPitch = Rotation2d(self.camToTargDistXY, -self.camToTarg.z)
        self.camToTargAngle = Rotation2d(
            math.hypot(self.camToTargYaw.radians(), self.camToTargPitch.radians())
        )
        self.targToCam = Transform3d(targetPose, cameraPose)
        self.targToCamYaw = Rotation2d(self.targToCam.x, self.targToCam.y)
        self.targToCamPitch = Rotation2d(self.camToTargDistXY, -self.targToCam.z)
        self.targtoCamAngle = Rotation2d(
            math.hypot(self.targToCamYaw.radians(), self.targToCamPitch.radians())
        )
