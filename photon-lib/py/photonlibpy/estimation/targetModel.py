import math
from typing import List, Self

from wpimath.geometry import Pose3d, Rotation2d, Rotation3d, Translation3d
from wpimath.units import meters

from . import RotTrlTransform3d


class TargetModel:
    """Describes the 3d model of a target."""

    def __init__(self):
        """Default constructor for initialising internal class members. DO NOT USE THIS!!! USE THE createPlanar,
        createCuboid, createSpheroid or create Arbitrary
        """
        self.vertices: List[Translation3d] = []
        self.isPlanar = False
        self.isSpherical = False

    @classmethod
    def createPlanar(cls, width: meters, height: meters) -> Self:
        """Creates a rectangular, planar target model given the width and height. The model has four
        vertices:

        - Point 0: [0, -width/2, -height/2]
        - Point 1: [0, width/2, -height/2]
        - Point 2: [0, width/2, height/2]
        - Point 3: [0, -width/2, height/2]
        """

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
        """Creates a cuboid target model given the length, width, height. The model has eight vertices:

        - Point 0: [length/2, -width/2, -height/2]
        - Point 1: [length/2, width/2, -height/2]
        - Point 2: [length/2, width/2, height/2]
        - Point 3: [length/2, -width/2, height/2]
        - Point 4: [-length/2, -width/2, height/2]
        - Point 5: [-length/2, width/2, height/2]
        - Point 6: [-length/2, width/2, -height/2]
        - Point 7: [-length/2, -width/2, -height/2]
        """

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
        """Creates a spherical target model which has similar dimensions regardless of its rotation. This
        model has four vertices:

        - Point 0: [0, -radius, 0]
        - Point 1: [0, 0, -radius]
        - Point 2: [0, radius, 0]
        - Point 3: [0, 0, radius]

        *Q: Why these vertices?* A: This target should be oriented to the camera every frame, much
        like a sprite/decal, and these vertices represent the ellipse vertices (maxima). These vertices
        are used for drawing the image of this sphere, but do not match the corners that will be
        published by photonvision.
        """

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
        """Creates a target model from arbitrary 3d vertices. Automatically determines if the given
        vertices are planar(x == 0). More than 2 vertices must be given. If this is a planar model, the
        vertices should define a non-intersecting contour.

        :param vertices: Translations representing the vertices of this target model relative to its
                         pose.
        """

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
        """This target's vertices offset from its field pose.

        Note: If this target is spherical, use {@link #getOrientedPose(Translation3d,
        Translation3d)} with this method.
        """

        basisChange = RotTrlTransform3d(targetPose.rotation(), targetPose.translation())

        retVal = []

        for vert in self.vertices:
            retVal.append(basisChange.applyTranslation(vert))

        return retVal

    @classmethod
    def getOrientedPose(cls, tgtTrl: Translation3d, cameraTrl: Translation3d):
        """Returns a Pose3d with the given target translation oriented (with its relative x-axis aligned)
        to the camera translation. This is used for spherical targets which should not have their
        projection change regardless of their own rotation.

        :param tgtTrl:    This target's translation
        :param cameraTrl: Camera's translation

        :returns: This target's pose oriented to the camera
        """

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
