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

#include "photonlib/Packet.h"

namespace photonlib {
PhotonCamera::PhotonCamera(std::shared_ptr<nt::NetworkTable> rootTable)
    : rawBytesEntry(rootTable->GetEntry("rawBytes")),
      driverModeEntry(rootTable->GetEntry("driverMode")),
      inputSaveImgEntry(rootTable->GetEntry("inputSaveImgCmd")),
      outputSaveImgEntry(rootTable->GetEntry("outputSaveImgCmd")),
      pipelineIndexEntry(rootTable->GetEntry("pipelineIndex")),
      ledModeEntry(mainTable->GetEntry("ledMode")) {}

PhotonCamera::PhotonCamera(const std::string& cameraName)
    : PhotonCamera(nt::NetworkTableInstance::GetDefault()
                       .GetTable("photonvision")
                       ->GetSubTable(cameraName)) {}

PhotonPipelineResult PhotonCamera::GetLatestResult() const {
  // Clear the current packet.
  packet.Clear();

  // Create the new result;
  PhotonPipelineResult result;

  // Fill the packet with latest data and populate result.
  std::shared_ptr<nt::Value> ntvalue = rawBytesEntry.GetValue();
  if (!ntvalue) return result;

  std::string value{ntvalue->GetRaw()};
  std::vector<char> bytes{value.begin(), value.end()};

  photonlib::Packet packet{bytes};

  packet >> result;
  return result;
}

void PhotonCamera::SetDriverMode(bool driverMode) {
  driverModeEntry.SetBoolean(driverMode);
}

void PhotonCamera::TakeInputSnapshot() { inputSaveImgEntry.SetBoolean(true); }

void PhotonCamera::TakeOutputSnapshot() { outputSaveImgEntry.SetBoolean(true); }

bool PhotonCamera::GetDriverMode() const {
  return driverModeEntry.GetBoolean(false);
}

void PhotonCamera::SetPipelineIndex(int index) {
  pipelineIndexEntry.SetDouble(static_cast<double>(index));
}

int PhotonCamera::GetPipelineIndex() const {
  return static_cast<int>(pipelineIndexEntry.GetDouble(0));
}

LEDMode PhotonCamera::GetLEDMode() const {
  return static_cast<LEDMode>(static_cast<int>(ledModeEntry.GetDouble(-1.0)));
}

void PhotonCamera::SetLEDMode(LEDMode mode) {
  ledModeEntry.SetDouble(static_cast<double>(static_cast<int>(mode)));
}
}  // namespace photonlib
