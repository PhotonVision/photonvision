import math

import ntcore as nt
import pytest
from photonlibpy.estimation import RotTrlTransform3d, TargetModel
from photonlibpy.estimation.openCVHelp import OpenCVHelp
from photonlibpy.photonCamera import setVersionCheckEnabled
from photonlibpy.simulation import SimCameraProperties, VisionTargetSim
from wpimath.geometry import Pose3d, Rotation3d, Translation3d


@pytest.fixture(autouse=True)
def setupCommon() -> None:
    nt.NetworkTableInstance.getDefault().startServer()
    setVersionCheckEnabled(False)


def test_TrlConvert():
    trl = Translation3d(0.75, -0.4, 0.1)
    tvec = OpenCVHelp.translationToTVec([trl])
    result = OpenCVHelp.tVecToTranslation(tvec[0])

    assert result.X() == pytest.approx(trl.X(), 0.005)
    assert result.Y() == pytest.approx(trl.Y(), 0.005)
    assert result.Z() == pytest.approx(trl.Z(), 0.005)


def test_RotConvert():
    rot = Rotation3d(0.5, 1, -1)
    rvec = OpenCVHelp.rotationToRVec(rot)
    result = OpenCVHelp.rVecToRotation(rvec[0])

    assert result.X() == pytest.approx(rot.X(), 0.25)
    assert result.Y() == pytest.approx(rot.Y(), 0.25)
    assert result.Z() == pytest.approx(rot.Z(), 0.25)


def test_Projection():
    prop = SimCameraProperties()

    target = VisionTargetSim(
        Pose3d(Translation3d(1.0, 0.0, 0.0), Rotation3d(0.0, 0.0, math.pi)),
        TargetModel.AprilTag16h5(),
        4774,
    )

    cameraPose = Pose3d(Translation3d(), Rotation3d())

    camRt = RotTrlTransform3d.makeRelativeTo(cameraPose)
    imagePoints = OpenCVHelp.projectPoints(
        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices()
    )

    # find circulation (counter/clockwise-ness)
    circulation = 0.0
    for i in range(0, len(imagePoints)):
        xDiff = imagePoints[(i + 1) % 4][0][0] - imagePoints[i][0][0]
        ySum = imagePoints[(i + 1) % 4][0][1] + imagePoints[i][0][1]
        circulation += xDiff * ySum

    assert circulation > 0, "2d fiducial points aren't counter-clockwise"

    # TODO Uncomment after OpenCVHelp.undistortPoints is implemented
    # # undo projection distortion
    # imagePoints = OpenCVHelp.undistortPoints(
    #     prop.getIntrinsics(), prop.getDistCoeffs(), imagePoints
    # )
    # # test projection results after moving camera
    # avgCenterRot1 = prop.getPixelRot(OpenCVHelp.avgPoint(imagePoints))
    # cameraPose = cameraPose + Transform3d(Translation3d(), Rotation3d(0.0, 0.25, 0.25))
    # camRt = RotTrlTransform3d.makeRelativeTo(cameraPose)
    # imagePoints = OpenCVHelp.projectPoints(
    #     prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices()
    # )
    # avgCenterRot2 = prop.getPixelRot(OpenCVHelp.avgPoint(imagePoints))

    # yaw2d = Rotation2d(avgCenterRot2.Z())
    # pitch2d = Rotation2d(avgCenterRot2.Y())
    # yawDiff = yaw2d - Rotation2d(avgCenterRot1.Z())
    # pitchDiff = pitch2d - Rotation2d(avgCenterRot2.Y())
    # assert yawDiff.radians() < 0.0, "2d points don't follow yaw"
    # assert pitchDiff.radians() < 0.0, "2d points don't follow pitch"

    # actualRelation = CameraTargetRelation(cameraPose, target.getPose())

    # assert actualRelation.camToTargPitch.degrees() == pytest.approx(
    #     pitchDiff.degrees()
    #     * math.cos(yaw2d.radians()),  # adjust for unaccounted perspective distortion
    #     abs=0.25,
    # ), "2d pitch doesn't match 3d"
    # assert actualRelation.camToTargYaw.degrees() == pytest.approx(
    #     yawDiff.degrees(), abs=0.25
    # ), "2d yaw doesn't match 3d"


