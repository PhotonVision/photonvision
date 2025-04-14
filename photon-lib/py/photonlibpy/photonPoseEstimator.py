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

import enum
from typing import Optional

import wpilib
from robotpy_apriltag import AprilTagFieldLayout
from wpimath.geometry import Pose2d, Pose3d, Transform3d

from .estimatedRobotPose import EstimatedRobotPose
from .photonCamera import PhotonCamera
from .targeting.photonPipelineResult import PhotonPipelineResult


class PoseStrategy(enum.Enum):
    """
    Position estimation strategies that can be used by the PhotonPoseEstimator class.
    """

    LOWEST_AMBIGUITY = enum.auto()
    """Choose the Pose with the lowest ambiguity."""

    CLOSEST_TO_CAMERA_HEIGHT = enum.auto()
    """Choose the Pose which is closest to the camera height."""

    CLOSEST_TO_REFERENCE_POSE = enum.auto()
    """Choose the Pose which is closest to a set Reference position."""

    CLOSEST_TO_LAST_POSE = enum.auto()
    """Choose the Pose which is closest to the last pose calculated."""

    AVERAGE_BEST_TARGETS = enum.auto()
    """Return the average of the best target poses using ambiguity as weight."""

    MULTI_TAG_PNP_ON_COPROCESSOR = enum.auto()
    """
    Use all visible tags to compute a single pose estimate on coprocessor.
    This option needs to be enabled on the PhotonVision web UI as well.
    """

    MULTI_TAG_PNP_ON_RIO = enum.auto()
    """
    Use all visible tags to compute a single pose estimate.
    This runs on the RoboRIO, and can take a lot of time.
    """


