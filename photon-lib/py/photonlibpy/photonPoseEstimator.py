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

from typing import Optional

import hal
import wpilib
import wpimath.units
from robotpy_apriltag import AprilTagFieldLayout
from wpimath.geometry import (
    Pose2d,
    Pose3d,
    Rotation2d,
    Rotation3d,
    Transform3d,
    Translation2d,
    Translation3d,
)
from wpimath.interpolation import TimeInterpolatableRotation2dBuffer

from .estimatedRobotPose import EstimatedRobotPose
from .targeting.photonPipelineResult import PhotonPipelineResult


class PhotonPoseEstimator:
    instance_count = 1

    """
    The PhotonPoseEstimator class filters or combines readings from all the AprilTags visible at a
    given timestamp on the field to produce a single robot in field pose, using the strategy set
    below. Example usage can be found in our apriltagExample example project.
    """

    def __init__(
        self,
        fieldTags: AprilTagFieldLayout,
        robotToCamera: Transform3d,
    ):
        """Create a new PhotonPoseEstimator.

        :param fieldTags: A WPILib AprilTagFieldLayout linking AprilTag IDs to Pose3d objects
                           with respect to the FIRST field using the Field Coordinate System.
                           Note that setting the origin of this layout object will affect the
                           results from this class.
        :param robotToCamera: Transform3d from the center of the robot to the camera mount position (i.e.,
                                robot ➔ camera) in the Robot Coordinate System.
        """
        self._fieldTags = fieldTags
        self.robotToCamera = robotToCamera

        self._reportedErrors: set[int] = set()
        self._headingBuffer = TimeInterpolatableRotation2dBuffer(1)

        # Usage reporting
        hal.report(
            hal.tResourceType.kResourceType_PhotonPoseEstimator.value,
            PhotonPoseEstimator.instance_count,
        )
        PhotonPoseEstimator.instance_count += 1

    @property
    def fieldTags(self) -> AprilTagFieldLayout:
        """Get the AprilTagFieldLayout being used by the PositionEstimator.

        Note: Setting the origin of this layout will affect the results from this class.

        :returns: the AprilTagFieldLayout
        """
        return self._fieldTags

    @fieldTags.setter
    def fieldTags(self, fieldTags: AprilTagFieldLayout):
        """Set the AprilTagFieldLayout being used by the PositionEstimator.

        Note: Setting the origin of this layout will affect the results from this class.

        :param fieldTags: the AprilTagFieldLayout
        """
        self._fieldTags = fieldTags

    def addHeadingData(
        self, timestampSeconds: wpimath.units.seconds, heading: Rotation2d | Rotation3d
    ) -> None:
        """
        Add robot heading data to buffer. Must be called periodically for the **PNP_DISTANCE_TRIG_SOLVE** strategy.

        :param timestampSeconds :timestamp of the robot heading data
        :param heading: field-relative robot heading at given timestamp
        """
        if isinstance(heading, Rotation3d):
            heading = heading.toRotation2d()
        self._headingBuffer.addSample(timestampSeconds, heading)

    def resetHeadingData(
        self, timestampSeconds: wpimath.units.seconds, heading: Rotation2d | Rotation3d
    ) -> None:
        """
        Clears all heading data in the buffer, and adds a new seed. Useful for preventing estimates
        from utilizing heading data provided prior to a pose or rotation reset.

        :param timestampSeconds: timestamp of the robot heading data
        :param  heading: field-relative robot heading at given timestamp
        """
        self._headingBuffer.clear()
        self.addHeadingData(timestampSeconds, heading)

    def _shouldEstimate(self, cameraResult: PhotonPipelineResult) -> bool:
        """
        :param cameraResult: A pipeline result from the camera.

        :returns: Whether or not estimation should be done.
        """
        if cameraResult.getTimestampSeconds() < 0:
            return False

        # If no targets seen, trivial case -- can't do estimation
        return len(cameraResult.targets) > 0

    def estimatePnpDistanceTrigSolvePose(
        self, result: PhotonPipelineResult
    ) -> Optional[EstimatedRobotPose]:
        """

        Return the estimated position of the robot by using distance data from best visible tag to
        compute a Pose. This runs on the RoboRIO in order to access the robot's yaw heading, and MUST
        have addHeadingData called every frame so heading data is up-to-date.

        Yields a Pose2d in estimatedRobotPose (0 for z, roll, pitch)

        https://www.chiefdelphi.com/t/frc-6328-mechanical-advantage-2025-build-thread/477314/98


        :param result: A pipeline result from the camera.
        :returns: An :class:`EstimatedRobotPose` with an estimated pose, timestamp, and targets used
          to create the estimate.
        """
        if (
            not self._shouldEstimate(result)
            or (bestTarget := result.getBestTarget()) is None
        ):
            return None

        if (
            headingSample := self._headingBuffer.sample(result.getTimestampSeconds())
        ) is None:
            return None

        if (tagPose := self._fieldTags.getTagPose(bestTarget.fiducialId)) is None:
            return None

        camToTagTranslation = (
            Translation3d(
                bestTarget.getBestCameraToTarget().translation().norm(),
                Rotation3d(
                    0,
                    -wpimath.units.degreesToRadians(bestTarget.getPitch()),
                    -wpimath.units.degreesToRadians(bestTarget.getYaw()),
                ),
            )
            .rotateBy(self.robotToCamera.rotation())
            .toTranslation2d()
            .rotateBy(headingSample)
        )

        fieldToCameraTranslation = (
            tagPose.toPose2d().translation() - camToTagTranslation
        )
        camToRobotTranslation: Translation2d = -(
            self.robotToCamera.translation().toTranslation2d()
        )
        camToRobotTranslation = camToRobotTranslation.rotateBy(headingSample)
        robotPose = Pose2d(
            fieldToCameraTranslation + camToRobotTranslation, headingSample
        )

        return EstimatedRobotPose(
            Pose3d(robotPose), result.getTimestampSeconds(), result.getTargets()
        )

    def estimateCoprocMultiTagPose(
        self, result: PhotonPipelineResult
    ) -> Optional[EstimatedRobotPose]:
        """
        Return the estimated position of the robot by using all visible tags to compute a single
        pose estimate on coprocessor. This option needs to be enabled on the PhotonVision web UI as
        well.

        :param result: A pipeline result from the camera.
        :returns: An :class:`EstimatedRobotPose` with an estimated pose, timestamp, and targets used
          to create the estimate.
        """
        if result.multitagResult is not None and self._shouldEstimate(result):
            best_tf = result.multitagResult.estimatedPose.best
            best = (
                Pose3d()
                .transformBy(best_tf)  # field-to-camera
                .relativeTo(self._fieldTags.getOrigin())
                .transformBy(self.robotToCamera.inverse())  # field-to-robot
            )
            return EstimatedRobotPose(
                best,
                result.getTimestampSeconds(),
                result.targets,
            )
        else:
            return None

    def estimateLowestAmbiguityPose(
        self, result: PhotonPipelineResult
    ) -> Optional[EstimatedRobotPose]:
        """
        Return the estimated position of the robot with the lowest position ambiguity from a pipeline results.

        :param result: A pipeline result from the camera.
        :returns: An :class:`EstimatedRobotPose` with an estimated pose, timestamp, and targets used
          to create the estimate.
        """
        if not self._shouldEstimate(result):
            return None
        lowestAmbiguityTarget = None

        lowestAmbiguityScore = 10.0
        for target in result.targets:
            targetPoseAmbiguity = target.poseAmbiguity

            # Make sure the target is a Fiducial target.
            if targetPoseAmbiguity != -1 and targetPoseAmbiguity < lowestAmbiguityScore:
                lowestAmbiguityScore = targetPoseAmbiguity
                lowestAmbiguityTarget = target

        # Although there are confirmed to be targets, none of them may be fiducial
        # targets.
        if not lowestAmbiguityTarget:
            return None

        targetFiducialId = lowestAmbiguityTarget.fiducialId

        targetPosition = self._fieldTags.getTagPose(targetFiducialId)

        if not targetPosition:
            self._reportFiducialPoseError(targetFiducialId)
            return None

        return EstimatedRobotPose(
            targetPosition.transformBy(
                lowestAmbiguityTarget.getBestCameraToTarget().inverse()
            ).transformBy(self.robotToCamera.inverse()),
            result.getTimestampSeconds(),
            result.targets,
        )

    def _reportFiducialPoseError(self, fiducialId: int) -> None:
        if fiducialId not in self._reportedErrors:
            wpilib.reportError(
                f"[PhotonPoseEstimator] Tried to get pose of unknown AprilTag: {fiducialId}",
                False,
            )
            self._reportedErrors.add(fiducialId)
