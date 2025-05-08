import math

import pytest
from photonlibpy.estimation import TargetModel, VisionEstimation
from photonlibpy.photonCamera import PhotonCamera
from photonlibpy.simulation import PhotonCameraSim, VisionSystemSim, VisionTargetSim
from robotpy_apriltag import AprilTag, AprilTagFieldLayout
from wpimath.geometry import (
    Pose2d,
    Pose3d,
    Rotation2d,
    Rotation3d,
    Transform3d,
    Translation2d,
    Translation3d,
)
from wpimath.units import feetToMeters, meters


def test_VisibilityCupidShuffle() -> None:
    targetPose = Pose3d(Translation3d(15.98, 0.0, 2.0), Rotation3d(0, 0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))

    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=1.0, height=1.0), 4774
            )
        ]
    )

    # To the right, to the right
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(-70.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()

    # To the right, to the right
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(-95.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()

    # To the left, to the left
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(90.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()

    # To the left, to the left
    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(65.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()

    # Now kick, now kick
    robotPose = Pose2d(Translation2d(2.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()

    # Now kick, now kick
    robotPose = Pose2d(Translation2d(2.0, 0.0), Rotation2d.fromDegrees(-5.0))
    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()

    # Now walk it by yourself
    robotPose = Pose2d(Translation2d(2.0, 0.0), Rotation2d.fromDegrees(-179.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()

    # Now walk it by yourself
    visionSysSim.adjustCamera(
        cameraSim, Transform3d(Translation3d(), Rotation3d(0, 0, math.pi))
    )
    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()


def test_NotVisibleVert1() -> None:
    targetPose = Pose3d(Translation3d(15.98, 0.0, 2.0), Rotation3d(0, 0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))

    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=3.0, height=3.0), 4774
            )
        ]
    )

    robotPose = Pose2d(Translation2d(5.0, 0.0), Rotation2d.fromDegrees(5.0))

    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()

    visionSysSim.adjustCamera(
        cameraSim,
        Transform3d(Translation3d(0.0, 0.0, 5000.0), Rotation3d(0.0, 0.0, math.pi)),
    )
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()


def test_NotVisibleVert2() -> None:
    targetPose = Pose3d(Translation3d(15.98, 0.0, 2.0), Rotation3d(0, 0, math.pi))

    robotToCamera = Transform3d(
        Translation3d(0.0, 0.0, 1.0), Rotation3d(0.0, -math.pi / 4.0, 0.0)
    )

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, robotToCamera)

    cameraSim.prop.setCalibrationFromFOV(
        4774, 4774, fovDiag=Rotation2d.fromDegrees(80.0)
    )
    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=0.5, height=0.5), 4774
            )
        ]
    )

    robotPose = Pose2d(Translation2d(13.98, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()

    robotPose = Pose2d(Translation2d(0.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()


def test_NotVisibleTargetSize() -> None:
    targetPose = Pose3d(Translation3d(15.98, 0.0, 1.0), Rotation3d(0, 0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))
    cameraSim.setMinTargetAreaPixels(20.0)
    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=0.1, height=0.1), 4774
            )
        ]
    )

    robotPose = Pose2d(Translation2d(12.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()

    robotPose = Pose2d(Translation2d(0.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()


def test_NotVisibleTooFarLeds() -> None:
    targetPose = Pose3d(Translation3d(15.98, 0.0, 1.0), Rotation3d(0, 0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))
    cameraSim.setMinTargetAreaPixels(1.0)
    cameraSim.setMaxSightRange(10.0)
    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=1.0, height=1.0), 4774
            )
        ]
    )

    robotPose = Pose2d(Translation2d(10.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert camera.getLatestResult().hasTargets()

    robotPose = Pose2d(Translation2d(0.0, 0.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.update(robotPose)
    assert not camera.getLatestResult().hasTargets()


@pytest.mark.parametrize(
    "expected_yaw", [-10.0, -5.0, -2.0, -1.0, 0.0, 5.0, 7.0, 10.23]
)
def test_YawAngles(expected_yaw) -> None:
    targetPose = Pose3d(
        Translation3d(15.98, 0.0, 1.0), Rotation3d(0.0, 0.0, 3.0 * math.pi / 4.0)
    )

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)

    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))
    cameraSim.setMinTargetAreaPixels(0.0)
    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=0.5, height=0.5), 4774
            )
        ]
    )

    robotPose = Pose2d(Translation2d(10.0, 0.0), Rotation2d.fromDegrees(expected_yaw))
    visionSysSim.update(robotPose)

    result = camera.getLatestResult()

    bestTarget = result.getBestTarget()
    assert bestTarget is not None
    assert bestTarget.getYaw() == pytest.approx(expected_yaw, abs=0.25)


