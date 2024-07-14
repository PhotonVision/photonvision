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

#include <memory>
#include <string>
#include <vector>

#include <networktables/BooleanTopic.h>
#include <networktables/DoubleArrayTopic.h>
#include <networktables/DoubleTopic.h>
#include <networktables/IntegerTopic.h>
#include <networktables/MultiSubscriber.h>
#include <networktables/NetworkTable.h>
#include <networktables/NetworkTableInstance.h>
#include <networktables/RawTopic.h>
#include <networktables/StringTopic.h>
#include <units/time.h>

#include "photon/targeting/PhotonPipelineResult.h"

namespace cv {
class Mat;
}  // namespace cv

namespace photon {

enum LEDMode : int { kDefault = -1, kOff = 0, kOn = 1, kBlink = 2 };

/**
 * Represents a camera that is connected to PhotonVision.ß
 */
class PhotonCamera {
 public:
  /**
   * Constructs a PhotonCamera from a root table.
   *
   * @param instance The NetworkTableInstance to pull data from. This can be a
   * custom instance in simulation, but should *usually* be the default
   * NTInstance from {@link NetworkTableInstance::getDefault}
   * @param cameraName The name of the camera, as seen in the UI.
   * over.
   */
  explicit PhotonCamera(nt::NetworkTableInstance instance,
                        const std::string_view cameraName);

  /**
   * Constructs a PhotonCamera from the name of the camera.
   * @param cameraName The nickname of the camera (found in the PhotonVision
   * UI).
   */
  explicit PhotonCamera(const std::string_view cameraName);

  PhotonCamera(PhotonCamera&&) = default;

  virtual ~PhotonCamera() = default;

  /**
   * Returns the latest pipeline result.
   * @return The latest pipeline result.
   */
  virtual PhotonPipelineResult GetLatestResult();

  /**
   * Toggles driver mode.
   * @param driverMode Whether to set driver mode.
   */
  void SetDriverMode(bool driverMode);

  /**
   * Returns whether the camera is in driver mode.
   * @return Whether the camera is in driver mode.
   */
  bool GetDriverMode() const;

  /**
   * Request the camera to save a new image file from the input
   * camera stream with overlays.
   * Images take up space in the filesystem of the PhotonCamera.
   * Calling it frequently will fill up disk space and eventually
   * cause the system to stop working.
   * Clear out images in /opt/photonvision/photonvision_config/imgSaves
   * frequently to prevent issues.
   */
  void TakeInputSnapshot(void);

  /**
   * Request the camera to save a new image file from the output
   * stream with overlays.
   * Images take up space in the filesystem of the PhotonCamera.
   * Calling it frequently will fill up disk space and eventually
   * cause the system to stop working.
   * Clear out images in /opt/photonvision/photonvision_config/imgSaves
   * frequently to prevent issues.
   */
  void TakeOutputSnapshot(void);

  /**
   * Allows the user to select the active pipeline index.
   * @param index The active pipeline index.
   */
  void SetPipelineIndex(int index);

  /**
   * Returns the active pipeline index.
   * @return The active pipeline index.
   */
  int GetPipelineIndex() const;

  /**
   * Returns the current LED mode.
   * @return The current LED mode.
   */
  LEDMode GetLEDMode() const;

  /**
   * Sets the LED mode.
   * @param led The mode to set to.
   */
  void SetLEDMode(LEDMode led);

  /**
   * Returns the name of the camera.
   * This will return the same value that was given to the constructor as
   * cameraName.
   * @return The name of the camera.
   */
  const std::string_view GetCameraName() const;

  using CameraMatrix = Eigen::Matrix<double, 3, 3>;
  using DistortionMatrix = Eigen::Matrix<double, 8, 1>;

  /**
   * @brief Get the camera calibration matrix, in standard OpenCV form
   *
   * @return std::optional<cv::Mat>
   */
  std::optional<CameraMatrix> GetCameraMatrix();

  /**
   * @brief Get the camera calibration distortion coefficients, in OPENCV8 form.
   * Higher order terms are set to zero.
   *
   * @return std::optional<cv::Mat>
   */
  std::optional<DistortionMatrix> GetDistCoeffs();

  static void SetVersionCheckEnabled(bool enabled);

  std::shared_ptr<nt::NetworkTable> GetCameraTable() const { return rootTable; }

  // For use in tests
  bool test = false;
  PhotonPipelineResult testResult;

 protected:
  std::shared_ptr<nt::NetworkTable> mainTable;
  std::shared_ptr<nt::NetworkTable> rootTable;
  nt::RawSubscriber rawBytesEntry;
  nt::IntegerPublisher inputSaveImgEntry;
  nt::IntegerSubscriber inputSaveImgSubscriber;
  nt::IntegerPublisher outputSaveImgEntry;
  nt::IntegerSubscriber outputSaveImgSubscriber;
  nt::IntegerPublisher pipelineIndexPub;
  nt::IntegerSubscriber pipelineIndexSub;
  nt::IntegerPublisher ledModePub;
  nt::IntegerSubscriber ledModeSub;
  nt::StringSubscriber versionEntry;

  nt::DoubleArraySubscriber cameraIntrinsicsSubscriber;
  nt::DoubleArraySubscriber cameraDistortionSubscriber;

  nt::BooleanSubscriber driverModeSubscriber;
  nt::BooleanPublisher driverModePublisher;
  nt::IntegerSubscriber ledModeSubscriber;

  nt::MultiSubscriber topicNameSubscriber;

  std::string path;
  std::string cameraName;

  mutable Packet packet;

 private:
  units::second_t lastVersionCheckTime = 0_s;
  static bool VERSION_CHECK_ENABLED;
  inline static int InstanceCount = 0;

  void VerifyVersion();

  std::vector<std::string> tablesThatLookLikePhotonCameras();
};

}  // namespace photon
