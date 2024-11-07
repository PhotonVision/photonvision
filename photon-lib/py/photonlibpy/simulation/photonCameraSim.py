from .simCameraProperties import SimCameraProperties
from .visionTargetSim import VisionTargetSim
from ..photonCamera import PhotonCamera
from ..targeting import (
    MultiTargetPNPResult,
    PhotonPipelineResult,
    PhotonPipelineMetadata,
    PhotonTrackedTarget,
    PnpResult,
    TargetCorner,
)
from ..estimation import OpenCVHelp, RotTrlTransform3d, TargetModel, VisionEstimation
from ..estimation.cameraTargetRelation import CameraTargetRelation
from ..networktables.NTTopicSet import NTTopicSet
from ..packet import Packet

from wpimath.geometry import Pose3d, Transform3d, Translation3d
from wpimath.units import meters, seconds

import robotpy_apriltag

import wpilib

import cscore as cs
import numpy as np

import math
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
        self.videoSimRawEnabled: bool = False
        self.videoSimWireframeEnabled: bool = False
        self.videoSimWireframeResolution: float = 0.1
        self.videoSimProcEnabled: bool = True
        self.ts = NTTopicSet()
        self.heartbeatCounter: int = 0
        self.nextNtEntryTime = int(wpilib.Timer.getFPGATimestamp() * 1e6)
        self.tagLayout = robotpy_apriltag.loadAprilTagLayoutField(
            robotpy_apriltag.AprilTagField.k2024Crescendo
        )

        if (
            camera is not None
            and props is None
            and minTargetAreaPercent is None
            and maxSightRange is None
        ):
            props = SimCameraProperties.PERFECT_90DEG()
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

        self.ts.subTable = self.cam._cameraTable
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
        raise ("Raw stream not implemented")
        # self.videoSimRawEnabled = enabled

    def enableDrawWireframe(self, enabled: bool) -> None:
        raise ("Wireframe not implemented")
        # self.videoSimWireframeEnabled = enabled

    def setWireframeResolution(self, resolution: float) -> None:
        self.videoSimWireframeResolution = resolution

    def enableProcessedStream(self, enabled: bool) -> None:
        raise ("Processed stream not implemented")
        # self.videoSimProcEnabled = enabled

    def process(
        self, latency: seconds, cameraPose: Pose3d, targets: list[VisionTargetSim]
    ) -> PhotonPipelineResult:
        # Sort targets by distance to camera - furthest to closest
        def distance(target: VisionTargetSim):
            return target.getPose().translation().distance(cameraPose.translation())

        targets.sort(key=distance, reverse=True)

        visibleTgts: list[
            typing.Tuple[VisionTargetSim, list[typing.Tuple[float, float]]]
        ] = []
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
                # TODO Implement this
                """
                cv::Point2d center = OpenCVHelp::AvgPoint(imagePoints);
                int l = 0;
                int t = 0;
                int b = 0;
                int r = 0;
                for (int i = 0; i < 4; i++) {
                  if (imagePoints[i].x < imagePoints[l].x) {
                    l = i;
                  }
                }
                cv::Point2d lc = imagePoints[l];
                std::array<double, 4> angles{};
                t = (l + 1) % 4;
                b = (l + 1) % 4;
                for (int i = 0; i < 4; i++) {
                  if (i == l) {
                    continue;
                  }
                  cv::Point2d ic = imagePoints[i];
                  angles[i] = std::atan2(lc.y - ic.y, ic.x - lc.x);
                  if (angles[i] >= angles[t]) {
                    t = i;
                  }
                  if (angles[i] <= angles[b]) {
                    b = i;
                  }
                }
                for (int i = 0; i < 4; i++) {
                  if (i != t && i != l && i != b) {
                    r = i;
                  }
                }
                cv::RotatedRect rect{
                    cv::Point2d{center.x, center.y},
                    cv::Size2d{imagePoints[r].x - lc.x,
                               imagePoints[b].y - imagePoints[t].y},
                    units::radian_t{-angles[r]}.convert<units::degrees>().to<float>()};
                std::vector<cv::Point2f> points{};
                rect.points(points);

                // Can't find an easier way to convert from Point2f to Point2d
                imagePoints.clear();
                std::transform(points.begin(), points.end(),
                               std::back_inserter(imagePoints),
                               [](const cv::Point2f& p) { return (cv::Point2d)p; });

                """
                pass

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
                pnpSim = OpenCVHelp.solvePNP_SQPNP(
                    self.prop.getIntrinsics(),
                    self.prop.getDistCoeffs(),
                    tgt.getModel().getVertices(),
                    noisyTargetCorners,
                )

            # tempCorners = OpenCVHelp.pointsToCorners(minAreaRectPts)
            smallVec: list[TargetCorner] = []

            # for corner in tempCorners:
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
                    bestCameraToTarget=pnpSim.best if pnpSim else Transform3d(),
                    altCameraToTarget=pnpSim.alt if pnpSim else Transform3d(),
                    poseAmbiguity=pnpSim.ambiguity if pnpSim else -1,
                )
            )

        # Video streams disabled for now
        if self.enableRawStream:
            # VideoSimUtil::UpdateVideoProp(videoSimRaw, prop);
            # cv::Size videoFrameSize{prop.GetResWidth(), prop.GetResHeight()};
            # cv::Mat blankFrame = cv::Mat::zeros(videoFrameSize, CV_8UC1);
            # blankFrame.assignTo(videoSimFrameRaw);
            pass
        if self.enableProcessedStream:
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
            multiTagResult=multiTagResults,
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
