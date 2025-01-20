from typing import Self

from wpimath.geometry import Pose3d, Rotation3d, Transform3d, Translation3d


class RotTrlTransform3d:
    """Represents a transformation that first rotates a pose around the origin, and then translates it."""

    def __init__(
        self, rot: Rotation3d = Rotation3d(), trl: Translation3d = Translation3d()
    ):
        """A rotation-translation transformation.

        Applying this RotTrlTransform3d to poses will preserve their current origin-to-pose
        transform as if the origin was transformed by these components instead.

        :param rot: The rotation component
        :param trl: The translation component
        """
        self.rot = rot
        self.trl = trl

    def inverse(self) -> Self:
        """The inverse of this transformation. Applying the inverse will "undo" this transformation."""
        invRot = -self.rot
        invTrl = -(self.trl.rotateBy(invRot))
        return type(self)(invRot, invTrl)

    def getTransform(self) -> Transform3d:
        """This transformation as a Transform3d (as if of the origin)"""
        return Transform3d(self.trl, self.rot)

    def getTranslation(self) -> Translation3d:
        """The translation component of this transformation"""
        return self.trl

    def getRotation(self) -> Rotation3d:
        """The rotation component of this transformation"""
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
        """The rotation-translation transformation that makes poses in the world consider this pose as the
        new origin, or change the basis to this pose.

        :param pose: The new origin
        """
        return cls(pose.rotation(), pose.translation()).inverse()

    @classmethod
    def makeBetweenPoses(cls, initial: Pose3d, last: Pose3d) -> Self:
        return cls(
            last.rotation() - initial.rotation(),
            last.translation()
            - initial.translation().rotateBy(last.rotation() - initial.rotation()),
        )
