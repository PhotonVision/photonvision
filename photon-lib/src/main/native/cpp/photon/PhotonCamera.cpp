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

#include "photon/PhotonCamera.h"

#include <hal/FRCUsageReporting.h>

#include <string_view>

#include <frc/Errors.h>
#include <frc/RobotController.h>
#include <frc/Timer.h>
#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>

#include "PhotonVersion.h"
#include "photon/dataflow/structures/Packet.h"

inline constexpr std::string_view bfw =
    "\n\n\n\n"
    ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
    ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
    ">>>                                          \n"
    ">>> You are running an incompatible version  \n"
    ">>> of PhotonVision on your coprocessor!     \n"
    ">>>                                          \n"
    ">>> This is neither tested nor supported.    \n"
    ">>> You MUST update PhotonVision,            \n"
    ">>> PhotonLib, or both.                      \n"
    ">>>                                          \n"
    ">>> Your code will now crash.                \n"
    ">>> We hope your day gets better.            \n"
    ">>>                                          \n"
    ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
    ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
    "\n\n";

namespace photon {

constexpr const units::second_t VERSION_CHECK_INTERVAL = 5_s;
static const std::vector<std::string_view> PHOTON_PREFIX = {"/photonvision/"};
bool PhotonCamera::VERSION_CHECK_ENABLED = true;

void PhotonCamera::SetVersionCheckEnabled(bool enabled) {
  VERSION_CHECK_ENABLED = enabled;
}

PhotonCamera::PhotonCamera(nt::NetworkTableInstance instance,
                           const std::string_view cameraName)
    : mainTable(instance.GetTable("photonvision")),
      rootTable(mainTable->GetSubTable(cameraName)),
      rawBytesEntry(
          rootTable->GetRawTopic("rawBytes")
              .Subscribe("rawBytes", {}, {.periodic = 0.01, .sendAll = true})),
      inputSaveImgEntry(
          rootTable->GetIntegerTopic("inputSaveImgCmd").Publish()),
      inputSaveImgSubscriber(
          rootTable->GetIntegerTopic("inputSaveImgCmd").Subscribe(0)),
      outputSaveImgEntry(
          rootTable->GetIntegerTopic("outputSaveImgCmd").Publish()),
      outputSaveImgSubscriber(
          rootTable->GetIntegerTopic("outputSaveImgCmd").Subscribe(0)),
      pipelineIndexPub(
          rootTable->GetIntegerTopic("pipelineIndexRequest").Publish()),
      pipelineIndexSub(
          rootTable->GetIntegerTopic("pipelineIndexState").Subscribe(0)),
      ledModePub(mainTable->GetIntegerTopic("ledMode").Publish()),
      ledModeSub(mainTable->GetIntegerTopic("ledMode").Subscribe(0)),
      versionEntry(mainTable->GetStringTopic("version").Subscribe("")),
      cameraIntrinsicsSubscriber(
          rootTable->GetDoubleArrayTopic("cameraIntrinsics").Subscribe({})),
      cameraDistortionSubscriber(
          rootTable->GetDoubleArrayTopic("cameraDistortion").Subscribe({})),
      driverModeSubscriber(
          rootTable->GetBooleanTopic("driverMode").Subscribe(false)),
      driverModePublisher(
          rootTable->GetBooleanTopic("driverModeRequest").Publish()),
      topicNameSubscriber(instance, PHOTON_PREFIX, {.topicsOnly = true}),
      path(rootTable->GetPath()),
      cameraName(cameraName) {
  HAL_Report(HALUsageReporting::kResourceType_PhotonCamera, InstanceCount);
  InstanceCount++;
}

PhotonCamera::PhotonCamera(const std::string_view cameraName)
    : PhotonCamera(nt::NetworkTableInstance::GetDefault(), cameraName) {}

PhotonPipelineResult PhotonCamera::GetLatestResult() {
  if (test) {
    return testResult;
  }

  // Prints warning if not connected
  VerifyVersion();

  // Clear the current packet.
  packet.Clear();

  // Create the new result;
  PhotonPipelineResult result;

  // Fill the packet with latest data and populate result.
  units::microsecond_t now =
      units::microsecond_t(frc::RobotController::GetFPGATime());
  const auto value = rawBytesEntry.Get();
  if (!value.size()) return result;

  photon::Packet packet{value};

  packet >> result;

  result.SetRecieveTimestamp(now);

  return result;
}

void PhotonCamera::SetDriverMode(bool driverMode) {
  driverModePublisher.Set(driverMode);
}

void PhotonCamera::TakeInputSnapshot() {
  inputSaveImgEntry.Set(inputSaveImgSubscriber.Get() + 1);
}

void PhotonCamera::TakeOutputSnapshot() {
  outputSaveImgEntry.Set(outputSaveImgSubscriber.Get() + 1);
}

bool PhotonCamera::GetDriverMode() const { return driverModeSubscriber.Get(); }

void PhotonCamera::SetPipelineIndex(int index) { pipelineIndexPub.Set(index); }

int PhotonCamera::GetPipelineIndex() const {
  return static_cast<int>(pipelineIndexSub.Get());
}

LEDMode PhotonCamera::GetLEDMode() const {
  return static_cast<LEDMode>(static_cast<int>(ledModeSub.Get()));
}

std::optional<cv::Mat> PhotonCamera::GetCameraMatrix() {
  auto camCoeffs = cameraIntrinsicsSubscriber.Get();
  if (camCoeffs.size() == 9) {
    cv::Mat retVal(3, 3, CV_64FC1);
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        retVal.at<double>(i, j) = camCoeffs[(j * 3) + i];
      }
    }
    return retVal;
  }
  return std::nullopt;
}

