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
  explicit PhotonCameraSim(PhotonCamera* camera);
  PhotonCameraSim(PhotonCamera* camera, const SimCameraProperties& props,
                  const frc::AprilTagFieldLayout& tagLayout =
                      frc::AprilTagFieldLayout::LoadField(
                          frc::AprilTagField::kDefaultField));
  PhotonCameraSim(PhotonCamera* camera, const SimCameraProperties& props,
                  double minTargetAreaPercent, units::meter_t maxSightRange);

  inline PhotonCamera* GetCamera() { return cam; }
  inline double GetMinTargetAreaPercent() { return minTargetAreaPercent; }
  inline double GetMinTargetAreaPixels() {
    return minTargetAreaPercent / 100.0 * prop.GetResArea();
  }
  inline units::meter_t GetMaxSightRange() { return maxSightRange; }
  inline const cs::CvSource& GetVideoSimRaw() { return videoSimRaw; }
  inline const cv::Mat& GetVideoSimFrameRaw() { return videoSimFrameRaw; }

  bool CanSeeTargetPose(const frc::Pose3d& camPose,
                        const VisionTargetSim& target);
  bool CanSeeCorner(const std::vector<cv::Point2f>& points);
  std::optional<uint64_t> ConsumeNextEntryTime();

  inline void SetMinTargetAreaPercent(double areaPercent) {
    minTargetAreaPercent = areaPercent;
  }
  inline void SetMinTargetAreaPixels(double areaPx) {
    minTargetAreaPercent = areaPx / prop.GetResArea() * 100;
  }
  inline void SetMaxSightRange(units::meter_t range) { maxSightRange = range; }
  inline void EnableRawStream(bool enabled) { videoSimRawEnabled = enabled; }
  inline void EnableDrawWireframe(bool enabled) {
    videoSimWireframeEnabled = enabled;
  }
  inline void SetWireframeResolution(double resolution) {
    videoSimWireframeResolution = resolution;
  }
  inline void EnabledProcessedStream(double enabled) {
    videoSimProcEnabled = enabled;
  }
  PhotonPipelineResult Process(units::second_t latency,
                               const frc::Pose3d& cameraPose,
                               std::vector<VisionTargetSim> targets);

  void SubmitProcessedFrame(const PhotonPipelineResult& result);
  void SubmitProcessedFrame(const PhotonPipelineResult& result,
                            uint64_t ReceiveTimestamp);

  SimCameraProperties prop;

 private:
  PhotonCamera* cam;

  NTTopicSet ts{};
  int64_t heartbeatCounter{0};

  uint64_t nextNTEntryTime{wpi::Now()};

  units::meter_t maxSightRange{std::numeric_limits<double>::max()};
  static constexpr double kDefaultMinAreaPx{100};
  double minTargetAreaPercent;

  frc::AprilTagFieldLayout tagLayout;

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
