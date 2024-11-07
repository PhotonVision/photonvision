from wpimath.geometry import Pose3d, Rotation3d, Translation3d, Transform3d

from typing import Self


class RotTrlTransform3d:
    def __init__(
        self, rot: Rotation3d = Rotation3d(), trl: Translation3d = Translation3d()
    ):
        self.rot = rot
        self.trl = trl

    def inverse(self) -> Self:
        invRot = -self.rot
        invTrl = -(self.trl.rotateBy(invRot))
        return RotTrlTransform3d(invRot, invTrl)

    def getTransform(self) -> Transform3d:
        return Transform3d(self.trl, self.rot)

    def getTranslation(self) -> Translation3d:
        return self.trl

    def getRotation(self) -> Rotation3d:
        return self.rot

    def apply(self, trlToApply: Translation3d) -> Translation3d:
        return trlToApply.rotateBy(self.rot) + self.trl

    @classmethod
    def makeRelativeTo(cls, pose: Pose3d) -> Self:
        return cls(pose.rotation(), pose.translation()).inverse()
