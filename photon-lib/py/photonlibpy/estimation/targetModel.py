from wpimath.geometry import Pose3d, Translation3d, Transform3d, Rotation3d, Rotation2d
from . import RotTrlTransform3d
import math
from typing import List, Self

from wpimath.units import meters


class TargetModel:
    def __init__(
        self,
        *,
        width: meters | None = None,
        height: meters | None = None,
        length: meters | None = None,
        diameter: meters | None = None,
        verts: List[Translation3d] | None = None
    ):

        if (
            width is not None
            and height is not None
            and length is None
            and diameter is None
            and verts is None
        ):
            self.isPlanar = True
            self.isSpherical = False
            self.vertices = [
                Translation3d(0.0, -width / 2.0, -height / 2.0),
                Translation3d(0.0, width / 2.0, -height / 2.0),
                Translation3d(0.0, width / 2.0, height / 2.0),
                Translation3d(0.0, -width / 2.0, height / 2.0),
            ]

            return

        elif (
            length is not None
            and width is not None
            and height is not None
            and diameter is None
            and verts is None
        ):
            verts = [
                Translation3d(length / 2.0, -width / 2.0, -height / 2.0),
                Translation3d(length / 2.0, width / 2.0, -height / 2.0),
                Translation3d(length / 2.0, width / 2.0, height / 2.0),
                Translation3d(length / 2.0, -width / 2.0, height / 2.0),
                Translation3d(-length / 2.0, -width / 2.0, height / 2.0),
                Translation3d(-length / 2.0, width / 2.0, height / 2.0),
                Translation3d(-length / 2.0, width / 2.0, -height / 2.0),
                Translation3d(-length / 2.0, -width / 2.0, -height / 2.0),
            ]
            # Handle the rest of this in the "default" case
        elif (
            diameter is not None
            and width is None
            and height is None
            and length is None
            and verts is None
        ):
            self.isPlanar = False
            self.isSpherical = True
            self.vertices = [
                Translation3d(0.0, -diameter / 2.0, 0.0),
                Translation3d(0.0, 0.0, -diameter / 2.0),
                Translation3d(0.0, diameter / 2.0, 0.0),
                Translation3d(0.0, 0.0, diameter / 2.0),
            ]
            return
        elif (
            verts is not None
            and width is None
            and height is None
            and length is None
            and diameter is None
        ):
            # Handle this in the "default" case
            pass
        else:
            raise Exception("Not a valid overload")

        # TODO maybe remove this if there is a better/preferred way
        # make the python type checking gods happy
        assert verts is not None

        self.isSpherical = False
        if len(verts) <= 2:
            self.vertices: List[Translation3d] = []
            self.isPlanar = False
        else:
            cornersPlaner = True
            for corner in verts:
                if abs(corner.X() < 1e-4):
                    cornersPlaner = False
            self.isPlanar = cornersPlaner

        self.vertices = verts

    def getFieldVertices(self, targetPose: Pose3d) -> List[Translation3d]:
        basisChange = RotTrlTransform3d(targetPose.rotation(), targetPose.translation())

        retVal = []

        for vert in self.vertices:
            retVal.append(basisChange.apply(vert))

        return retVal

    @classmethod
    def getOrientedPose(cls, tgtTrl: Translation3d, cameraTrl: Translation3d):
        relCam = cameraTrl - tgtTrl
        orientToCam = Rotation3d(
            0.0,
            Rotation2d(math.hypot(relCam.X(), relCam.Y()), relCam.Z()).radians(),
            Rotation2d(relCam.X(), relCam.Y()).radians(),
        )
        return Pose3d(tgtTrl, orientToCam)

    def getVertices(self) -> List[Translation3d]:
        return self.vertices

    def getIsPlanar(self) -> bool:
        return self.isPlanar

    def getIsSpherical(self) -> bool:
        return self.isSpherical

    @classmethod
    def AprilTag36h11(cls) -> Self:
        return cls(width=6.5 * 0.0254, height=6.5 * 0.0254)

    @classmethod
    def AprilTag16h5(cls) -> Self:
        return cls(width=6.0 * 0.0254, height=6.0 * 0.0254)
