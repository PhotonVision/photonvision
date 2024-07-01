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

#pragma once

#include <cameraserver/CameraServer.h>
#include <photon/PhotonCamera.h>
#include <photon/PhotonTargetSortMode.h>
#include <photon/estimation/CameraTargetRelation.h>
#include <photon/estimation/VisionEstimation.h>
#include <photon/networktables/NTTopicSet.h>
#include <photon/simulation/SimCameraProperties.h>
#include <photon/simulation/VideoSimUtil.h>
#include <photon/simulation/VisionTargetSim.h>

#include <algorithm>
#include <limits>
#include <string>
#include <utility>
#include <vector>

#include <frc/Timer.h>
#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/apriltag/AprilTagFields.h>
#include <units/math.h>
#include <wpi/timestamp.h>

namespace photon {
class PhotonCameraSim {
 public:
  explicit PhotonCameraSim(PhotonCamera* camera)
      : PhotonCameraSim(camera, photon::SimCameraProperties::PERFECT_90DEG()) {}
  PhotonCameraSim(PhotonCamera* camera, const SimCameraProperties& props)
      : prop(props), cam(camera) {
    SetMinTargetAreaPixels(kDefaultMinAreaPx);
    videoSimRaw = frc::CameraServer::PutVideo(
        std::string{camera->GetCameraName()} + "-raw", prop.GetResWidth(),
        prop.GetResHeight());
    videoSimRaw.SetPixelFormat(cs::VideoMode::PixelFormat::kGray);
    videoSimProcessed = frc::CameraServer::PutVideo(
        std::string{camera->GetCameraName()} + "-processed", prop.GetResWidth(),
        prop.GetResHeight());
    ts.subTable = cam->GetCameraTable();
    ts.UpdateEntries();
  }
  PhotonCameraSim(PhotonCamera* camera, const SimCameraProperties& props,
                  double minTargetAreaPercent, units::meter_t maxSightRange)
      : PhotonCameraSim(camera, props) {
    this->minTargetAreaPercent = minTargetAreaPercent;
    this->maxSightRange = maxSightRange;
  }
  PhotonCamera* GetCamera() { return cam; }
  double GetMinTargetAreaPercent() { return minTargetAreaPercent; }
  double GetMinTargetAreaPixels() {
    return minTargetAreaPercent / 100.0 * prop.GetResArea();
  }
  units::meter_t GetMaxSightRange() { return maxSightRange; }
  const cs::CvSource& GetVideoSimRaw() { return videoSimRaw; }
  const cv::Mat& GetVideoSimFrameRaw() { return videoSimFrameRaw; }
  bool CanSeeTargetPose(const frc::Pose3d& camPose,
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
  bool CanSeeCorner(const std::vector<cv::Point2f>& points) {
    for (const auto& pt : points) {
      if (std::clamp<float>(pt.x, 0, prop.GetResWidth()) != pt.x ||
          std::clamp<float>(pt.y, 0, prop.GetResHeight()) != pt.y) {
        return false;
      }
    }
    return true;
  }
  std::optional<uint64_t> ConsumeNextEntryTime() {
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
  void SetMinTargetAreaPercent(double areaPercent) {
    minTargetAreaPercent = areaPercent;
  }
  void SetMinTargetAreaPixels(double areaPx) {
    minTargetAreaPercent = areaPx / prop.GetResArea() * 100;
  }
  void SetMaxSightRange(units::meter_t range) { maxSightRange = range; }
  void EnableRawStream(bool enabled) { videoSimRawEnabled = enabled; }
  void EnableDrawWireframe(bool enabled) { videoSimWireframeEnabled = enabled; }
  void SetWireframeResolution(double resolution) {
    videoSimWireframeResolution = resolution;
  }
  void EnabledProcessedStream(double enabled) { videoSimProcEnabled = enabled; }
  PhotonPipelineResult Process(units::second_t latency,
                               const frc::Pose3d& cameraPose,
                               std::vector<VisionTargetSim> targets) {
    std::sort(
        targets.begin(), targets.end(),
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

      PNPResult pnpSim{};
      if (tgt.fiducialId >= 0 && tgt.GetFieldVertices().size() == 4) {
        pnpSim = OpenCVHelp::SolvePNP_Square(
            prop.GetIntrinsics(), prop.GetDistCoeffs(),
            tgt.GetModel().GetVertices(), noisyTargetCorners);
      }

      std::vector<std::pair<float, float>> tempCorners =
          OpenCVHelp::PointsToCorners(minAreaRectPts);
      wpi::SmallVector<std::pair<double, double>, 4> smallVec;

      for (const auto& corner : tempCorners) {
        smallVec.emplace_back(
            std::make_pair(static_cast<double>(corner.first),
                           static_cast<double>(corner.second)));
      }

      std::vector<std::pair<float, float>> cornersFloat =
          OpenCVHelp::PointsToCorners(noisyTargetCorners);

      std::vector<std::pair<double, double>> cornersDouble{cornersFloat.begin(),
                                                           cornersFloat.end()};
      detectableTgts.emplace_back(PhotonTrackedTarget{
          -centerRot.Z().convert<units::degrees>().to<double>(),
          -centerRot.Y().convert<units::degrees>().to<double>(), areaPercent,
          centerRot.X().convert<units::degrees>().to<double>(), tgt.fiducialId,
          -1, -1, pnpSim.best, pnpSim.alt, pnpSim.ambiguity, smallVec,
          cornersDouble});
    }

    if (videoSimRawEnabled) {
      if (videoSimWireframeEnabled) {
        VideoSimUtil::DrawFieldWireFrame(
            camRt, prop, videoSimWireframeResolution, 1.5, cv::Scalar{80}, 6, 1,
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
      cv::cvtColor(videoSimFrameRaw, videoSimFrameProcessed,
                   cv::COLOR_GRAY2BGR);
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
        std::vector<std::pair<float, float>> detectedCornerFloat{
            detectedCornersDouble.begin(), detectedCornersDouble.end()};
        if (tgt.GetFiducialId() >= 0) {
          VideoSimUtil::DrawTagDetection(
              tgt.GetFiducialId(),
              OpenCVHelp::CornersToPoints(detectedCornerFloat),
              videoSimFrameProcessed);
        } else {
          cv::rectangle(videoSimFrameProcessed,
                        OpenCVHelp::GetBoundingRect(
                            OpenCVHelp::CornersToPoints(detectedCornerFloat)),
                        cv::Scalar{0, 0, 255},
                        static_cast<int>(VideoSimUtil::GetScaledThickness(
                            1, videoSimFrameProcessed)),
                        cv::LINE_AA);

          wpi::SmallVector<std::pair<double, double>, 4> smallVec =
              tgt.GetMinAreaRectCorners();

          std::vector<std::pair<float, float>> cornersCopy{};
          cornersCopy.reserve(4);

          for (const auto& corner : smallVec) {
            cornersCopy.emplace_back(
                std::make_pair(static_cast<float>(corner.first),
                               static_cast<float>(corner.second)));
          }

          VideoSimUtil::DrawPoly(
              OpenCVHelp::CornersToPoints(cornersCopy),
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

    MultiTargetPNPResult multiTagResults{};

    std::vector<frc::AprilTag> visibleLayoutTags =
        VisionEstimation::GetVisibleLayoutTags(detectableTgts, tagLayout);
    if (visibleLayoutTags.size() > 1) {
      wpi::SmallVector<int16_t, 32> usedIds{};
      std::transform(visibleLayoutTags.begin(), visibleLayoutTags.end(),
                     usedIds.begin(),
                     [](const frc::AprilTag& tag) { return tag.ID; });
      std::sort(usedIds.begin(), usedIds.end());
      PNPResult pnpResult = VisionEstimation::EstimateCamPosePNP(
          prop.GetIntrinsics(), prop.GetDistCoeffs(), detectableTgts, tagLayout,
          kAprilTag36h11);
      multiTagResults = MultiTargetPNPResult{pnpResult, usedIds};
    }

    units::second_t now = frc::Timer::GetFPGATimestamp();

    return PhotonPipelineResult{heartbeatCounter, now - latency, now,
                                detectableTgts, multiTagResults};
  }
  void SubmitProcessedFrame(const PhotonPipelineResult& result) {
    SubmitProcessedFrame(result, wpi::Now());
  }
  void SubmitProcessedFrame(const PhotonPipelineResult& result,
                            uint64_t recieveTimestamp) {
    ts.latencyMillisEntry.Set(
        result.GetLatency().convert<units::milliseconds>().to<double>(),
        recieveTimestamp);

    Packet newPacket{};
    newPacket << result;
    ts.rawBytesEntry.Set(newPacket.GetData(), recieveTimestamp);

    bool hasTargets = result.HasTargets();
    ts.hasTargetEntry.Set(hasTargets, recieveTimestamp);
    if (!hasTargets) {
      ts.targetPitchEntry.Set(0.0, recieveTimestamp);
      ts.targetYawEntry.Set(0.0, recieveTimestamp);
      ts.targetAreaEntry.Set(0.0, recieveTimestamp);
      std::array<double, 3> poseData{0.0, 0.0, 0.0};
      ts.targetPoseEntry.Set(poseData, recieveTimestamp);
      ts.targetSkewEntry.Set(0.0, recieveTimestamp);
    } else {
      PhotonTrackedTarget bestTarget = result.GetBestTarget();

      ts.targetPitchEntry.Set(bestTarget.GetPitch(), recieveTimestamp);
      ts.targetYawEntry.Set(bestTarget.GetYaw(), recieveTimestamp);
      ts.targetAreaEntry.Set(bestTarget.GetArea(), recieveTimestamp);
      ts.targetSkewEntry.Set(bestTarget.GetSkew(), recieveTimestamp);

      frc::Transform3d transform = bestTarget.GetBestCameraToTarget();
      std::array<double, 4> poseData{
          transform.X().to<double>(), transform.Y().to<double>(),
          transform.Rotation().ToRotation2d().Degrees().to<double>()};
      ts.targetPoseEntry.Set(poseData, recieveTimestamp);
    }

    auto intrinsics = prop.GetIntrinsics();
    std::vector<double> intrinsicsView{intrinsics.data(),
                                       intrinsics.data() + intrinsics.size()};
    ts.cameraIntrinsicsPublisher.Set(intrinsicsView, recieveTimestamp);

    auto distortion = prop.GetDistCoeffs();
    std::vector<double> distortionView{distortion.data(),
                                       distortion.data() + distortion.size()};
    ts.cameraDistortionPublisher.Set(distortionView, recieveTimestamp);

    ts.heartbeatPublisher.Set(heartbeatCounter++, recieveTimestamp);
  }
  SimCameraProperties prop;

 private:
  PhotonCamera* cam;

  NTTopicSet ts{};
  int64_t heartbeatCounter{0};

  uint64_t nextNTEntryTime{wpi::Now()};

  units::meter_t maxSightRange{std::numeric_limits<double>::max()};
  static constexpr double kDefaultMinAreaPx{100};
  double minTargetAreaPercent;

  frc::AprilTagFieldLayout tagLayout{
      frc::AprilTagFieldLayout::LoadField(frc::AprilTagField::k2024Crescendo)};

  cs::CvSource videoSimRaw;
  cv::Mat videoSimFrameRaw{};
  bool videoSimRawEnabled{true};
  bool videoSimWireframeEnabled{false};
  double videoSimWireframeResolution{0.1};
  cs::CvSource videoSimProcessed;
  cv::Mat videoSimFrameProcessed{};
  bool videoSimProcEnabled{true};
};
}  // namespace photon