void PhotonCamera::SetLEDMode(LEDMode mode) {
  ledModePub.Set(static_cast<int>(mode));
}

const std::string_view PhotonCamera::GetCameraName() const {
  return cameraName;
}

std::optional<cv::Mat> PhotonCamera::GetDistCoeffs() {
  auto distCoeffs = cameraDistortionSubscriber.Get();
  if (distCoeffs.size() == 5) {
    cv::Mat retVal(5, 1, CV_64FC1);
    for (int i = 0; i < 5; i++) {
      retVal.at<double>(i, 0) = distCoeffs[i];
    }
    return retVal;
  }
  return std::nullopt;
}

void PhotonCamera::VerifyVersion() {
  if (!PhotonCamera::VERSION_CHECK_ENABLED) {
    return;
  }

  if ((frc::Timer::GetFPGATimestamp() - lastVersionCheckTime) <
      VERSION_CHECK_INTERVAL)
    return;
  this->lastVersionCheckTime = frc::Timer::GetFPGATimestamp();

  const std::string& versionString = versionEntry.Get("");
  if (versionString.empty()) {
    std::string path_ = path;
    std::vector<std::string> cameraNames =
        rootTable->GetInstance().GetTable("photonvision")->GetSubTables();
    if (cameraNames.empty()) {
      FRC_ReportError(frc::warn::Warning,
                      "Could not find any PhotonVision coprocessors on "
                      "NetworkTables. Double check that PhotonVision is "
                      "running, and that your camera is connected!");
    } else {
      FRC_ReportError(
          frc::warn::Warning,
          "PhotonVision coprocessor at path {} not found on NetworkTables. "
          "Double check that your camera names match!",
          path_);

      std::string cameraNameOutString;
      for (unsigned int i = 0; i < cameraNames.size(); i++) {
        cameraNameOutString += "\n" + cameraNames[i];
      }
      FRC_ReportError(
          frc::warn::Warning,
          "Found the following PhotonVision cameras on NetworkTables:{}",
          cameraNameOutString);
    }
  } else if (!VersionMatches(versionString)) {
    FRC_ReportError(frc::warn::Warning, bfw);
    std::string error_str = fmt::format(
        "Photonlib version {} does not match coprocessor version {}!",
        PhotonVersion::versionString, versionString);
    FRC_ReportError(frc::err::Error, "{}", error_str);
    throw std::runtime_error(error_str);
  }
}

std::vector<std::string> PhotonCamera::tablesThatLookLikePhotonCameras() {
  std::vector<std::string> cameraNames = mainTable->GetSubTables();

  std::vector<std::string> ret;
  std::copy_if(
      cameraNames.begin(), cameraNames.end(), std::back_inserter(ret),
      [this](auto& it) {
        return mainTable->GetSubTable(it)->GetEntry("rawBytes").Exists();
      });

  return ret;
}

}  // namespace photon
