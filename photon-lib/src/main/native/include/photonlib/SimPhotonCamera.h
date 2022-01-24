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
   * @param instance The NetworkTableInstance to pull data from. This can be a
   * custom instance in simulation, but should *usually* be the default
   * NTInstance from {@link NetworkTableInstance::getDefault}
   * @param cameraName The name of the camera, as seen in the UI.
   */
  explicit SimPhotonCamera(std::shared_ptr<nt::NetworkTableInstance> instance,
                           const std::string& cameraName);

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
