import typing

import wpilib
from robotpy_apriltag import AprilTagFieldLayout
from wpilib import Field2d
from wpimath.geometry import Pose2d, Pose3d, Transform3d

# TODO(auscompgeek): update import path when RobotPy re-exports are fixed
from wpimath.interpolation._interpolation import TimeInterpolatablePose3dBuffer
from wpimath.units import seconds

from ..estimation import TargetModel
from .photonCameraSim import PhotonCameraSim
from .visionTargetSim import VisionTargetSim


class VisionSystemSim:
    """A simulated vision system involving a camera(s) and coprocessor(s) mounted on a mobile robot
    running PhotonVision, detecting targets placed on the field. :class:`.VisionTargetSim`s added to
    this class will be detected by the :class:`.PhotonCameraSim`s added to this class. This class
    should be updated periodically with the robot's current pose in order to publish the simulated
    camera target info.
    """

    def __init__(self, visionSystemName: str):
        """A simulated vision system involving a camera(s) and coprocessor(s) mounted on a mobile robot
        running PhotonVision, detecting targets placed on the field. :class:`.VisionTargetSim`s added to
        this class will be detected by the :class:`.PhotonCameraSim`s added to this class. This class
        should be updated periodically with the robot's current pose in order to publish the simulated
        camera target info.

        :param visionSystemName: The specific identifier for this vision system in NetworkTables.
        """
        self.dbgField: Field2d = Field2d()
        self.bufferLength: seconds = 1.5

        self.camSimMap: typing.Dict[str, PhotonCameraSim] = {}
        self.camTrfMap: typing.Dict[PhotonCameraSim, TimeInterpolatablePose3dBuffer] = (
            {}
        )
        self.robotPoseBuffer: TimeInterpolatablePose3dBuffer = (
            TimeInterpolatablePose3dBuffer(self.bufferLength)
        )
        self.targetSets: typing.Dict[str, list[VisionTargetSim]] = {}

        self.tableName: str = "VisionSystemSim-" + visionSystemName
        wpilib.SmartDashboard.putData(self.tableName + "/Sim Field", self.dbgField)

    def getCameraSim(self, name: str) -> PhotonCameraSim | None:
        """Get one of the simulated cameras."""
        return self.camSimMap.get(name, None)

    def getCameraSims(self) -> list[PhotonCameraSim]:
        """Get all the simulated cameras."""
        return [*self.camSimMap.values()]

    def addCamera(self, cameraSim: PhotonCameraSim, robotToCamera: Transform3d) -> None:
        """Adds a simulated camera to this vision system with a specified robot-to-camera transformation.
        The vision targets registered with this vision system simulation will be observed by the
        simulated :class:`.PhotonCamera`.

        :param cameraSim:     The camera simulation
        :param robotToCamera: The transform from the robot pose to the camera pose
        """
        name = cameraSim.getCamera().getName()
        if name not in self.camSimMap:
            self.camSimMap[name] = cameraSim
            self.camTrfMap[cameraSim] = TimeInterpolatablePose3dBuffer(
                self.bufferLength
            )
            self.camTrfMap[cameraSim].addSample(
                wpilib.Timer.getFPGATimestamp(), Pose3d() + robotToCamera
            )

    def clearCameras(self) -> None:
        """Remove all simulated cameras from this vision system."""
        self.camSimMap.clear()
        self.camTrfMap.clear()

    def removeCamera(self, cameraSim: PhotonCameraSim) -> bool:
        """Remove a simulated camera from this vision system.

        :returns: If the camera was present and removed
        """
        name = cameraSim.getCamera().getName()
        if name in self.camSimMap:
            del self.camSimMap[name]
            return True
        else:
            return False

    def getRobotToCamera(
        self,
        cameraSim: PhotonCameraSim,
        time: seconds = wpilib.Timer.getFPGATimestamp(),
    ) -> Transform3d | None:
        """Get a simulated camera's position relative to the robot. If the requested camera is invalid, an
        empty optional is returned.

        :param cameraSim:   The specific camera to get the robot-to-camera transform of
        :param timeSeconds: Timestamp in seconds of when the transform should be observed

        :returns: The transform of this camera, or an empty optional if it is invalid
        """
        if cameraSim in self.camTrfMap:
            trfBuffer = self.camTrfMap[cameraSim]
            sample = trfBuffer.sample(time)
            if sample is None:
                return None
            else:
                return Transform3d(Pose3d(), sample)
        else:
            return None

    def getCameraPose(
        self,
        cameraSim: PhotonCameraSim,
        time: seconds = wpilib.Timer.getFPGATimestamp(),
    ) -> Pose3d | None:
        """Get a simulated camera's position on the field. If the requested camera is invalid, an empty
        optional is returned.

        :param cameraSim: The specific camera to get the field pose of

        :returns: The pose of this camera, or an empty optional if it is invalid
        """
        robotToCamera = self.getRobotToCamera(cameraSim, time)
        if robotToCamera is None:
            return None
        else:
            pose = self.getRobotPose(time)
            if pose:
                return pose + robotToCamera
            else:
                return None

    def adjustCamera(
        self, cameraSim: PhotonCameraSim, robotToCamera: Transform3d
    ) -> bool:
        """Adjust a camera's position relative to the robot. Use this if your camera is on a gimbal or
        turret or some other mobile platform.

        :param cameraSim:     The simulated camera to change the relative position of
        :param robotToCamera: New transform from the robot to the camera

        :returns: If the cameraSim was valid and transform was adjusted
        """
        if cameraSim in self.camTrfMap:
            self.camTrfMap[cameraSim].addSample(
                wpilib.Timer.getFPGATimestamp(), Pose3d() + robotToCamera
            )
            return True
        else:
            return False

    def resetCameraTransforms(self, cameraSim: PhotonCameraSim | None = None) -> None:
        """Reset the transform history for this camera to just the current transform."""
        now = wpilib.Timer.getFPGATimestamp()

        def resetSingleCamera(self, cameraSim: PhotonCameraSim) -> bool:
            if cameraSim in self.camTrfMap:
                trfBuffer = self.camTrfMap[cameraSim]
                lastTrf = Transform3d(Pose3d(), trfBuffer.sample(now))
                trfBuffer.clear()
                self.adjustCamera(cameraSim, lastTrf)
                return True
            else:
                return False

        if cameraSim is None:
            for camera in self.camTrfMap.keys():
                resetSingleCamera(self, camera)
        else:
            resetSingleCamera(self, cameraSim)

    def getVisionTargets(self, targetType: str | None = None) -> list[VisionTargetSim]:
        if targetType is None:
            all: list[VisionTargetSim] = []
            for targets in self.targetSets.values():
                for target in targets:
                    all.append(target)
            return all
        else:
            return self.targetSets[targetType]

    def addVisionTargets(
        self, targets: list[VisionTargetSim], targetType: str = "targets"
    ) -> None:
        """Adds targets on the field which your vision system is designed to detect. The {@link
        PhotonCamera}s simulated from this system will report the location of the camera relative to
        the subset of these targets which are visible from the given camera position.

        :param targets: Targets to add to the simulated field
        :param type:    Type of target (e.g. "cargo").
        """

        if targetType not in self.targetSets:
            self.targetSets[targetType] = targets
        else:
            self.targetSets[targetType] += targets

    def addAprilTags(self, layout: AprilTagFieldLayout) -> None:
        """Adds targets on the field which your vision system is designed to detect. The {@link
        PhotonCamera}s simulated from this system will report the location of the camera relative to
        the subset of these targets which are visible from the given camera position.

        The AprilTags from this layout will be added as vision targets under the type "apriltag".
        The poses added preserve the tag layout's current alliance origin. If the tag layout's alliance
        origin is changed, these added tags will have to be cleared and re-added.

        :param tagLayout: The field tag layout to get Apriltag poses and IDs from
        """
        targets: list[VisionTargetSim] = []
        for tag in layout.getTags():
            tag_pose = layout.getTagPose(tag.ID)
            # TODO this was done to make the python gods happy. Confirm that this is desired or if types dont matter
            assert tag_pose is not None
            targets.append(
                VisionTargetSim(tag_pose, TargetModel.AprilTag36h11(), tag.ID)
            )
        self.addVisionTargets(targets, "apriltag")

    def clearVisionTargets(self) -> None:
        self.targetSets.clear()

    def clearAprilTags(self) -> None:
        self.removeVisionTargetType("apriltag")

    def removeVisionTargetType(self, targetType: str) -> None:
        del self.targetSets[targetType]

    def removeVisionTargets(
        self, targets: list[VisionTargetSim]
    ) -> list[VisionTargetSim]:
        removedList: list[VisionTargetSim] = []
        for target in targets:
            for _, currentTargets in self.targetSets.items():
                if target in currentTargets:
                    removedList.append(target)
                    currentTargets.remove(target)
        return removedList

    def getRobotPose(
        self, timestamp: seconds = wpilib.Timer.getFPGATimestamp()
    ) -> Pose3d | None:
        """Get the robot pose in meters saved by the vision system at this timestamp.

        :param timestamp: Timestamp of the desired robot pose
        """

        return self.robotPoseBuffer.sample(timestamp)

    def resetRobotPose(self, robotPose: Pose2d | Pose3d) -> None:
        """Clears all previous robot poses and sets robotPose at current time."""
        if type(robotPose) is Pose2d:
            robotPose = Pose3d(robotPose)
        assert type(robotPose) is Pose3d

        self.robotPoseBuffer.clear()
        self.robotPoseBuffer.addSample(wpilib.Timer.getFPGATimestamp(), robotPose)

    def getDebugField(self) -> Field2d:
        return self.dbgField

    def update(self, robotPose: Pose2d | Pose3d) -> None:
        """Periodic update. Ensure this is called repeatedly-- camera performance is used to automatically
        determine if a new frame should be submitted.

        :param robotPoseMeters: The simulated robot pose in meters
        """
        if type(robotPose) is Pose2d:
            robotPose = Pose3d(robotPose)
        assert type(robotPose) is Pose3d

        # update vision targets on field
        for targetType, targets in self.targetSets.items():
            posesToAdd: list[Pose2d] = []
            for target in targets:
                posesToAdd.append(target.getPose().toPose2d())
            self.dbgField.getObject(targetType).setPoses(posesToAdd)

        # save "real" robot poses over time
        now = wpilib.Timer.getFPGATimestamp()
        self.robotPoseBuffer.addSample(now, robotPose)
        self.dbgField.setRobotPose(robotPose.toPose2d())

        allTargets: list[VisionTargetSim] = []
        for targets in self.targetSets.values():
            for target in targets:
                allTargets.append(target)

        visTgtPoses2d: list[Pose2d] = []
        cameraPoses2d: list[Pose2d] = []
        processed = False
        # process each camera
        for camSim in self.camSimMap.values():
            # check if this camera is ready to process and get latency
            optTimestamp = camSim.consumeNextEntryTime()
            if optTimestamp is None:
                continue
            else:
                processed = True

            # when this result "was" read by NT
            timestampNt = optTimestamp
            latency = camSim.prop.estLatency()
            # the image capture timestamp in seconds of this result
            timestampCapture = timestampNt * 1.0e-6 - latency

            # use camera pose from the image capture timestamp
            lateRobotPose = self.getRobotPose(timestampCapture)
            robotToCamera = self.getRobotToCamera(camSim, timestampCapture)
            if lateRobotPose is None or robotToCamera is None:
                return None
            lateCameraPose = lateRobotPose + robotToCamera
            cameraPoses2d.append(lateCameraPose.toPose2d())

            # process a PhotonPipelineResult with visible targets
            camResult = camSim.process(latency, lateCameraPose, allTargets)
            # publish this info to NT at estimated timestamp of receive
            camSim.submitProcessedFrame(camResult, timestampNt)
            # display debug results
            for tgt in camResult.getTargets():
                trf = tgt.getBestCameraToTarget()
                if trf == Transform3d():
                    continue

                visTgtPoses2d.append(lateCameraPose.transformBy(trf).toPose2d())

        if processed:
            self.dbgField.getObject("visibleTargetPoses").setPoses(visTgtPoses2d)

        if len(cameraPoses2d) != 0:
            self.dbgField.getObject("cameras").setPoses(cameraPoses2d)
