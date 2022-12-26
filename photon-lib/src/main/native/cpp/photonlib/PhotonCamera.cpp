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

PhotonCamera::PhotonCamera(std::shared_ptr<nt::NetworkTableInstance> instance,
                           const std::string_view cameraName)
    : mainTable(instance->GetTable("photonvision")),
      rootTable(mainTable->GetSubTable(cameraName)),
      rawBytesEntry(rootTable->GetRawTopic("rawBytes").Subscribe("raw", {})),
      driverModeEntry(rootTable->GetBooleanTopic("driverMode").Publish()),
      inputSaveImgEntry(
          rootTable->GetBooleanTopic("inputSaveImgCmd").Publish()),
      outputSaveImgEntry(
          rootTable->GetBooleanTopic("outputSaveImgCmd").Publish()),
      pipelineIndexEntry(rootTable->GetIntegerTopic("pipelineIndex").Publish()),
      ledModeEntry(mainTable->GetIntegerTopic("ledMode").Publish()),
      versionEntry(mainTable->GetStringTopic("version").Subscribe("")),
      driverModeSubscriber(
          rootTable->GetBooleanTopic("driverMode").Subscribe(false)),
      pipelineIndexSubscriber(
          rootTable->GetIntegerTopic("pipelineIndex").Subscribe(-1)),
      ledModeSubscriber(mainTable->GetIntegerTopic("ledMode").Subscribe(0)),
      path(rootTable->GetPath()),
      m_cameraName(cameraName) {}

PhotonCamera::PhotonCamera(const std::string_view cameraName)
    : PhotonCamera(std::make_shared<nt::NetworkTableInstance>(
                       nt::NetworkTableInstance::GetDefault()),
                   cameraName) {}

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
  driverModeEntry.Set(driverMode);
}

void PhotonCamera::TakeInputSnapshot() { inputSaveImgEntry.Set(true); }

void PhotonCamera::TakeOutputSnapshot() { outputSaveImgEntry.Set(true); }

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
    FRC_ReportError(
        frc::warn::Warning,
        "PhotonVision coprocessor at path {} not found on NetworkTables!",
        path_);
  } else if (!VersionMatches(versionString)) {
    FRC_ReportError(frc::warn::Warning,
                    "Photon version {} does not match coprocessor version {}!",
                    PhotonVersion::versionString, versionString);
  }
}

}  // namespace photonlib
