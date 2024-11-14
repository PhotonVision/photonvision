import math
from typing import List, Self

from wpimath.geometry import Pose3d, Rotation2d, Rotation3d, Translation3d
from wpimath.units import meters

from . import RotTrlTransform3d


class TargetModel:

    def __init__(self):
        self.vertices: List[Translation3d] = []
        self.isPlanar = False
        self.isSpherical = False

    @classmethod
    def createPlanar(cls, width: meters, height: meters) -> Self:
        tm = cls()

        tm.isPlanar = True
        tm.isSpherical = False
        tm.vertices = [
            Translation3d(0.0, -width / 2.0, -height / 2.0),
            Translation3d(0.0, width / 2.0, -height / 2.0),
            Translation3d(0.0, width / 2.0, height / 2.0),
            Translation3d(0.0, -width / 2.0, height / 2.0),
        ]
        return tm

    @classmethod
    def createCuboid(cls, length: meters, width: meters, height: meters) -> Self:
        tm = cls()
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

        tm._common_construction(verts)

        return tm

    @classmethod
    def createSpheroid(cls, diameter: meters) -> Self:
        tm = cls()

        tm.isPlanar = False
        tm.isSpherical = True
        tm.vertices = [
            Translation3d(0.0, -diameter / 2.0, 0.0),
            Translation3d(0.0, 0.0, -diameter / 2.0),
            Translation3d(0.0, diameter / 2.0, 0.0),
            Translation3d(0.0, 0.0, diameter / 2.0),
        ]

        return tm

    @classmethod
    def createArbitrary(cls, verts: List[Translation3d]) -> Self:
        tm = cls()
        tm._common_construction(verts)

        return tm

    def _common_construction(self, verts: List[Translation3d]) -> None:
        self.isSpherical = False
        if len(verts) <= 2:
            self.vertices = []
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
            retVal.append(basisChange.applyTranslation(vert))

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
        return cls.createPlanar(width=6.5 * 0.0254, height=6.5 * 0.0254)

    @classmethod
    def AprilTag16h5(cls) -> Self:
        return cls.createPlanar(width=6.0 * 0.0254, height=6.0 * 0.0254)
