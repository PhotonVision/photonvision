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

#include "photonlib/SimPhotonCamera.h"

namespace photonlib {

SimPhotonCamera::SimPhotonCamera(std::shared_ptr<nt::NetworkTable> rootTable)
    : PhotonCamera(rootTable) {}

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
