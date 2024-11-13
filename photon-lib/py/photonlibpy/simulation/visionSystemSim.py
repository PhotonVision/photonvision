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
    def __init__(self, visionSystemName: str):
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
        return self.camSimMap.get(name, None)

    def getCameraSims(self) -> list[PhotonCameraSim]:
        return [*self.camSimMap.values()]

    def addCamera(self, cameraSim: PhotonCameraSim, robotToCamera: Transform3d) -> None:
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
        self.camSimMap.clear()
        self.camTrfMap.clear()

    def removeCamera(self, cameraSim: PhotonCameraSim) -> bool:
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
        if cameraSim in self.camTrfMap:
            self.camTrfMap[cameraSim].addSample(
                wpilib.Timer.getFPGATimestamp(), Pose3d() + robotToCamera
            )
            return True
        else:
            return False

    def resetCameraTransforms(self, cameraSim: PhotonCameraSim | None = None) -> None:
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
        if targetType not in self.targetSets:
            self.targetSets[targetType] = targets
        else:
            self.targetSets[targetType] += targets

    def addAprilTags(self, layout: AprilTagFieldLayout) -> None:
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
        return self.robotPoseBuffer.sample(timestamp)

    def resetRobotPose(self, robotPose: Pose2d | Pose3d) -> None:
        if type(robotPose) is Pose2d:
            robotPose = Pose3d(robotPose)
        assert type(robotPose) is Pose3d

        self.robotPoseBuffer.clear()
        self.robotPoseBuffer.addSample(wpilib.Timer.getFPGATimestamp(), robotPose)

    def getDebugField(self) -> Field2d:
        return self.dbgField

    def update(self, robotPose: Pose2d | Pose3d) -> None:
        if type(robotPose) is Pose2d:
            robotPose = Pose3d(robotPose)
        assert type(robotPose) is Pose3d

        for targetType, targets in self.targetSets.items():
            posesToAdd: list[Pose2d] = []
            for target in targets:
                posesToAdd.append(target.getPose().toPose2d())
            self.dbgField.getObject(targetType).setPoses(posesToAdd)

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
        for camSim in self.camSimMap.values():
            optTimestamp = camSim.consumeNextEntryTime()
            if optTimestamp is None:
                continue
            else:
                processed = True

            timestampNt = optTimestamp
            latency = camSim.prop.estLatency()
            timestampCapture = timestampNt * 1.0e-6 - latency

            lateRobotPose = self.getRobotPose(timestampCapture)
            robotToCamera = self.getRobotToCamera(camSim, timestampCapture)
            if lateRobotPose is None or robotToCamera is None:
                return None
            lateCameraPose = lateRobotPose + robotToCamera
            cameraPoses2d.append(lateCameraPose.toPose2d())

            camResult = camSim.process(latency, lateCameraPose, allTargets)
            camSim.submitProcessedFrame(camResult, timestampNt)
            for tgt in camResult.getTargets():
                trf = tgt.getBestCameraToTarget()
                if trf == Transform3d():
                    continue

                visTgtPoses2d.append(lateCameraPose.transformBy(trf).toPose2d())

        if processed:
            self.dbgField.getObject("visibleTargetPoses").setPoses(visTgtPoses2d)

        if len(cameraPoses2d) != 0:
            self.dbgField.getObject("cameras").setPoses(cameraPoses2d)
