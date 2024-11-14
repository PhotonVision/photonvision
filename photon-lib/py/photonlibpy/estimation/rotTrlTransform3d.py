from typing import Self

from wpimath.geometry import Pose3d, Rotation3d, Transform3d, Translation3d


class RotTrlTransform3d:
    def __init__(
        self, rot: Rotation3d = Rotation3d(), trl: Translation3d = Translation3d()
    ):
        self.rot = rot
        self.trl = trl

    def inverse(self) -> Self:
        invRot = -self.rot
        invTrl = -(self.trl.rotateBy(invRot))
        return type(self)(invRot, invTrl)

    def getTransform(self) -> Transform3d:
        return Transform3d(self.trl, self.rot)

    def getTranslation(self) -> Translation3d:
        return self.trl

    def getRotation(self) -> Rotation3d:
        return self.rot

    def applyTranslation(self, trlToApply: Translation3d) -> Translation3d:
        return trlToApply.rotateBy(self.rot) + self.trl

    def applyRotation(self, rotToApply: Rotation3d) -> Rotation3d:
        return rotToApply + self.rot

    def applyPose(self, poseToApply: Pose3d) -> Pose3d:
        return Pose3d(
            self.applyTranslation(poseToApply.translation()),
            self.applyRotation(poseToApply.rotation()),
        )

    def applyTrls(self, rots: list[Rotation3d]) -> list[Rotation3d]:
        retVal: list[Rotation3d] = []
        for rot in rots:
            retVal.append(self.applyRotation(rot))
        return retVal

    @classmethod
    def makeRelativeTo(cls, pose: Pose3d) -> Self:
        return cls(pose.rotation(), pose.translation()).inverse()

    @classmethod
    def makeBetweenPoses(cls, initial: Pose3d, last: Pose3d) -> Self:
        return cls(
            last.rotation() - initial.rotation(),
            last.translation()
            - initial.translation().rotateBy(last.rotation() - initial.rotation()),
        )
