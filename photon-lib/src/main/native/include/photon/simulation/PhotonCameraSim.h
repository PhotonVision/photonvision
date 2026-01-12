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

#include <algorithm>
#include <limits>
#include <string>
#include <utility>
#include <vector>

#include <cameraserver/CameraServer.h>
#include <frc/Timer.h>
#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/apriltag/AprilTagFields.h>
#include <photon/PhotonCamera.h>
#include <photon/PhotonTargetSortMode.h>
#include <photon/estimation/CameraTargetRelation.h>
#include <photon/estimation/VisionEstimation.h>
#include <photon/networktables/NTTopicSet.h>
#include <photon/simulation/SimCameraProperties.h>
#include <photon/simulation/VideoSimUtil.h>
#include <photon/simulation/VisionTargetSim.h>
#include <units/math.h>
#include <wpi/timestamp.h>

namespace photon {
class PhotonCameraSim {
 public:
  /**
   * Constructs a handle for simulating PhotonCamera values. Processing
   * simulated targets through this class will change the associated
   * PhotonCamera's results.
   *
   * WARNING: This constructor's camera has a 90 deg FOV with no simulated lag!
   *
   * By default, the minimum target area is 100 pixels and there is no
   * maximum sight range.
   *
   * @param camera The camera to be simulated
   */
  explicit PhotonCameraSim(PhotonCamera* camera);

  /**
   * Constructs a handle for simulating PhotonCamera values. Processing
   * simulated targets through this class will change the associated
   * PhotonCamera's results.
   *
   * By default, the minimum target area is 100 pixels and there is no
   * maximum sight range.
   *
   * @param camera The camera to be simulated
   * @param prop Properties of this camera such as FOV and FPS
   * @param tagLayout The AprilTagFieldLayout used to solve for tag
   * positions.
   */
  PhotonCameraSim(PhotonCamera* camera, const SimCameraProperties& props,
                  const frc::AprilTagFieldLayout& tagLayout =
                      frc::AprilTagFieldLayout::LoadField(
                          frc::AprilTagField::kDefaultField));

  /**
   * Constructs a handle for simulating PhotonCamera values. Processing
   * simulated targets through this class will change the associated
   * PhotonCamera's results.
   *
   * @param camera The camera to be simulated
   * @param prop Properties of this camera such as FOV and FPS
   * @param minTargetAreaPercent The minimum percentage (0 - 100) a detected
   * target must take up of the camera's image to be processed. Match this with
   * your contour filtering settings in the PhotonVision GUI.
   * @param maxSightRange Maximum distance at which the target is
   * illuminated to your camera. Note that minimum target area of the image is
   * separate from this.
   */
  PhotonCameraSim(PhotonCamera* camera, const SimCameraProperties& props,
                  double minTargetAreaPercent, units::meter_t maxSightRange);

  /**
   * Returns the camera being simulated.
   *
   * @return The camera
   */
  inline PhotonCamera* GetCamera() { return cam; }

  /**
   * Returns the minimum percentage (0 - 100) a detected target must take up of
   * the camera's image to be processed.
   *
   * @return The percentage
   */
  inline double GetMinTargetAreaPercent() { return minTargetAreaPercent; }

  /**
   * Returns the minimum number of pixels a detected target must take up in the
   * camera's image to be processed.
   *
   * @return The number of pixels
   */
  inline double GetMinTargetAreaPixels() {
    return minTargetAreaPercent / 100.0 * prop.GetResArea();
  }

  /**
   * Returns the maximum distance at which the target is illuminated to your
   * camera. Note that minimum target area of the image is separate from this.
   *
   * @return The distance
   */
  inline units::meter_t GetMaxSightRange() { return maxSightRange; }
  inline const cs::CvSource& GetVideoSimRaw() { return videoSimRaw; }
  inline const cv::Mat& GetVideoSimFrameRaw() { return videoSimFrameRaw; }

  /**
   * Determines if this target's pose should be visible to the camera without
   * considering its projected image points. Does not account for image area.
   *
   * @param camPose Camera's 3d pose
   * @param target Vision target containing pose and shape
   * @return If this vision target can be seen before image projection.
   */
  bool CanSeeTargetPose(const frc::Pose3d& camPose,
                        const VisionTargetSim& target);

  /**
   * Determines if all target points are inside the camera's image.
   *
   * @param points The target's 2d image points
   * @return True if all the target points are inside the camera's image, false
   * otherwise.
   */
  bool CanSeeCorner(const std::vector<cv::Point2f>& points);

  /**
   * Determine if this camera should process a new frame based on performance
   * metrics and the time since the last update. This returns an Optional which
   * is either empty if no update should occur or a Long of the timestamp in
   * microseconds of when the frame which should be received by NT. If a
   * timestamp is returned, the last frame update time becomes that timestamp.
   *
   * @return Optional long which is empty while blocked or the NT entry
   * timestamp in microseconds if ready
   */
  std::optional<uint64_t> ConsumeNextEntryTime();

  /**
   * Sets the minimum percentage (0 - 100) a detected target must take up of the
   * camera's image to be processed.
   *
   * @param areaPercent The percentage
   */
  inline void SetMinTargetAreaPercent(double areaPercent) {
    minTargetAreaPercent = areaPercent;
  }

  /**
   * Sets the minimum number of pixels a detected target must take up in the
   * camera's image to be processed.
   *
   * @param areaPx The number of pixels
   */
  inline void SetMinTargetAreaPixels(double areaPx) {
    minTargetAreaPercent = areaPx / prop.GetResArea() * 100;
  }

  /**
   * Sets the maximum distance at which the target is illuminated to your
   * camera. Note that minimum target area of the image is separate from this.
   *
   * @param rangeMeters The distance
   */
  inline void SetMaxSightRange(units::meter_t range) { maxSightRange = range; }

  /**
   * Sets whether the raw video stream simulation is enabled.
   *
   * Note: This may increase loop times.
   *
   * @param enabled Whether or not to enable the raw video stream
   */
  inline void EnableRawStream(bool enabled) { videoSimRawEnabled = enabled; }

  /**
   * Sets whether a wireframe of the field is drawn to the raw video stream.
   *
   * Note: This will dramatically increase loop times.
   *
   * @param enabled Whether or not to enable the wireframe in the raw video
   * stream
   */
  inline void EnableDrawWireframe(bool enabled) {
    videoSimWireframeEnabled = enabled;
  }

  /**
   * Sets the resolution of the drawn wireframe if enabled. Drawn line segments
   * will be subdivided into smaller segments based on a threshold set by the
   * resolution.
   *
   * @param resolution Resolution as a fraction(0 - 1) of the video frame's
   * diagonal length in pixels
   */
  inline void SetWireframeResolution(double resolution) {
    videoSimWireframeResolution = resolution;
  }

  /**
   * Sets whether the processed video stream simulation is enabled.
   *
   * @param enabled Whether or not to enable the processed video stream
   */
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
