/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

bool PhotonCamera::GetDriverMode() const { return driverMode; }

void PhotonCamera::SetPipelineIndex(int index) {
  pipelineIndexEntry.SetDouble(static_cast<double>(index));
}

int PhotonCamera::GetPipelineIndex() const { return pipelineIndex; }

LEDMode PhotonCamera::GetLEDMode() const {
  return static_cast<LEDMode>(static_cast<int>(ledModeEntry.GetDouble(-1.0)));
}

void PhotonCamera::SetLEDMode(LEDMode mode) {
  ledModeEntry.SetDouble(static_cast<double>(static_cast<int>(mode)));
}
}  // namespace photonlib