@pytest.mark.parametrize(
    "expected_pitch", [-10.0, -5.0, -2.0, -1.0, 0.0, 5.0, 7.0, 10.23]
)
def test_PitchAngles(expected_pitch) -> None:
    targetPose = Pose3d(
        Translation3d(15.98, 0.0, 0.0), Rotation3d(0, 0, 3.0 * math.pi / 4.0)
    )
    robotPose = Pose2d(
        Translation2d(10.0, 0.0), Rotation2d.fromDegrees(-expected_pitch)
    )
    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(
        640, 480, fovDiag=Rotation2d.fromDegrees(120.0)
    )
    cameraSim.setMinTargetAreaPixels(0.0)
    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=0.5, height=0.5), 4774
            )
        ]
    )
    visionSysSim.adjustCamera(
        cameraSim,
        Transform3d(
            Translation3d(), Rotation3d(0.0, math.radians(expected_pitch), 0.0)
        ),
    )
    visionSysSim.update(robotPose)

    result = camera.getLatestResult()

    bestTarget = result.getBestTarget()
    assert bestTarget is not None
    assert bestTarget.getPitch() == pytest.approx(expected_pitch, abs=0.25)


@pytest.mark.parametrize(
    "distParam, pitchParam, heightParam",
    [
        (5, -15.98, 0),
        (6, -15.98, 1),
        (10, -15.98, 0),
        (15, -15.98, 2),
        (19.95, -15.98, 0),
        (20, -15.98, 0),
        (5, -42, 1),
        (6, -42, 0),
        (10, -42, 2),
        (15, -42, 0.5),
        (19.42, -15.98, 0),
        (20, -42, 0),
        (5, -55, 2),
        (6, -55, 0),
        (10, -54, 2.2),
        (15, -53, 0),
        (19.52, -15.98, 1.1),
    ],
)
def test_distanceCalc(distParam, pitchParam, heightParam) -> None:
    distParam = feetToMeters(distParam)
    pitchParam = math.radians(pitchParam)
    heightParam = feetToMeters(heightParam)

    targetPose = Pose3d(
        Translation3d(15.98, 0.0, 1.0), Rotation3d(0.0, 0.0, 0.98 * math.pi)
    )
    robotPose = Pose3d(Translation3d(15.98 - distParam, 0.0, 0.0), Rotation3d())
    robotToCamera = Transform3d(
        Translation3d(0.0, 0.0, heightParam), Rotation3d(0.0, pitchParam, 0.0)
    )

    visionSysSim = VisionSystemSim(
        "absurdlylongnamewhichshouldneveractuallyhappenbuteehwelltestitanywaysohowsyourdaygoingihopegoodhaveagreatrestofyourlife"
    )
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(
        640, 480, fovDiag=Rotation2d.fromDegrees(160.0)
    )
    cameraSim.setMinTargetAreaPixels(0.0)
    visionSysSim.adjustCamera(cameraSim, robotToCamera)
    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPose, TargetModel.createPlanar(width=0.5, height=0.5), 4774
            )
        ]
    )
    visionSysSim.update(robotPose)

    result = camera.getLatestResult()

    target = result.getBestTarget()
    assert target is not None
    assert target.getYaw() == pytest.approx(0.0, abs=0.5)

    # TODO Enable when PhotonUtils is ported
    # dist = PhotonUtils.calculateDistanceToTarget(
    #    robotToCamera.Z(), targetPose.Z(), -pitchParam, math.degrees(target.getPitch())
    # )
    # assert dist == pytest.approx(distParam, abs=0.25)


