import ntcore as nt
from photonlibpy.photonCamera import PhotonCamera, setVersionCheckEnabled

from wpimath.geometry import (
    Pose3d,
    Translation3d,
    Rotation3d,
    Transform3d,
    Rotation2d,
    Pose2d,
    Translation2d,
)
from photonlibpy.simulation import VisionSystemSim, PhotonCameraSim, VisionTargetSim
from photonlibpy.estimation import TargetModel
import math


def setupCommon() -> None:

    nt.NetworkTableInstance.getDefault().startServer()
    setVersionCheckEnabled(False)


def test_VisibilityCupidShuffle():
    setupCommon()

    targetPose = Pose3d(Translation3d(15.98, 0.0, 2.0), Rotation3d(0, 0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibration(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))

    visionSysSim.addVisionTargets(
        [VisionTargetSim(targetPose, TargetModel(width=1.0, height=1.0), 3)]
    )

    # To the right, to the right
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(-70.0))
    visionSysSim.update(robotPose)

    assertFalse(camera.getLatestResult().hasTargets())

    # To the right, to the right
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(-95.0))
    visionSysSim.update(robotPose)
    assertFalse(camera.getLatestResult().hasTargets())

    # To the left, to the left
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(90.0))
    visionSysSim.update(robotPose)
    assertFalse(camera.getLatestResult().hasTargets())

    # To the left, to the left
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(65.0))
    visionSysSim.update(robotPose)
    assertFalse(camera.getLatestResult().hasTargets())

    # Now kick, now kick
    robotPose = Pose2d(Translation2d(2.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assertTrue(camera.getLatestResult().hasTargets())

    # Now kick, now kick
    robotPose = Pose2d(Translation2d(2.0, 0.0), Rotation2d.fromDegrees(-5.0))
    visionSysSim.update(robotPose)
    assertTrue(camera.getLatestResult().hasTargets())

    # Now walk it by yourself
    robotPose = Pose2d(Translation2d(2.0, 0.0), Rotation2d.fromDegrees(-179.0))
    visionSysSim.update(robotPose)
    assertFalse(camera.getLatestResult().hasTargets())

    # Now walk it by yourself
    visionSysSim.adjustCamera(
        cameraSim, Transform3d(Translation3d(), Rotation3d(0, 0, math.pi))
    )
    visionSysSim.update(robotPose)
    assertTrue(camera.getLatestResult().hasTargets())

def test_NotVisibleVert1():
    setupCommon()

    targetPose = Pose3d(Translation3d(15.98, 0.0, 2.0), Rotation3d(0, 0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibration(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))

    visionSysSim.addVisionTargets(
        [VisionTargetSim(targetPose, TargetModel(width=3.0, height=3.0), 3)]
    )

    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(5.0))

    visionSysSim.update(robotPose)
    assertTrue(camera.getLatestResult().hasTargets())

    visionSysSim.adjustCamera(cameraSim, Transform3d(Translation3d(0.0, 0.0, 5000.0), Rotation3d(0.0, 0.0, math.pi)))
    visionSysSim.update(robotPose)
    assertFalse(camera.getLatestResult().hasTargets())


def assertFalse(expected):
    assert not expected


def assertTrue(expected):
    assert expected
