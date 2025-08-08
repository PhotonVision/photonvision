###############################################################################
## Copyright (C) Photon Vision.
###############################################################################
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <https://www.gnu.org/licenses/>.
###############################################################################

from test import testUtil

import wpimath.units
from photonlibpy import PhotonCamera, PhotonPoseEstimator, PoseStrategy
from photonlibpy.estimation import TargetModel
from photonlibpy.simulation import PhotonCameraSim, SimCameraProperties, VisionTargetSim
from photonlibpy.targeting import (
    PhotonPipelineMetadata,
    PhotonTrackedTarget,
    TargetCorner,
)
from photonlibpy.targeting.multiTargetPNPResult import MultiTargetPNPResult, PnpResult
from photonlibpy.targeting.photonPipelineResult import PhotonPipelineResult
from robotpy_apriltag import AprilTag, AprilTagFieldLayout
from wpimath.geometry import Pose3d, Rotation3d, Transform3d, Translation3d


class PhotonCameraInjector(PhotonCamera):
    result: PhotonPipelineResult

    def __init__(self, cameraName="camera"):
        super().__init__(cameraName)

    def getLatestResult(self) -> PhotonPipelineResult:
        return self.result


def fakeAprilTagFieldLayout() -> AprilTagFieldLayout:
    tagList = []
    tagPoses = (
        Pose3d(3, 3, 3, Rotation3d()),
        Pose3d(5, 5, 5, Rotation3d()),
    )
    for id_, pose in enumerate(tagPoses):
        aprilTag = AprilTag()
        aprilTag.ID = id_
        aprilTag.pose = pose
        tagList.append(aprilTag)

    fieldLength = 54 / 3.281  # 54 ft -> meters
    fieldWidth = 27 / 3.281  # 24 ft -> meters

    return AprilTagFieldLayout(tagList, fieldLength, fieldWidth)


def test_lowestAmbiguityStrategy():
    aprilTags = fakeAprilTagFieldLayout()
    cameraOne = PhotonCameraInjector()
    cameraOne.result = PhotonPipelineResult(
        int(11 * 1e6),
        [
            PhotonTrackedTarget(
                3.0,
                -4.0,
                9.0,
                4.0,
                0,
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                0.7,
            ),
            PhotonTrackedTarget(
                3.0,
                -4.0,
                9.1,
                6.7,
                1,
                Transform3d(Translation3d(4, 2, 3), Rotation3d(0, 0, 0)),
                Transform3d(Translation3d(4, 2, 3), Rotation3d(1, 5, 3)),
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                0.3,
            ),
            PhotonTrackedTarget(
                9.0,
                -2.0,
                19.0,
                3.0,
                0,
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                0.4,
            ),
        ],
        metadata=PhotonPipelineMetadata(0, int(2 * 1e3), 0),
        multitagResult=None,
    )

    estimator = PhotonPoseEstimator(
        aprilTags, PoseStrategy.LOWEST_AMBIGUITY, cameraOne, Transform3d()
    )

    estimatedPose = estimator.update()

    assert estimatedPose is not None

    pose = estimatedPose.estimatedPose

    assertEquals(11 - 0.002, estimatedPose.timestampSeconds, 1e-3)
    assertEquals(1, pose.x, 0.01)
    assertEquals(3, pose.y, 0.01)
    assertEquals(2, pose.z, 0.01)


def test_pnpDistanceTrigSolve():
    aprilTags = fakeAprilTagFieldLayout()
    cameraOne = PhotonCameraInjector()
    latencySecs: wpimath.units.seconds = 1
    fakeTimestampSecs: wpimath.units.seconds = 9 + latencySecs

    cameraOneSim = PhotonCameraSim(cameraOne, SimCameraProperties.PERFECT_90DEG())
    simTargets = [
        VisionTargetSim(tag.pose, TargetModel.AprilTag36h11(), tag.ID)
        for tag in aprilTags.getTags()
    ]

    # Compound Rolled + Pitched + Yaw
    compoundTestTransform = Transform3d(
        -wpimath.units.inchesToMeters(12),
        -wpimath.units.inchesToMeters(11),
        3,
        Rotation3d(
            wpimath.units.degreesToRadians(37),
            wpimath.units.degreesToRadians(6),
            wpimath.units.degreesToRadians(60),
        ),
    )

    estimator = PhotonPoseEstimator(
        aprilTags,
        PoseStrategy.PNP_DISTANCE_TRIG_SOLVE,
        cameraOne,
        compoundTestTransform,
    )

    realPose = Pose3d(7.3, 4.42, 0, Rotation3d(0, 0, 2.197))  # Pose to compare with
    result = cameraOneSim.process(
        latencySecs, realPose.transformBy(estimator.robotToCamera), simTargets
    )
    bestTarget = result.getBestTarget()
    assert bestTarget is not None
    assert bestTarget.fiducialId == 0
    assert result.ntReceiveTimestampMicros > 0
    # Make test independent of the FPGA time.
    result.ntReceiveTimestampMicros = int(fakeTimestampSecs * 1e6)

    estimator.addHeadingData(
        result.getTimestampSeconds(), realPose.rotation().toRotation2d()
    )
    estimatedRobotPose = estimator.update(result)

    assert estimatedRobotPose is not None
    pose = estimatedRobotPose.estimatedPose
    assertEquals(realPose.x, pose.x, 0.01)
    assertEquals(realPose.y, pose.y, 0.01)
    assertEquals(0.0, pose.z, 0.01)

    # Straight on
    fakeTimestampSecs += 60
    straightOnTestTransform = Transform3d(0, 0, 3, Rotation3d())
    estimator.robotToCamera = straightOnTestTransform
    realPose = Pose3d(4.81, 2.38, 0, Rotation3d(0, 0, 2.818))  # Pose to compare with
    result = cameraOneSim.process(
        latencySecs, realPose.transformBy(estimator.robotToCamera), simTargets
    )
    bestTarget = result.getBestTarget()
    assert bestTarget is not None
    assert bestTarget.fiducialId == 0
    assert result.ntReceiveTimestampMicros > 0
    # Make test independent of the FPGA time.
    result.ntReceiveTimestampMicros = int(fakeTimestampSecs * 1e6)

    estimator.addHeadingData(
        result.getTimestampSeconds(), realPose.rotation().toRotation2d()
    )
    estimatedRobotPose = estimator.update(result)

    assert estimatedRobotPose is not None
    pose = estimatedRobotPose.estimatedPose
    assertEquals(realPose.x, pose.x, 0.01)
    assertEquals(realPose.y, pose.y, 0.01)
    assertEquals(0.0, pose.z, 0.01)