def test_MultipleTargets() -> None:
    targetPoseL = Pose3d(Translation3d(15.98, 2.0, 0.0), Rotation3d(0.0, 0.0, math.pi))
    targetPoseC = Pose3d(Translation3d(15.98, 0.0, 0.0), Rotation3d(0.0, 0.0, math.pi))
    targetPoseR = Pose3d(Translation3d(15.98, -2.0, 0.0), Rotation3d(0.0, 0.0, math.pi))

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))
    cameraSim.setMinTargetAreaPixels(20.0)

    visionSysSim.addVisionTargets(
        [
            VisionTargetSim(
                targetPoseL.transformBy(
                    Transform3d(Translation3d(0, 0, 0), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                1,
            ),
            VisionTargetSim(
                targetPoseC.transformBy(
                    Transform3d(Translation3d(0, 0, 0), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                2,
            ),
            VisionTargetSim(
                targetPoseR.transformBy(
                    Transform3d(Translation3d(0, 0, 0), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                3,
            ),
            VisionTargetSim(
                targetPoseL.transformBy(
                    Transform3d(Translation3d(0, 0, 1), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                4,
            ),
            VisionTargetSim(
                targetPoseC.transformBy(
                    Transform3d(Translation3d(0, 0, 1), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                5,
            ),
            VisionTargetSim(
                targetPoseR.transformBy(
                    Transform3d(Translation3d(0, 0, 1), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                6,
            ),
            VisionTargetSim(
                targetPoseL.transformBy(
                    Transform3d(Translation3d(0, 0, 0.5), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                7,
            ),
            VisionTargetSim(
                targetPoseC.transformBy(
                    Transform3d(Translation3d(0, 0, 0.5), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                8,
            ),
            VisionTargetSim(
                targetPoseL.transformBy(
                    Transform3d(Translation3d(0, 0, 0.75), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                9,
            ),
            VisionTargetSim(
                targetPoseR.transformBy(
                    Transform3d(Translation3d(0, 0, 0.75), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                10,
            ),
            VisionTargetSim(
                targetPoseL.transformBy(
                    Transform3d(Translation3d(0, 0, 0.25), Rotation3d())
                ),
                TargetModel.AprilTag16h5(),
                11,
            ),
        ]
    )
    robotPose = Pose2d(Translation2d(6.0, 0.0), Rotation2d.fromDegrees(0.25))
    visionSysSim.update(robotPose)
    res = camera.getLatestResult()
    assert res.hasTargets()
    tgtList = res.getTargets()
    assert len(tgtList) == 11


def test_PoseEstimation() -> None:
    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(90.0))
    cameraSim.setMinTargetAreaPixels(20.0)

    tagList: list[AprilTag] = []
    at0 = AprilTag()
    at0.ID = 0
    at0.pose = Pose3d(12.0, 3.0, 1.0, Rotation3d(0.0, 0.0, math.pi))
    tagList.append(at0)
    at1 = AprilTag()
    at1.ID = 1
    at1.pose = Pose3d(12.0, 1.0, -1.0, Rotation3d(0.0, 0.0, math.pi))
    tagList.append(at1)
    at2 = AprilTag()
    at2.ID = 2
    at2.pose = Pose3d(11.0, 0.0, 2.0, Rotation3d(0.0, 0.0, math.pi))
    tagList.append(at2)

    fieldLength: meters = 54.0
    fieldWidth: meters = 27.0
    layout = AprilTagFieldLayout(tagList, fieldLength, fieldWidth)
    robotPose = Pose2d(Translation2d(5.0, 1.0), Rotation2d.fromDegrees(5.0))
    visionSysSim.addVisionTargets(
        [VisionTargetSim(tagList[0].pose, TargetModel.AprilTag16h5(), 0)]
    )

    visionSysSim.update(robotPose)

    camEigen = cameraSim.prop.getIntrinsics()
    distEigen = cameraSim.prop.getDistCoeffs()

    camResults = camera.getLatestResult()
    targets = camResults.getTargets()
    results = VisionEstimation.estimateCamPosePNP(
        camEigen, distEigen, targets, layout, TargetModel.AprilTag16h5()
    )
    assert results is not None
    pose: Pose3d = Pose3d() + results.best
    assert pose.X() == pytest.approx(5.0, abs=0.01)
    assert pose.Y() == pytest.approx(1.0, abs=0.01)
    assert pose.Z() == pytest.approx(0.0, abs=0.01)
    assert pose.rotation().Z() == pytest.approx(math.radians(5.0), abs=0.01)

    visionSysSim.addVisionTargets(
        [VisionTargetSim(tagList[1].pose, TargetModel.AprilTag16h5(), 1)]
    )
    visionSysSim.addVisionTargets(
        [VisionTargetSim(tagList[2].pose, TargetModel.AprilTag16h5(), 2)]
    )
    visionSysSim.update(robotPose)

    camResults2 = camera.getLatestResult()
    targets2 = camResults2.getTargets()
    results2 = VisionEstimation.estimateCamPosePNP(
        camEigen, distEigen, targets2, layout, TargetModel.AprilTag16h5()
    )
    assert results2 is not None
    pose2 = Pose3d() + results2.best

    assert pose2.X() == pytest.approx(robotPose.X(), abs=0.01)
    assert pose2.Y() == pytest.approx(robotPose.Y(), abs=0.01)
    assert pose2.Z() == pytest.approx(0.0, abs=0.01)
    assert pose2.rotation().Z() == pytest.approx(math.radians(5.0), abs=0.01)


def test_PoseEstimationRotated() -> None:
    robotToCamera = Transform3d(
        Translation3d(6.0 * 0.0254, 6.0 * 0.0254, 6.0 * 0.0254),
        Rotation3d(0.0, math.radians(-30.0), math.radians(25.5)),
    )

    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, robotToCamera)

    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(90.0))
    cameraSim.setMinTargetAreaPixels(20.0)

    tagList: list[AprilTag] = []
    at0 = AprilTag()
    at0.ID = 0
    at0.pose = Pose3d(12.0, 3.0, 1.0, Rotation3d(0.0, 0.0, math.pi))
    tagList.append(at0)
    at1 = AprilTag()
    at1.ID = 1
    at1.pose = Pose3d(12.0, 1.0, -1.0, Rotation3d(0.0, 0.0, math.pi))
    tagList.append(at1)
    at2 = AprilTag()
    at2.ID = 2
    at2.pose = Pose3d(11.0, 0.0, 2.0, Rotation3d(0.0, 0.0, math.pi))
    tagList.append(at2)

    fieldLength: meters = 54.0
    fieldWidth: meters = 27.0
    layout = AprilTagFieldLayout(tagList, fieldLength, fieldWidth)
    robotPose = Pose2d(Translation2d(5.0, 1.0), Rotation2d.fromDegrees(-5.0))
    visionSysSim.addVisionTargets(
        [VisionTargetSim(tagList[0].pose, TargetModel.AprilTag36h11(), 0)]
    )

    visionSysSim.update(robotPose)

    camEigen = cameraSim.prop.getIntrinsics()
    distEigen = cameraSim.prop.getDistCoeffs()

    camResults = camera.getLatestResult()
    targets = camResults.getTargets()
    results = VisionEstimation.estimateCamPosePNP(
        camEigen, distEigen, targets, layout, TargetModel.AprilTag36h11()
    )
    assert results is not None
    pose: Pose3d = Pose3d() + results.best
    pose = pose.transformBy(robotToCamera.inverse())
    assert pose.X() == pytest.approx(5.0, abs=0.01)
    assert pose.Y() == pytest.approx(1.0, abs=0.01)
    assert pose.Z() == pytest.approx(0.0, abs=0.01)
    assert pose.rotation().Z() == pytest.approx(math.radians(-5.0), abs=0.01)

    visionSysSim.addVisionTargets(
        [VisionTargetSim(tagList[1].pose, TargetModel.AprilTag36h11(), 1)]
    )
    visionSysSim.addVisionTargets(
        [VisionTargetSim(tagList[2].pose, TargetModel.AprilTag36h11(), 2)]
    )
    visionSysSim.update(robotPose)

    camResults2 = camera.getLatestResult()
    targets2 = camResults2.getTargets()
    results2 = VisionEstimation.estimateCamPosePNP(
        camEigen, distEigen, targets2, layout, TargetModel.AprilTag36h11()
    )
    assert results2 is not None
    pose2 = Pose3d() + results2.best
    pose2 = pose2.transformBy(robotToCamera.inverse())

    assert pose2.X() == pytest.approx(robotPose.X(), abs=0.01)
    assert pose2.Y() == pytest.approx(robotPose.Y(), abs=0.01)
    assert pose2.Z() == pytest.approx(0.0, abs=0.01)
    assert pose2.rotation().Z() == pytest.approx(math.radians(-5.0), abs=0.01)


def test_TagAmbiguity() -> None:
    visionSysSim = VisionSystemSim("Test")
    camera = PhotonCamera("camera")
    cameraSim = PhotonCameraSim(camera)
    visionSysSim.addCamera(cameraSim, Transform3d())
    cameraSim.prop.setCalibrationFromFOV(640, 480, fovDiag=Rotation2d.fromDegrees(80.0))
    cameraSim.setMinTargetAreaPixels(20.0)

    targetPose = Pose3d(Translation3d(2.0, 0.0, 0.0), Rotation3d(0, 0, math.pi))
    visionSysSim.addVisionTargets(
        [VisionTargetSim(targetPose, TargetModel.AprilTag36h11(), 3)]
    )

    robotPose = Pose2d()
    visionSysSim.update(robotPose)
    tgt = camera.getLatestResult().getBestTarget()
    assert tgt is not None
    ambiguity = tgt.getPoseAmbiguity()
    assert ambiguity > 0.5, "Tag ambiguity expected to be high"

    robotPose = Pose2d(Translation2d(-2.0, -2.0), Rotation2d.fromDegrees(30.0))
    visionSysSim.update(robotPose)
    tgt = camera.getLatestResult().getBestTarget()
    assert tgt is not None
    ambiguity = tgt.getPoseAmbiguity()
    assert 0 < ambiguity < 0.2, "Tag ambiguity expected to be high"
