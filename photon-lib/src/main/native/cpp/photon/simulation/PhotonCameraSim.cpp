/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#include "photon/simulation/PhotonCameraSim.h"

#include <algorithm>
#include <string>
#include <utility>
#include <vector>

#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/apriltag/AprilTagFields.h>

namespace photon {
PhotonCameraSim::PhotonCameraSim(PhotonCamera* camera)
    : PhotonCameraSim(camera, photon::SimCameraProperties::PERFECT_90DEG(),
                      frc::AprilTagFieldLayout::LoadField(
                          frc::AprilTagField::kDefaultField)) {}

PhotonCameraSim::PhotonCameraSim(PhotonCamera* camera,
                                 const SimCameraProperties& props,
                                 const frc::AprilTagFieldLayout& tagLayout)
    : prop{props}, cam{camera}, tagLayout{tagLayout} {
  SetMinTargetAreaPixels(kDefaultMinAreaPx);
  videoSimRaw =
      frc::CameraServer::PutVideo(std::string{camera->GetCameraName()} + "-raw",
                                  prop.GetResWidth(), prop.GetResHeight());
  videoSimRaw.SetPixelFormat(cs::VideoMode::PixelFormat::kGray);
  videoSimProcessed = frc::CameraServer::PutVideo(
      std::string{camera->GetCameraName()} + "-processed", prop.GetResWidth(),
      prop.GetResHeight());
  ts.subTable = cam->GetCameraTable();
  ts.UpdateEntries();
}

PhotonCameraSim::PhotonCameraSim(PhotonCamera* camera,
                                 const SimCameraProperties& props,
                                 double minTargetAreaPercent,
                                 units::meter_t maxSightRange)
    : PhotonCameraSim(camera, props) {
  this->minTargetAreaPercent = minTargetAreaPercent;
  this->maxSightRange = maxSightRange;
}

bool PhotonCameraSim::CanSeeTargetPose(const frc::Pose3d& camPose,
                                       const VisionTargetSim& target) {
  CameraTargetRelation rel{camPose, target.GetPose()};
  return ((units::math::abs(rel.camToTargYaw.Degrees()) <
           prop.GetHorizFOV().Degrees() / 2.0) &&
          (units::math::abs(rel.camToTargPitch.Degrees()) <
           prop.GetVertFOV().Degrees() / 2.0) &&
          (!target.GetModel().GetIsPlanar() ||
           units::math::abs(rel.targToCamAngle.Degrees()) < 90_deg) &&
          (rel.camToTarg.Translation().Norm() <= maxSightRange));
}
bool PhotonCameraSim::CanSeeCorner(const std::vector<cv::Point2f>& points) {
  for (const auto& pt : points) {
    if (std::clamp<float>(pt.x, 0, prop.GetResWidth()) != pt.x ||
        std::clamp<float>(pt.y, 0, prop.GetResHeight()) != pt.y) {
      return false;
    }
  }
  return true;
}
std::optional<uint64_t> PhotonCameraSim::ConsumeNextEntryTime() {
  uint64_t now = wpi::Now();
  uint64_t timestamp{};
  int iter = 0;
  while (now >= nextNTEntryTime) {
    timestamp = nextNTEntryTime;
    uint64_t frameTime = prop.EstSecUntilNextFrame()
                             .convert<units::microseconds>()
                             .to<uint64_t>();
    nextNTEntryTime += frameTime;

    if (iter++ > 50) {
      timestamp = now;
      nextNTEntryTime = now + frameTime;
      break;
    }
  }

  if (timestamp != 0) {
    return timestamp;
  } else {
    return std::nullopt;
  }
}
PhotonPipelineResult PhotonCameraSim::Process(
    units::second_t latency, const frc::Pose3d& cameraPose,
    std::vector<VisionTargetSim> targets) {
  std::sort(targets.begin(), targets.end(),
            [cameraPose](const VisionTargetSim& t1, const VisionTargetSim& t2) {
              units::meter_t dist1 =
                  t1.GetPose().Translation().Distance(cameraPose.Translation());
              units::meter_t dist2 =
                  t2.GetPose().Translation().Distance(cameraPose.Translation());
              return dist1 > dist2;
            });

  std::vector<std::pair<VisionTargetSim, std::vector<cv::Point2f>>>
      visibleTgts{};
  std::vector<PhotonTrackedTarget> detectableTgts{};
  RotTrlTransform3d camRt = RotTrlTransform3d::MakeRelativeTo(cameraPose);

  VideoSimUtil::UpdateVideoProp(videoSimRaw, prop);
  VideoSimUtil::UpdateVideoProp(videoSimProcessed, prop);
  cv::Size videoFrameSize{prop.GetResWidth(), prop.GetResHeight()};
  cv::Mat blankFrame = cv::Mat::zeros(videoFrameSize, CV_8UC1);
  blankFrame.assignTo(videoSimFrameRaw);

  for (const auto& tgt : targets) {
    if (!CanSeeTargetPose(cameraPose, tgt)) {
      continue;
    }

    std::vector<frc::Translation3d> fieldCorners = tgt.GetFieldVertices();
    if (tgt.GetModel().GetIsSpherical()) {
      TargetModel model = tgt.GetModel();
      fieldCorners = model.GetFieldVertices(TargetModel::GetOrientedPose(
          tgt.GetPose().Translation(), cameraPose.Translation()));
    }

    std::vector<cv::Point2f> imagePoints = OpenCVHelp::ProjectPoints(
        prop.GetIntrinsics(), prop.GetDistCoeffs(), camRt, fieldCorners);
    if (tgt.GetModel().GetIsSpherical()) {
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
    }

    visibleTgts.emplace_back(std::make_pair(tgt, imagePoints));
    std::vector<cv::Point2f> noisyTargetCorners =
        prop.EstPixelNoise(imagePoints);
    cv::RotatedRect minAreaRect =
        OpenCVHelp::GetMinAreaRect(noisyTargetCorners);
    std::vector<cv::Point2f> minAreaRectPts;
    minAreaRectPts.reserve(4);
    minAreaRect.points(minAreaRectPts);
    cv::Point2d centerPt = minAreaRect.center;
    frc::Rotation3d centerRot = prop.GetPixelRot(centerPt);
    double areaPercent = prop.GetContourAreaPercent(noisyTargetCorners);

    if (!(CanSeeCorner(noisyTargetCorners) &&
          areaPercent >= minTargetAreaPercent)) {
      continue;
    }

    std::optional<photon::PnpResult> pnpSim = std::nullopt;
    if (tgt.fiducialId >= 0 && tgt.GetFieldVertices().size() == 4) {
      pnpSim = OpenCVHelp::SolvePNP_Square(
          prop.GetIntrinsics(), prop.GetDistCoeffs(),
          tgt.GetModel().GetVertices(), noisyTargetCorners);
    }

    std::vector<std::pair<float, float>> tempCorners =
        OpenCVHelp::PointsToCorners(minAreaRectPts);
    std::vector<TargetCorner> smallVec;

    for (const auto& corner : tempCorners) {
      smallVec.emplace_back(static_cast<double>(corner.first),
                            static_cast<double>(corner.second));
    }

    auto cornersFloat = OpenCVHelp::PointsToTargetCorners(noisyTargetCorners);

    std::vector<TargetCorner> cornersDouble{cornersFloat.begin(),
                                            cornersFloat.end()};
    detectableTgts.emplace_back(
        -centerRot.Z().convert<units::degrees>().to<double>(),
        -centerRot.Y().convert<units::degrees>().to<double>(), areaPercent,
        centerRot.X().convert<units::degrees>().to<double>(), tgt.fiducialId,
        tgt.objDetClassId, tgt.objDetConf,
        pnpSim ? pnpSim->best : frc::Transform3d{},
        pnpSim ? pnpSim->alt : frc::Transform3d{},
        pnpSim ? pnpSim->ambiguity : -1, smallVec, cornersDouble);
  }

  if (videoSimRawEnabled) {
    if (videoSimWireframeEnabled) {
      VideoSimUtil::DrawFieldWireFrame(camRt, prop, videoSimWireframeResolution,
                                       1.5, cv::Scalar{80}, 6, 1,
                                       cv::Scalar{30}, videoSimFrameRaw);
    }

    for (const auto& pair : visibleTgts) {
      VisionTargetSim tgt = pair.first;
      std::vector<cv::Point2f> corners = pair.second;

      if (tgt.fiducialId > 0) {
        VideoSimUtil::Warp165h5TagImage(tgt.fiducialId, corners, true,
                                        videoSimFrameRaw);
      } else if (!tgt.GetModel().GetIsSpherical()) {
        std::vector<cv::Point2f> contour = corners;
        if (!tgt.GetModel().GetIsPlanar()) {
          contour = OpenCVHelp::GetConvexHull(contour);
        }
        VideoSimUtil::DrawPoly(contour, -1, cv::Scalar{255}, true,
                               videoSimFrameRaw);
      } else {
        VideoSimUtil::DrawInscribedEllipse(corners, cv::Scalar{255},
                                           videoSimFrameRaw);
      }
    }
    videoSimRaw.PutFrame(videoSimFrameRaw);
  } else {
    videoSimRaw.SetConnectionStrategy(
        cs::VideoSource::ConnectionStrategy::kConnectionForceClose);
  }

  if (videoSimProcEnabled) {
    cv::cvtColor(videoSimFrameRaw, videoSimFrameProcessed, cv::COLOR_GRAY2BGR);
    cv::drawMarker(
        videoSimFrameProcessed,
        cv::Point2d{prop.GetResWidth() / 2.0, prop.GetResHeight() / 2.0},
        cv::Scalar{0, 255, 0}, cv::MARKER_CROSS,
        static_cast<int>(
            VideoSimUtil::GetScaledThickness(15, videoSimFrameProcessed)),
        static_cast<int>(
            VideoSimUtil::GetScaledThickness(1, videoSimFrameProcessed)),
        cv::LINE_AA);
    for (const auto& tgt : detectableTgts) {
      auto detectedCornersDouble = tgt.GetDetectedCorners();
      if (tgt.GetFiducialId() >= 0) {
        VideoSimUtil::DrawTagDetection(
            tgt.GetFiducialId(),
            OpenCVHelp::CornersToPoints(detectedCornersDouble),
            videoSimFrameProcessed);
      } else {
        cv::rectangle(videoSimFrameProcessed,
                      OpenCVHelp::GetBoundingRect(
                          OpenCVHelp::CornersToPoints(detectedCornersDouble)),
                      cv::Scalar{0, 0, 255},
                      static_cast<int>(VideoSimUtil::GetScaledThickness(
                          1, videoSimFrameProcessed)),
                      cv::LINE_AA);

        auto smallVec = tgt.GetMinAreaRectCorners();

        std::vector<std::pair<float, float>> cornersCopy{};
        cornersCopy.reserve(4);

        VideoSimUtil::DrawPoly(
            OpenCVHelp::CornersToPoints(smallVec),
            static_cast<int>(
                VideoSimUtil::GetScaledThickness(1, videoSimFrameProcessed)),
            cv::Scalar{255, 30, 30}, true, videoSimFrameProcessed);
      }
    }
    videoSimProcessed.PutFrame(videoSimFrameProcessed);
  } else {
    videoSimProcessed.SetConnectionStrategy(
        cs::VideoSource::ConnectionStrategy::kConnectionForceClose);
  }

  std::optional<MultiTargetPNPResult> multiTagResults = std::nullopt;

  std::vector<frc::AprilTag> visibleLayoutTags =
      VisionEstimation::GetVisibleLayoutTags(detectableTgts, tagLayout);
  if (visibleLayoutTags.size() > 1) {
    std::vector<int16_t> usedIds{};
    usedIds.resize(visibleLayoutTags.size());
    std::transform(visibleLayoutTags.begin(), visibleLayoutTags.end(),
                   usedIds.begin(),
                   [](const frc::AprilTag& tag) { return tag.ID; });
    std::sort(usedIds.begin(), usedIds.end());
    auto pnpResult = VisionEstimation::EstimateCamPosePNP(
        prop.GetIntrinsics(), prop.GetDistCoeffs(), detectableTgts, tagLayout,
        kAprilTag36h11);
    if (pnpResult) {
      multiTagResults = MultiTargetPNPResult{*pnpResult, usedIds};
    }
  }

  heartbeatCounter++;
  return PhotonPipelineResult{
      PhotonPipelineMetadata{heartbeatCounter, 0,
                             units::microsecond_t{latency}.to<int64_t>(),
                             1000000},
      detectableTgts, multiTagResults};
}
void PhotonCameraSim::SubmitProcessedFrame(const PhotonPipelineResult& result) {
  SubmitProcessedFrame(result, wpi::Now());
}
void PhotonCameraSim::SubmitProcessedFrame(const PhotonPipelineResult& result,
                                           uint64_t ReceiveTimestamp) {
  ts.latencyMillisEntry.Set(
      result.GetLatency().convert<units::milliseconds>().to<double>(),
      ReceiveTimestamp);

  Packet newPacket{};
  newPacket.Pack(result);

  ts.rawBytesEntry.Set(newPacket.GetData(), ReceiveTimestamp);

  bool hasTargets = result.HasTargets();
  ts.hasTargetEntry.Set(hasTargets, ReceiveTimestamp);
  if (!hasTargets) {
    ts.targetPitchEntry.Set(0.0, ReceiveTimestamp);
    ts.targetYawEntry.Set(0.0, ReceiveTimestamp);
    ts.targetAreaEntry.Set(0.0, ReceiveTimestamp);
    ts.targetPoseEntry.Set(frc::Transform3d{}, ReceiveTimestamp);
    ts.targetSkewEntry.Set(0.0, ReceiveTimestamp);
  } else {
    PhotonTrackedTarget bestTarget = result.GetBestTarget();

    ts.targetPitchEntry.Set(bestTarget.GetPitch(), ReceiveTimestamp);
    ts.targetYawEntry.Set(bestTarget.GetYaw(), ReceiveTimestamp);
    ts.targetAreaEntry.Set(bestTarget.GetArea(), ReceiveTimestamp);
    ts.targetSkewEntry.Set(bestTarget.GetSkew(), ReceiveTimestamp);

    ts.targetPoseEntry.Set(bestTarget.GetBestCameraToTarget(),
                           ReceiveTimestamp);
  }

  Eigen::Matrix<double, 3, 3, Eigen::RowMajor> intrinsics =
      prop.GetIntrinsics();
  std::span<double> intrinsicsView{intrinsics.data(),
                                   intrinsics.data() + intrinsics.size()};
  ts.cameraIntrinsicsPublisher.Set(intrinsicsView, ReceiveTimestamp);

  auto distortion = prop.GetDistCoeffs();
  std::vector<double> distortionView{distortion.data(),
                                     distortion.data() + distortion.size()};
  ts.cameraDistortionPublisher.Set(distortionView, ReceiveTimestamp);

  ts.heartbeatPublisher.Set(heartbeatCounter, ReceiveTimestamp);

  ts.subTable->GetInstance().Flush();

  fmt::println("published seq {} at {}", result.metadata.sequenceID, ReceiveTimestamp);
}

}  // namespace photon
