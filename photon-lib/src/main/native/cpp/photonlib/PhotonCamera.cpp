/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

#include "photonlib/PhotonCamera.h"

#include <frc/Errors.h>
#include <frc/Timer.h>

#include "PhotonVersion.h"
#include "photonlib/Packet.h"

namespace photonlib {

constexpr const units::second_t VERSION_CHECK_INTERVAL = 5_s;
static const std::vector<std::string_view> PHOTON_PREFIX = {"/photonvision/"};

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
      pipelineIndexEntry(rootTable->GetIntegerTopic("pipelineIndex").Publish()),
      ledModeEntry(mainTable->GetIntegerTopic("ledMode").Publish()),
      versionEntry(mainTable->GetStringTopic("version").Subscribe("")),
      driverModeSubscriber(
          rootTable->GetBooleanTopic("driverMode").Subscribe(false)),
      driverModePublisher(
          rootTable->GetBooleanTopic("driverModeRequest").Publish()),
      pipelineIndexSubscriber(
          rootTable->GetIntegerTopic("pipelineIndex").Subscribe(-1)),
      ledModeSubscriber(mainTable->GetIntegerTopic("ledMode").Subscribe(0)),
      m_topicNameSubscriber(instance, PHOTON_PREFIX, {.topicsOnly = true}),
      path(rootTable->GetPath()),
      m_cameraName(cameraName) {}

PhotonCamera::PhotonCamera(const std::string_view cameraName)
    : PhotonCamera(nt::NetworkTableInstance::GetDefault(), cameraName) {}

PhotonPipelineResult PhotonCamera::GetLatestResult() {
  if (test) return testResult;
  // Prints warning if not connected
  VerifyVersion();

  // Clear the current packet.
  packet.Clear();

  // Create the new result;
  PhotonPipelineResult result;

  // Fill the packet with latest data and populate result.
  const auto value = rawBytesEntry.Get();
  if (!value.size()) return result;

  photonlib::Packet packet{value};

  packet >> result;

  result.SetTimestamp(units::microsecond_t(rawBytesEntry.GetLastChange()) -
                      result.GetLatency());

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

void PhotonCamera::SetPipelineIndex(int index) {
  pipelineIndexEntry.Set(static_cast<double>(index));
}

int PhotonCamera::GetPipelineIndex() const {
  return static_cast<int>(pipelineIndexSubscriber.Get());
}

LEDMode PhotonCamera::GetLEDMode() const {
  return static_cast<LEDMode>(static_cast<int>(ledModeSubscriber.Get()));
}

void PhotonCamera::SetLEDMode(LEDMode mode) {
  ledModeEntry.Set(static_cast<double>(static_cast<int>(mode)));
}

const std::string_view PhotonCamera::GetCameraName() const {
  return m_cameraName;
}

void PhotonCamera::VerifyVersion() {
  if (!PhotonCamera::VERSION_CHECK_ENABLED) return;

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
    FRC_ReportError(frc::warn::Warning,
                    "Photon version {} does not match coprocessor version {}!",
                    PhotonVersion::versionString, versionString);
  }
}

}  // namespace photonlib
