import math
import typing

import cscore as cs
import cv2 as cv
import numpy as np
import wpilib
from robotpy_apriltag import AprilTagField, AprilTagFieldLayout
from wpimath.geometry import Pose3d, Transform3d
from wpimath.units import meters, seconds

from ..estimation import OpenCVHelp, RotTrlTransform3d, TargetModel, VisionEstimation
from ..estimation.cameraTargetRelation import CameraTargetRelation
from ..networktables.NTTopicSet import NTTopicSet
from ..photonCamera import PhotonCamera
from ..targeting import (
    MultiTargetPNPResult,
    PhotonPipelineMetadata,
    PhotonPipelineResult,
    PhotonTrackedTarget,
    PnpResult,
    TargetCorner,
)
from .simCameraProperties import SimCameraProperties
from .visionTargetSim import VisionTargetSim


class PhotonCameraSim:
    kDefaultMinAreaPx: float = 100

    def __init__(
        self,
        camera: PhotonCamera,
        props: SimCameraProperties = SimCameraProperties.PERFECT_90DEG(),
        minTargetAreaPercent: float | None = None,
        maxSightRange: meters | None = None,
    ):

        self.minTargetAreaPercent: float = 0.0
        self.maxSightRange: float = 1.0e99
        self.videoSimRawEnabled: bool = False
        self.videoSimWireframeEnabled: bool = False
        self.videoSimWireframeResolution: float = 0.1
        self.videoSimProcEnabled: bool = (
            False  # TODO switch this back to default True when the functionality is enabled
        )
        self.heartbeatCounter: int = 0
        self.nextNtEntryTime = int(wpilib.Timer.getFPGATimestamp() * 1e6)
        self.tagLayout = AprilTagFieldLayout.loadField(AprilTagField.k2024Crescendo)

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

        self.ts = NTTopicSet("photonvision", self.cam.getName())
        self.ts.updateEntries()

        # Handle this last explicitly for this function signature because the other constructor is called in the initialiser list
        if minTargetAreaPercent is not None and maxSightRange is not None:
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
            (
                abs(rel.camToTargYaw.degrees())
                < self.prop.getHorizFOV().degrees() / 2.0
                and abs(rel.camToTargPitch.degrees())
                < self.prop.getVertFOV().degrees() / 2.0
            )
            and (
                not target.getModel().getIsPlanar()
                or abs(rel.targtoCamAngle.degrees()) < 90
            )
            and rel.camToTarg.translation().norm() <= self.maxSightRange
        )

    def canSeeCorner(self, points: np.ndarray) -> bool:
        assert points.shape[1] == 1
        assert points.shape[2] == 2
        for pt in points:
            x = pt[0, 0]
            y = pt[0, 1]
            if (
                x < 0
                or x > self.prop.getResWidth()
                or y < 0
                or y > self.prop.getResHeight()
            ):
                return False

        return True

    def consumeNextEntryTime(self) -> float | None:
        now = int(wpilib.Timer.getFPGATimestamp() * 1e6)
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
                break

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
        raise Exception("Raw stream not implemented")

    def enableDrawWireframe(self, enabled: bool) -> None:
        self.videoSimWireframeEnabled = enabled
        raise Exception("Wireframe not implemented")

    def setWireframeResolution(self, resolution: float) -> None:
        self.videoSimWireframeResolution = resolution

    def enableProcessedStream(self, enabled: bool) -> None:
        self.videoSimProcEnabled = enabled
        raise Exception("Processed stream not implemented")

    def process(
        self, latency: seconds, cameraPose: Pose3d, targets: list[VisionTargetSim]
    ) -> PhotonPipelineResult:
        # Sort targets by distance to camera - furthest to closest
        def distance(target: VisionTargetSim):
            return target.getPose().translation().distance(cameraPose.translation())

        targets.sort(key=distance, reverse=True)

        visibleTgts: list[typing.Tuple[VisionTargetSim, np.ndarray]] = []
        detectableTgts: list[PhotonTrackedTarget] = []

        camRt = RotTrlTransform3d.makeRelativeTo(cameraPose)

        for tgt in targets:
            if not self.canSeeTargetPose(cameraPose, tgt):
                continue

            fieldCorners = tgt.getFieldVertices()
            isSpherical = tgt.getModel().getIsSpherical()
            if isSpherical:
                model = tgt.getModel()
                fieldCorners = model.getFieldVertices(
                    TargetModel.getOrientedPose(
                        tgt.getPose().translation(), cameraPose.translation()
                    )
                )

            imagePoints = OpenCVHelp.projectPoints(
                self.prop.getIntrinsics(),
                self.prop.getDistCoeffs(),
                camRt,
                fieldCorners,
            )

            if isSpherical:
                center = OpenCVHelp.avgPoint(imagePoints)
                l: int = 0
                for i in range(4):
                    if imagePoints[i, 0, 0] < imagePoints[l, 0, 0].x:
                        l = i

                lc = imagePoints[l]
                angles = [
                    0.0,
                ] * 4
                t = (l + 1) % 4
                b = (l + 1) % 4
                r = 0
                for i in range(4):
                    if i == l:
                        continue
                    ic = imagePoints[i]
                    angles[i] = math.atan2(lc[0, 1] - ic[0, 1], ic[0, 0] - lc[0, 0])
                    if angles[i] >= angles[t]:
                        t = i
                    if angles[i] <= angles[b]:
                        b = i
                for i in range(4):
                    if i != t and i != l and i != b:
                        r = i
                rect = cv.RotatedRect(
                    (center[0, 0], center[0, 1]),
                    (
                        imagePoints[r, 0, 0] - lc[0, 0],
                        imagePoints[b, 0, 1] - imagePoints[t, 0, 1],
                    ),
                    -angles[r],
                )
                imagePoints = np.array([[p[0], p[1], p[2]] for p in rect.points()])

            visibleTgts.append((tgt, imagePoints))
            noisyTargetCorners = self.prop.estPixelNoise(imagePoints)
            minAreaRect = OpenCVHelp.getMinAreaRect(noisyTargetCorners)
            minAreaRectPts = minAreaRect.points()
            centerPt = minAreaRect.center
            centerRot = self.prop.getPixelRot(centerPt)
            areaPercent = self.prop.getContourAreaPercent(noisyTargetCorners)

            if (
                not self.canSeeCorner(noisyTargetCorners)
                or not areaPercent >= self.minTargetAreaPercent
            ):
                continue

            pnpSim: PnpResult | None = None
            if tgt.fiducialId >= 0 and len(tgt.getFieldVertices()) == 4:
                pnpSim = OpenCVHelp.solvePNP_Square(
                    self.prop.getIntrinsics(),
                    self.prop.getDistCoeffs(),
                    tgt.getModel().getVertices(),
                    noisyTargetCorners,
                )

            smallVec: list[TargetCorner] = []
            for corner in minAreaRectPts:
                smallVec.append(TargetCorner(corner[0], corner[1]))

            cornersFloat = OpenCVHelp.pointsToTargetCorners(noisyTargetCorners)

            detectableTgts.append(
                PhotonTrackedTarget(
                    yaw=math.degrees(-centerRot.Z()),
                    pitch=math.degrees(-centerRot.Y()),
                    area=areaPercent,
                    skew=math.degrees(centerRot.X()),
                    fiducialId=tgt.fiducialId,
                    detectedCorners=cornersFloat,
                    minAreaRectCorners=smallVec,
                    bestCameraToTarget=pnpSim.best if pnpSim else Transform3d(),
                    altCameraToTarget=pnpSim.alt if pnpSim else Transform3d(),
                    poseAmbiguity=pnpSim.ambiguity if pnpSim else -1,
                )
            )

        # Video streams disabled for now
        if self.videoSimRawEnabled:
            # VideoSimUtil::UpdateVideoProp(videoSimRaw, prop);
            # cv::Size videoFrameSize{prop.GetResWidth(), prop.GetResHeight()};
            # cv::Mat blankFrame = cv::Mat::zeros(videoFrameSize, CV_8UC1);
            # blankFrame.assignTo(videoSimFrameRaw);
            pass
        if self.videoSimProcEnabled:
            # VideoSimUtil::UpdateVideoProp(videoSimProcessed, prop);
            pass

        multiTagResults: MultiTargetPNPResult | None = None

        visibleLayoutTags = VisionEstimation.getVisibleLayoutTags(
            detectableTgts, self.tagLayout
        )

        if len(visibleLayoutTags) > 1:
            usedIds = [tag.ID for tag in visibleLayoutTags]
            usedIds.sort()
            pnpResult = VisionEstimation.estimateCamPosePNP(
                self.prop.getIntrinsics(),
                self.prop.getDistCoeffs(),
                detectableTgts,
                self.tagLayout,
                TargetModel.AprilTag36h11(),
            )
            if pnpResult is not None:
                multiTagResults = MultiTargetPNPResult(pnpResult, usedIds)

        self.heartbeatCounter += 1
        return PhotonPipelineResult(
            metadata=PhotonPipelineMetadata(
                self.heartbeatCounter, int(latency * 1e6), 1000000
            ),
            targets=detectableTgts,
            multitagResult=multiTagResults,
        )

    def submitProcessedFrame(
        self, result: PhotonPipelineResult, receiveTimestamp: float | None
    ):
        if receiveTimestamp is None:
            receiveTimestamp = wpilib.Timer.getFPGATimestamp() * 1e6
        receiveTimestamp = int(receiveTimestamp)

        self.ts.latencyMillisEntry.set(result.getLatencyMillis(), receiveTimestamp)

        newPacket = PhotonPipelineResult.photonStruct.pack(result)
        self.ts.rawBytesEntry.set(newPacket.getData(), receiveTimestamp)

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
            assert bestTarget

            self.ts.targetPitchEntry.set(bestTarget.getPitch(), receiveTimestamp)
            self.ts.targetYawEntry.set(bestTarget.getYaw(), receiveTimestamp)
            self.ts.targetAreaEntry.set(bestTarget.getArea(), receiveTimestamp)
            self.ts.targetSkewEntry.set(bestTarget.getSkew(), receiveTimestamp)

            self.ts.targetPoseEntry.set(
                bestTarget.getBestCameraToTarget(), receiveTimestamp
            )

            intrinsics = self.prop.getIntrinsics()
            intrinsicsView = intrinsics.flatten().tolist()
            self.ts.cameraIntrinsicsPublisher.set(intrinsicsView, receiveTimestamp)

            distortion = self.prop.getDistCoeffs()
            distortionView = distortion.flatten().tolist()
            self.ts.cameraDistortionPublisher.set(distortionView, receiveTimestamp)

            self.ts.heartbeatPublisher.set(self.heartbeatCounter, receiveTimestamp)

            self.ts.subTable.getInstance().flush()