class PhotonPoseEstimator:
    """
    The PhotonPoseEstimator class filters or combines readings from all the AprilTags visible at a
    given timestamp on the field to produce a single robot in field pose, using the strategy set
    below. Example usage can be found in our apriltagExample example project.
    """

    def __init__(
        self,
        fieldTags: AprilTagFieldLayout,
        strategy: PoseStrategy,
        camera: PhotonCamera,
        robotToCamera: Transform3d,
    ):
        """Create a new PhotonPoseEstimator.

        :param fieldTags: A WPILib AprilTagFieldLayout linking AprilTag IDs to Pose3d objects
                           with respect to the FIRST field using the Field Coordinate System.
                           Note that setting the origin of this layout object will affect the
                           results from this class.
        :param strategy: The strategy it should use to determine the best pose.
        :param camera: PhotonCamera
        :param robotToCamera: Transform3d from the center of the robot to the camera mount position (i.e.,
                                robot ➔ camera) in the Robot Coordinate System.
        """
        self._fieldTags = fieldTags
        self._primaryStrategy = strategy
        self._camera = camera
        self.robotToCamera = robotToCamera

        self._multiTagFallbackStrategy = PoseStrategy.LOWEST_AMBIGUITY
        self._reportedErrors: set[int] = set()
        self._poseCacheTimestampSeconds = -1.0
        self._lastPose: Optional[Pose3d] = None
        self._referencePose: Optional[Pose3d] = None

        # TODO: Implement HAL reporting

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
        self._checkUpdate(self._fieldTags, fieldTags)
        self._fieldTags = fieldTags

    @property
    def primaryStrategy(self) -> PoseStrategy:
        """Get the Position Estimation Strategy being used by the Position Estimator.

        :returns: the strategy
        """
        return self._primaryStrategy

    @primaryStrategy.setter
    def primaryStrategy(self, strategy: PoseStrategy):
        """Set the Position Estimation Strategy used by the Position Estimator.

        :param strategy: the strategy to set
        """
        self._checkUpdate(self._primaryStrategy, strategy)
        self._primaryStrategy = strategy

    @property
    def multiTagFallbackStrategy(self) -> PoseStrategy:
        return self._multiTagFallbackStrategy

    @multiTagFallbackStrategy.setter
    def multiTagFallbackStrategy(self, strategy: PoseStrategy):
        """Set the Position Estimation Strategy used in multi-tag mode when only one tag can be seen. Must
        NOT be MULTI_TAG_PNP

        :param strategy: the strategy to set
        """
        self._checkUpdate(self._multiTagFallbackStrategy, strategy)
        if (
            strategy is PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR
            or strategy is PoseStrategy.MULTI_TAG_PNP_ON_RIO
        ):
            wpilib.reportWarning(
                "Fallback cannot be set to MULTI_TAG_PNP! Setting to lowest ambiguity",
                False,
            )
            strategy = PoseStrategy.LOWEST_AMBIGUITY
        self._multiTagFallbackStrategy = strategy

    @property
    def referencePose(self) -> Optional[Pose3d]:
        """Return the reference position that is being used by the estimator.

        :returns: the referencePose
        """
        return self._referencePose

    @referencePose.setter
    def referencePose(self, referencePose: Pose3d | Pose2d):
        """Update the stored reference pose for use when using the **CLOSEST_TO_REFERENCE_POSE**
        strategy.

        :param referencePose: the referencePose to set
        """
        if isinstance(referencePose, Pose2d):
            referencePose = Pose3d(referencePose)
        self._checkUpdate(self._referencePose, referencePose)
        self._referencePose = referencePose

    @property
    def lastPose(self) -> Optional[Pose3d]:
        return self._lastPose

    @lastPose.setter
    def lastPose(self, lastPose: Pose3d | Pose2d):
        """Update the stored last pose. Useful for setting the initial estimate when using the
        **CLOSEST_TO_LAST_POSE** strategy.

        :param lastPose: the lastPose to set
        """
        if isinstance(lastPose, Pose2d):
            lastPose = Pose3d(lastPose)
        self._checkUpdate(self._lastPose, lastPose)
        self._lastPose = lastPose

    def _invalidatePoseCache(self) -> None:
        self._poseCacheTimestampSeconds = -1.0

    def _checkUpdate(self, oldObj, newObj) -> None:
        if oldObj != newObj and oldObj is not None and oldObj is not newObj:
            self._invalidatePoseCache()

    def update(
        self, cameraResult: Optional[PhotonPipelineResult] = None
    ) -> Optional[EstimatedRobotPose]:
        """
        Updates the estimated position of the robot. Returns empty if:

         - The timestamp of the provided pipeline result is the same as in the previous call to
         ``update()``.

         - No targets were found in the pipeline results.

        :param cameraResult: The latest pipeline result from the camera

        :returns: an :class:`EstimatedRobotPose` with an estimated pose, timestamp, and targets used to
                   create the estimate.
        """
        if not cameraResult:
            if not self._camera:
                wpilib.reportError("[PhotonPoseEstimator] Missing camera!", False)
                return None
            cameraResult = self._camera.getLatestResult()

        if cameraResult.getTimestampSeconds() < 0:
            return None

        # If the pose cache timestamp was set, and the result is from the same
        # timestamp, return an
        # empty result
        if (
            self._poseCacheTimestampSeconds > 0.0
            and abs(
                self._poseCacheTimestampSeconds - cameraResult.getTimestampSeconds()
            )
            < 1e-6
        ):
            return None

        # Remember the timestamp of the current result used
        self._poseCacheTimestampSeconds = cameraResult.getTimestampSeconds()

        # If no targets seen, trivial case -- return empty result
        if not cameraResult.targets:
            return None

        return self._update(cameraResult, self._primaryStrategy)

    def _update(
        self, cameraResult: PhotonPipelineResult, strat: PoseStrategy
    ) -> Optional[EstimatedRobotPose]:
        if strat is PoseStrategy.LOWEST_AMBIGUITY:
            estimatedPose = self._lowestAmbiguityStrategy(cameraResult)
        elif strat is PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR:
            estimatedPose = self._multiTagOnCoprocStrategy(cameraResult)
        else:
            wpilib.reportError(
                "[PhotonPoseEstimator] Unknown Position Estimation Strategy!", False
            )
            return None

        if not estimatedPose:
            self._lastPose = None

        return estimatedPose

    def _multiTagOnCoprocStrategy(
        self, result: PhotonPipelineResult
    ) -> Optional[EstimatedRobotPose]:
        if result.multitagResult is not None:
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
                PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
            )
        else:
            return self._update(result, self._multiTagFallbackStrategy)

    def _lowestAmbiguityStrategy(
        self, result: PhotonPipelineResult
    ) -> Optional[EstimatedRobotPose]:
        """
        Return the estimated position of the robot with the lowest position ambiguity from a List of
        pipeline results.

        :param result: pipeline result

        :returns: the estimated position of the robot in the FCS and the estimated timestamp of this
                  estimation.
        """
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
            PoseStrategy.LOWEST_AMBIGUITY,
        )

    def _reportFiducialPoseError(self, fiducialId: int) -> None:
        if fiducialId not in self._reportedErrors:
            wpilib.reportError(
                f"[PhotonPoseEstimator] Tried to get pose of unknown AprilTag: {fiducialId}",
                False,
            )
            self._reportedErrors.add(fiducialId)