def test_multiTagOnCoprocStrategy():
    cameraOne = PhotonCameraInjector()
    cameraOne.result = PhotonPipelineResult(
        int(11 * 1e6),
        # There needs to be at least one target present for pose estimation to work
        # Doesn't matter which/how many targets for this test
        [
            PhotonTrackedTarget(
                3.0,
                -4.0,
                9.0,
                4.0,
                0,
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                0.7,
            )
        ],
        metadata=PhotonPipelineMetadata(0, int(2 * 1e3), 0),
        multitagResult=MultiTargetPNPResult(
            PnpResult(Transform3d(1, 3, 2, Rotation3d()))
        ),
    )

    estimator = PhotonPoseEstimator(
        AprilTagFieldLayout(),
        PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
        cameraOne,
        Transform3d(),
    )

    estimatedPose = estimator.update()

    assert estimatedPose is not None

    pose = estimatedPose.estimatedPose

    assertEquals(11 - 2e-3, estimatedPose.timestampSeconds, 1e-3)
    assertEquals(1, pose.x, 0.01)
    assertEquals(3, pose.y, 0.01)
    assertEquals(2, pose.z, 0.01)


def test_cacheIsInvalidated():
    aprilTags = fakeAprilTagFieldLayout()
    cameraOne = PhotonCameraInjector()

    estimator = PhotonPoseEstimator(
        aprilTags, PoseStrategy.LOWEST_AMBIGUITY, cameraOne, Transform3d()
    )

    # Initial state, expect no timestamp.
    assertEquals(-1, estimator._poseCacheTimestampSeconds)

    # First result is 17s after epoch start.
    timestamps = testUtil.PipelineTimestamps(captureTimestampMicros=17_000_000)
    latencySecs = timestamps.pipelineLatencySecs()

    # No targets, expect empty result
    cameraOne.result = PhotonPipelineResult(
        timestamps.receiveTimestampMicros(),
        metadata=timestamps.toPhotonPipelineMetadata(),
    )
    estimatedPose = estimator.update()

    assert estimatedPose is None
    assertEquals(
        timestamps.receiveTimestampMicros() * 1e-6 - latencySecs,
        estimator._poseCacheTimestampSeconds,
        1e-3,
    )

    # Set actual result
    timestamps.incrementTimeMicros(2_500_000)
    result = PhotonPipelineResult(
        timestamps.receiveTimestampMicros(),
        [
            PhotonTrackedTarget(
                3.0,
                -4.0,
                9.0,
                4.0,
                0,
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                Transform3d(Translation3d(1, 2, 3), Rotation3d(1, 2, 3)),
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                [
                    TargetCorner(1, 2),
                    TargetCorner(3, 4),
                    TargetCorner(5, 6),
                    TargetCorner(7, 8),
                ],
                0.7,
            )
        ],
        metadata=timestamps.toPhotonPipelineMetadata(),
    )
    cameraOne.result = result
    estimatedPose = estimator.update()
    assert estimatedPose is not None
    expectedTimestamp = timestamps.receiveTimestampMicros() * 1e-6 - latencySecs
    assertEquals(expectedTimestamp, estimatedPose.timestampSeconds, 1e-3)
    assertEquals(expectedTimestamp, estimator._poseCacheTimestampSeconds, 1e-3)

    # And again -- pose cache should mean this is empty
    cameraOne.result = result
    estimatedPose = estimator.update()
    assert estimatedPose is None
    # Expect the old timestamp to still be here
    assertEquals(expectedTimestamp, estimator._poseCacheTimestampSeconds, 1e-3)

    # Set new field layout -- right after, the pose cache timestamp should be -1
    estimator.fieldTags = AprilTagFieldLayout([AprilTag()], 0, 0)
    assertEquals(-1, estimator._poseCacheTimestampSeconds)
    # Update should cache the current timestamp (20) again
    cameraOne.result = result
    estimatedPose = estimator.update()

    assert estimatedPose is not None

    assertEquals(expectedTimestamp, estimatedPose.timestampSeconds, 1e-3)
    assertEquals(expectedTimestamp, estimator._poseCacheTimestampSeconds, 1e-3)

    # Setting a value from None to a non-None should invalidate the cache.
    assert estimator.referencePose is None
    estimator.referencePose = Pose3d(3, 3, 3, Rotation3d())

    assertEquals(-1, estimator._poseCacheTimestampSeconds)


def assertEquals(expected, actual, epsilon=0.0):
    assert abs(expected - actual) <= epsilon
