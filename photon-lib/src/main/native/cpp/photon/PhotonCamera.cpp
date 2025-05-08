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
#include <net/TimeSyncServer.h>

#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

#include <WPILibVersion.h>
#include <frc/Errors.h>
#include <frc/RobotController.h>
#include <frc/Timer.h>
#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>
#include <wpi/json.h>

#include "PhotonVersion.h"
#include "opencv2/core/utility.hpp"
#include "photon/dataflow/structures/Packet.h"

static constexpr units::second_t WARN_DEBOUNCE_SEC = 5_s;
static constexpr units::second_t HEARTBEAT_DEBOUNCE_SEC = 500_ms;

inline void verifyDependencies() {
  if (!(std::string_view{GetWPILibVersion()} ==
        std::string_view{photon::PhotonVersion::wpilibTargetVersion})) {
    std::string bfw =
        "\n\n\n\n\n"
        ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
        ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        ">>>                                          \n"
        ">>> You are running an incompatible version  \n"
        ">>> of PhotonVision !                        \n"
        ">>>                                          \n"
        ">>> PhotonLib ";
    bfw += photon::PhotonVersion::versionString;
    bfw += " is built for WPILib ";
    bfw += photon::PhotonVersion::wpilibTargetVersion;
    bfw +=
        "\n"
        ">>> but you are using WPILib ";
    bfw += GetWPILibVersion();
    bfw +=
        "\n>>>                                          \n"
        ">>> This is neither tested nor supported.    \n"
        ">>> You MUST update PhotonVision,            \n"
        ">>> PhotonLib, or both.                      \n"
        ">>> Verify the output of `./gradlew dependencies` \n"
        ">>>                                          \n"
        ">>> Your code will now crash.                \n"
        ">>> We hope your day gets better.            \n"
        ">>>                                          \n"
        ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n";

    FRC_ReportWarning(bfw);
    FRC_ReportError(frc::err::Error, bfw);
    throw new std::runtime_error(std::string{bfw});
  }
  if (!(std::string_view{cv::getVersionString()} ==
        std::string_view{photon::PhotonVersion::opencvTargetVersion})) {
    std::string bfw =
        "\n\n\n\n\n"
        ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
        ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        ">>>                                          \n"
        ">>> You are running an incompatible version  \n"
        ">>> of PhotonVision !                        \n"
        ">>>                                          \n"
        ">>> PhotonLib ";
    bfw += photon::PhotonVersion::versionString;
    bfw += " is built for OpenCV ";
    bfw += photon::PhotonVersion::opencvTargetVersion;
    bfw +=
        "\n"
        ">>> but you are using OpenCV ";
    bfw += cv::getVersionString();
    bfw +=
        "\n>>>                                          \n"
        ">>> This is neither tested nor supported.    \n"
        ">>> You MUST update PhotonVision,            \n"
        ">>> PhotonLib, or both.                      \n"
        ">>> Verify the output of `./gradlew dependencies` \n"
        ">>>                                          \n"
        ">>> Your code will now crash.                \n"
        ">>> We hope your day gets better.            \n"
        ">>>                                          \n"
        ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n";

    FRC_ReportWarning(bfw);
    FRC_ReportError(frc::err::Error, bfw);
    throw new std::runtime_error(std::string{bfw});
  }
}

// bit of a hack -- start a TimeSync server on port 5810 (hard-coded). We want
// to avoid calling this from static initialization
static void InitTspServer() {
  // We dont impose requirements about not calling the PhotonCamera constructor
  // from different threads, so i guess we need this?
  static std::mutex g_timeSyncServerMutex;
  static bool g_timeSyncServerStarted{false};
  static wpi::tsp::TimeSyncServer timesyncServer{5810};

  std::lock_guard lock{g_timeSyncServerMutex};
  if (!g_timeSyncServerStarted) {
    timesyncServer.Start();
    g_timeSyncServerStarted = true;
  }
}