def test_SolvePNP_SQUARE():
    prop = SimCameraProperties()

    # square AprilTag target
    target = VisionTargetSim(
        Pose3d(Translation3d(5.0, 0.5, 1.0), Rotation3d(0.0, 0.0, math.pi)),
        TargetModel.AprilTag16h5(),
        4774,
    )
    cameraPose = Pose3d(Translation3d(), Rotation3d())
    camRt = RotTrlTransform3d.makeRelativeTo(cameraPose)

    # target relative to camera
    relTarget = camRt.applyPose(target.getPose())

    # simulate solvePNP estimation
    targetCorners = OpenCVHelp.projectPoints(
        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices()
    )

    pnpSim = OpenCVHelp.solvePNP_Square(
        prop.getIntrinsics(),
        prop.getDistCoeffs(),
        target.getModel().vertices,
        targetCorners,
    )

    assert pnpSim is not None

    # check solvePNP estimation accuracy
    assert relTarget.rotation().X() == pytest.approx(
        pnpSim.best.rotation().X(), abs=0.25
    )
    assert relTarget.rotation().Y() == pytest.approx(
        pnpSim.best.rotation().Y(), abs=0.25
    )
    assert relTarget.rotation().Z() == pytest.approx(
        pnpSim.best.rotation().Z(), abs=0.25
    )

    assert relTarget.translation().X() == pytest.approx(
        pnpSim.best.translation().X(), abs=0.005
    )
    assert relTarget.translation().Y() == pytest.approx(
        pnpSim.best.translation().Y(), abs=0.005
    )
    assert relTarget.translation().Z() == pytest.approx(
        pnpSim.best.translation().Z(), abs=0.005
    )


def test_SolvePNP_SQPNP():
    prop = SimCameraProperties()

    # (for targets with arbitrary number of non-colinear points > 2)
    target = VisionTargetSim(
        Pose3d(Translation3d(5.0, 0.5, 1.0), Rotation3d(0.0, 0.0, math.pi)),
        TargetModel.createArbitrary(
            verts=[
                Translation3d(0.0, 0.0, 0.0),
                Translation3d(1.0, 0.0, 0.0),
                Translation3d(0.0, 1.0, 0.0),
                Translation3d(0.0, 0.0, 1.0),
                Translation3d(0.125, 0.25, 0.5),
                Translation3d(0.0, 0.0, -1.0),
                Translation3d(0.0, -1.0, 0.0),
                Translation3d(-1.0, 0.0, 0.0),
            ]
        ),
        4774,
    )
    cameraPose = Pose3d(Translation3d(), Rotation3d())
    camRt = RotTrlTransform3d.makeRelativeTo(cameraPose)
    # target relative to camera
    relTarget = camRt.applyPose(target.getPose())

    # simulate solvePNP estimation
    targetCorners = OpenCVHelp.projectPoints(
        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, target.getFieldVertices()
    )

    pnpSim = OpenCVHelp.solvePNP_SQPNP(
        prop.getIntrinsics(),
        prop.getDistCoeffs(),
        target.getModel().vertices,
        targetCorners,
    )

    assert pnpSim is not None

    # check solvePNP estimation accuracy
    assert relTarget.rotation().X() == pytest.approx(
        pnpSim.best.rotation().X(), abs=0.25
    )
    assert relTarget.rotation().Y() == pytest.approx(
        pnpSim.best.rotation().Y(), abs=0.25
    )
    assert relTarget.rotation().Z() == pytest.approx(
        pnpSim.best.rotation().Z(), abs=0.25
    )

    assert relTarget.translation().X() == pytest.approx(
        pnpSim.best.translation().X(), abs=0.005
    )
    assert relTarget.translation().Y() == pytest.approx(
        pnpSim.best.translation().Y(), abs=0.005
    )
    assert relTarget.translation().Z() == pytest.approx(
        pnpSim.best.translation().Z(), abs=0.005
    )
