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

#pragma once

#include <memory>
#include <string>

#include <units/time.h>
#include <wpi/SmallVector.h>
#include <wpi/span.h>

#include "photonlib/Packet.h"
#include "photonlib/PhotonCamera.h"

namespace photonlib {

/**
 * Represents a camera that is connected to PhotonVision.ß
 */
class SimPhotonCamera : public PhotonCamera {
 public:
  /**
   * Constructs a Simulated PhotonCamera from a root table.
   *
   * @param rootTable The root table that the camera is broadcasting information
   *                  over.
   */
  explicit SimPhotonCamera(std::shared_ptr<nt::NetworkTable> rootTable);

  /**
   * Constructs a Simulated PhotonCamera from the name of the camera.
   *
   * @param cameraName The nickname of the camera (found in the PhotonVision
   *                   UI).
   */
  explicit SimPhotonCamera(const std::string& cameraName);

  /**
   * Simulate one processed frame of vision data, putting one result to NT.
   * @param latency Latency of frame processing
   * @param tgtList Set of targets detected
   */
  void SubmitProcessedFrame(units::second_t latency,
                            wpi::span<const PhotonTrackedTarget> tgtList);

 private:
  mutable Packet simPacket;
};

}  // namespace photonlib