namespace photon {

constexpr const units::second_t VERSION_CHECK_INTERVAL = 5_s;
static const std::vector<std::string_view> PHOTON_PREFIX = {"/photonvision/"};
static const std::string PHOTON_ALERT_GROUP{"PhotonAlerts"};
bool PhotonCamera::VERSION_CHECK_ENABLED = true;

void PhotonCamera::SetVersionCheckEnabled(bool enabled) {
  VERSION_CHECK_ENABLED = enabled;
}

static const std::string TYPE_STRING =
    std::string{"photonstruct:PhotonPipelineResult:"} +
    std::string{SerdeType<PhotonPipelineResult>::GetSchemaHash()};

PhotonCamera::PhotonCamera(nt::NetworkTableInstance instance,
                           const std::string_view cameraName)
    : mainTable(instance.GetTable("photonvision")),
      rootTable(mainTable->GetSubTable(cameraName)),
      rawBytesEntry(
          rootTable->GetRawTopic("rawBytes")
              .Subscribe(
                  TYPE_STRING, {},
                  {.pollStorage = 20, .periodic = 0.01, .sendAll = true})),
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
      heartbeatSubscriber(
          rootTable->GetIntegerTopic("heartbeat").Subscribe(-1)),
      topicNameSubscriber(instance, PHOTON_PREFIX, {.topicsOnly = true}),
      path(rootTable->GetPath()),
      cameraName(cameraName),
      disconnectAlert(PHOTON_ALERT_GROUP,
                      std::string{"PhotonCamera '"} + std::string{cameraName} +
                          "' is disconnected.",
                      frc::Alert::AlertType::kWarning),
      timesyncAlert(PHOTON_ALERT_GROUP, "", frc::Alert::AlertType::kWarning) {
  verifyDependencies();
  HAL_Report(HALUsageReporting::kResourceType_PhotonCamera, InstanceCount);
  InstanceCount++;

  // The Robot class is actually created here:
  // https://github.com/wpilibsuite/allwpilib/blob/811b1309683e930a1ce69fae818f943ff161b7a5/wpilibc/src/main/native/include/frc/RobotBase.h#L33
  // so we should be fine to call this from the ctor
  InitTspServer();
}

PhotonCamera::PhotonCamera(const std::string_view cameraName)
    : PhotonCamera(nt::NetworkTableInstance::GetDefault(), cameraName) {}

PhotonPipelineResult PhotonCamera::GetLatestResult() {
  if (test) {
    if (testResult.size())
      return testResult.back();
    else
      return PhotonPipelineResult{};
  }

  // Prints warning if not connected
  VerifyVersion();

  // Fill the packet with latest data and populate result.
  units::microsecond_t now =
      units::microsecond_t(frc::RobotController::GetFPGATime());
  const auto value = rawBytesEntry.Get();
  if (!value.size()) return PhotonPipelineResult{};

  photon::Packet packet{value};

  // Create the new result;
  PhotonPipelineResult result = packet.Unpack<PhotonPipelineResult>();

  CheckTimeSyncOrWarn(result);

  result.SetReceiveTimestamp(now);

  return result;
}

std::vector<PhotonPipelineResult> PhotonCamera::GetAllUnreadResults() {
  if (test) {
    return testResult;
  }

  // Prints warning if not connected
  VerifyVersion();
  UpdateDisconnectAlert();

  const auto changes = rawBytesEntry.ReadQueue();

  // Create the new result list
  std::vector<PhotonPipelineResult> ret;
  ret.reserve(changes.size());

  for (size_t i = 0; i < changes.size(); i++) {
    const nt::Timestamped<std::vector<uint8_t>>& value = changes[i];

    if (!value.value.size() || value.time == 0) {
      continue;
    }

    // Fill the packet with latest data and populate result.
    photon::Packet packet{value.value};
    auto result = packet.Unpack<PhotonPipelineResult>();

    CheckTimeSyncOrWarn(result);

    // TODO: NT4 timestamps are still not to be trusted. But it's the best we
    // can do until we can make time sync more reliable.
    result.SetReceiveTimestamp(units::microsecond_t(value.time) -
                               result.GetLatency());

    ret.push_back(result);
  }

  return ret;
}

void PhotonCamera::UpdateDisconnectAlert() {
  disconnectAlert.Set(!IsConnected());
}

void PhotonCamera::CheckTimeSyncOrWarn(photon::PhotonPipelineResult& result) {
  if (result.metadata.timeSinceLastPong > 5L * 1000000L) {
    std::string warningText =
        "PhotonVision coprocessor at path " + path +
        " is not connected to the TimeSyncServer? It's been " +
        std::to_string(result.metadata.timeSinceLastPong / 1e6) +
        "s since the coprocessor last heard a pong.";

    timesyncAlert.SetText(warningText);
    timesyncAlert.Set(true);

    if (frc::Timer::GetFPGATimestamp() <
        (prevTimeSyncWarnTime + WARN_DEBOUNCE_SEC)) {
      prevTimeSyncWarnTime = frc::Timer::GetFPGATimestamp();

      FRC_ReportWarning(
          warningText +
          "\n\nCheck /photonvision/.timesync/{{COPROCESSOR_HOSTNAME}} for more "
          "information.");
    }
  } else {
    // Got a valid packet, reset the last time
    prevTimeSyncWarnTime = 0_s;
    timesyncAlert.Set(false);
  }
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

void PhotonCamera::SetLEDMode(LEDMode mode) {
  ledModePub.Set(static_cast<int>(mode));
}

const std::string_view PhotonCamera::GetCameraName() const {
  return cameraName;
}

bool PhotonCamera::IsConnected() {
  auto currentHeartbeat = heartbeatSubscriber.Get();
  auto now = frc::Timer::GetFPGATimestamp();

  if (currentHeartbeat < 0) {
    // we have never heard from the camera
    return false;
  }

  if (currentHeartbeat != prevHeartbeatValue) {
    // New heartbeat value from the coprocessor
    prevHeartbeatChangeTime = now;
    prevHeartbeatValue = currentHeartbeat;
  }

  return (now - prevHeartbeatChangeTime) < HEARTBEAT_DEBOUNCE_SEC;
}

std::optional<PhotonCamera::CameraMatrix> PhotonCamera::GetCameraMatrix() {
  auto camCoeffs = cameraIntrinsicsSubscriber.Get();
  if (camCoeffs.size() == 9) {
    PhotonCamera::CameraMatrix retVal =
        Eigen::Map<const Eigen::Matrix<double, 3, 3, Eigen::RowMajor>>(
            camCoeffs.data());
    return retVal;
  }

  return std::nullopt;
}

std::optional<PhotonCamera::DistortionMatrix> PhotonCamera::GetDistCoeffs() {
  auto distCoeffs = cameraDistortionSubscriber.Get();
  auto bound = distCoeffs.size();
  if (bound > 0 && bound <= 8) {
    PhotonCamera::DistortionMatrix retVal =
        PhotonCamera::DistortionMatrix::Zero();

    Eigen::Map<const Eigen::VectorXd> map(distCoeffs.data(), bound);
    retVal.block(0, 0, bound, 1) = map;

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
        cameraNameOutString += ("\n" + cameraNames[i]);
      }
      FRC_ReportError(
          frc::warn::Warning,
          "Found the following PhotonVision cameras on NetworkTables:\n{}",
          cameraNameOutString);
    }
  } else {
    std::string local_uuid{SerdeType<PhotonPipelineResult>::GetSchemaHash()};

    // implicit conversion here might throw an exception, so be careful of that
    wpi::json remote_uuid_json =
        rawBytesEntry.GetTopic().GetProperty("message_uuid");
    if (!remote_uuid_json.is_string()) {
      FRC_ReportError(frc::warn::Warning,
                      "Cannot find property message_uuid for PhotonCamera {}",
                      path);
      return;
    }
    std::string remote_uuid{remote_uuid_json};

    if (local_uuid != remote_uuid) {
      constexpr std::string_view bfw =
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
      FRC_ReportWarning(bfw);
      std::string error_str = fmt::format(
          "Photonlib version {} (message definition version {}) does not match "
          "coprocessor version {} (message definition version {})!",
          PhotonVersion::versionString, local_uuid, versionString, remote_uuid);
      FRC_ReportError(frc::err::Error, "{}", error_str);
      throw std::runtime_error(error_str);
    }
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
