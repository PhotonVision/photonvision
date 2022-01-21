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

#include "photonlib/SimPhotonCamera.h"

namespace photonlib {

SimPhotonCamera::SimPhotonCamera(
    std::shared_ptr<nt::NetworkTableInstance> instance,
    const std::string& cameraName)
    : PhotonCamera(instance, cameraName) {}

SimPhotonCamera::SimPhotonCamera(const std::string& cameraName)
    : PhotonCamera(cameraName) {}

void SimPhotonCamera::SubmitProcessedFrame(
    units::second_t latency, wpi::span<const PhotonTrackedTarget> tgtList) {
  if (!GetDriverMode()) {
    // Clear the current packet.
    simPacket.Clear();

    // Create the new result and pump it into the packet
    simPacket << PhotonPipelineResult(latency, tgtList);

    rawBytesEntry.SetRaw(std::string_view{simPacket.GetData().data(),
                                          simPacket.GetData().size()});
  }
}

}  // namespace photonlib
