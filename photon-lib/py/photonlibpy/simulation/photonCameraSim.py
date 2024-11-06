from .simCameraProperties import SimCameraProperties, PERFECT_90DEG
from .visionTargetSim import VisionTargetSim
from ..photonCamera import PhotonCamera
from ..targeting import PhotonPipelineResult
from ..estimation.cameraTargetRelation import CameraTargetRelation
from ..networktables.NTTopicSet import NTTopicSet
from ..packet import Packet

from wpimath.geometry import Pose3d, Transform3d
from wpimath.units import meters, seconds
import wpilib

import cscore as cs
import numpy as np

import typing


class PhotonCameraSim:
    kDefaultMinAreaPx: float = 100

    def __init__(
        self,
        camera: PhotonCamera,
        props: SimCameraProperties | None = None,
        minTargetAreaPercent: float | None = None,
        maxSightRange: meters | None = None,
    ):

        self.minTargetAreaPercent: float = 0.0
        self.maxSightRange: float = 1.0e99
        self.videoSimRawEnabled: bool = True
        self.videoSimWireframeEnabled: bool = False
        self.videoSimWireframeResolution: float = 0.1
        self.videoSimProcEnabled: bool = True
        self.ts = NTTopicSet()
        self.heartbeatCounter: int = 0
        self.nextNtEntryTime = int(wpilib.Timer.getFPGATimestamp() * 1e6)

        if (
            camera is not None
            and props is None
            and minTargetAreaPercent is None
            and maxSightRange is None
        ):
            props = PERFECT_90DEG()
        elif (
            camera is not None
            and props is not None
            and minTargetAreaPercent is not None
            and maxSightRange is not None
        ):
            pass
        elif (
            camera is not None
            and props is not None
            and minTargetAreaPercent is None
            and maxSightRange is None
        ):
            pass
        else:
            raise Exception("Invalid Constructor Called")

        self.cam = camera
        self.prop = props
        self.setMinTargetAreaPixels(PhotonCameraSim.kDefaultMinAreaPx)

        # TODO Check fps is right
        self.videoSimRaw = cs.CvSource(
            self.cam.getName() + "-raw",
            cs.VideoMode.PixelFormat.kGray,
            self.prop.getResWidth(),
            self.prop.getResHeight(),
            1,
        )
        self.videoSimFrameRaw = np.zeros(
            (self.prop.getResWidth(), self.prop.getResHeight())
        )

        # TODO Check fps is right
        self.videoSimProcessed = cs.CvSource(
            self.cam.getName() + "-processed",
            cs.VideoMode.PixelFormat.kGray,
            self.prop.getResWidth(),
            self.prop.getResHeight(),
            1,
        )
        self.videoSimFrameProcessed = np.zeros(
            (self.prop.getResWidth(), self.prop.getResHeight())
        )

        # TODO what is the syntax to get and set the right nt subtable here
        # self.ts.subTable = self.cam.
        self.ts.updateEntries()

        # Handle this last explicitly for this function signature because the other constructor is called in the initialiser list
        if (
            camera is not None
            and props is not None
            and minTargetAreaPercent is not None
            and maxSightRange is not None
        ):
            self.minTargetAreaPercent = minTargetAreaPercent
            self.maxSightRange = maxSightRange

    def getCamera(self) -> PhotonCamera:
        return self.cam

    def getMinTargetAreaPercent(self) -> float:
        return self.minTargetAreaPercent

    def getMinTargetAreaPixels(self) -> float:
        return self.minTargetAreaPercent / 100.0 * self.prop.getResArea()

    def getMaxSightRange(self) -> meters:
        return self.maxSightRange

    def getVideoSimRaw(self) -> cs.CvSource:
        return self.videoSimRaw

    def getVideoSimFrameRaw(self) -> np.ndarray:
        return self.videoSimFrameRaw

    def canSeeTargetPose(self, camPose: Pose3d, target: VisionTargetSim) -> bool:
        rel = CameraTargetRelation(camPose, target.getPose())
        return (
            abs(rel.camToTargYaw.degrees()) < self.prop.getHorizFOV().degrees() / 2.0
            and abs(rel.camToTargPitch.degrees())
            < self.prop.getVertFOV().degrees() / 2.0
            and not target.getModel().getIsPlanar()
            or abs(rel.targtoCamAngle.degrees()) < 90
            and rel.camToTarg.translation().norm() <= self.maxSightRange
        )

    def canSeeCorner(self, points: list[tuple[float, float]]) -> bool:
        for pt in points:
            if not (
                abs((max(pt[0], min(pt[0], self.prop.getResWidth()))) - pt[0]) < 1e-4
            ) or not (
                abs((max(pt[1], min(pt[1], self.prop.getResHeight()))) - pt[1]) < 1e-4
            ):
                return False

        return True

    def consumeNextEntryTime(self) -> float | None:
        now = wpilib.Timer.getFPGATimestamp() * 1e6
        timestamp = 0
        iter = 0
        while now >= self.nextNtEntryTime:
            timestamp = int(self.nextNtEntryTime)
            frameTime = int(self.prop.estSecUntilNextFrame() * 1e6)
            self.nextNtEntryTime += frameTime

            iter += 1
            if iter > 50:
                timestamp = now
                self.nextNtEntryTime = now + frameTime

        if timestamp != 0:
            return timestamp

        return None

    def setMinTargetAreaPercent(self, areaPercent: float) -> None:
        self.minTargetAreaPercent = areaPercent

    def setMinTargetAreaPixels(self, areaPx: float) -> None:
        self.minTargetAreaPercent = areaPx / self.prop.getResArea() * 100.0

    def setMaxSightRange(self, range: meters) -> None:
        self.maxSightRange = range

    def enableRawStream(self, enabled: bool) -> None:
        self.videoSimRawEnabled = enabled

    def enableDrawWireframe(self, enabled: bool) -> None:
        self.videoSimWireframeEnabled = enabled

    def setWireframeResolution(self, resolution: float) -> None:
        self.videoSimWireframeResolution = resolution

    def enableProcessedStream(self, enabled: bool) -> None:
        self.videoSimProcEnabled = enabled

    def process(
        self, latency: seconds, cameraPose: Pose3d, targets: list[VisionTargetSim]
    ) -> PhotonPipelineResult:
        raise Exception("Not yet implemented")

    def submitProcessedFrame(
        self, result: PhotonPipelineResult, receiveTimestamp: float | None
    ):
        if receiveTimestamp is None:
            receiveTimestamp = wpilib.Timer.getFPGATimestamp() * 1e6
        receiveTimestamp = int(receiveTimestamp)

        self.ts.latencyMillisEntry.set(result.getLatencyMillis(), receiveTimestamp)

        #### TODO Create and send out Packet
        # newPacket = Packet()
        # newPacket.Pack(result)
        #
        # self.ts.rawBytesEntry.set(newPacket.getData(), receiveTimestamp)

        hasTargets = result.hasTargets()
        self.ts.hasTargetEntry.set(hasTargets, receiveTimestamp)
        if not hasTargets:
            self.ts.targetPitchEntry.set(0.0, receiveTimestamp)
            self.ts.targetYawEntry.set(0.0, receiveTimestamp)
            self.ts.targetAreaEntry.set(0.0, receiveTimestamp)
            self.ts.targetPoseEntry.set(Transform3d(), receiveTimestamp)
            self.ts.targetSkewEntry.set(0.0, receiveTimestamp)
        else:
            bestTarget = result.getBestTarget()

            self.ts.targetPitchEntry.set(bestTarget.getPitch(), receiveTimestamp)
            self.ts.targetYawEntry.set(bestTarget.getYaw(), receiveTimestamp)
            self.ts.targetAreaEntry.set(bestTarget.getArea(), receiveTimestamp)
            self.ts.targetSkewEntry.set(bestTarget.getSkew(), receiveTimestamp)

            self.ts.targetPoseEntry.set(
                bestTarget.getBestCameraToTarget(), receiveTimestamp
            )

            intrinsics = self.prop.getIntrinsics()
            intrinsicsView = intrinsics.tolist()
            self.ts.cameraIntrinsicsPublisher.set(intrinsicsView, receiveTimestamp)

            distortion = self.prop.getDistCoeffs()
            distortionView = distortion.tolist()
            self.ts.cameraDistortionPublisher.set(distortionView, receiveTimestamp)

            self.ts.heartbeatPublisher.set(self.heartbeatCounter, receiveTimestamp)

            self.ts.subTable.getInstance().flush()
