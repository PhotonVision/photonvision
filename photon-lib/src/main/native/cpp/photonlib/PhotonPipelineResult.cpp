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

#include "photonlib/PhotonPipelineResult.h"

namespace photonlib {
PhotonPipelineResult::PhotonPipelineResult(
    units::second_t latency, std::span<const PhotonTrackedTarget> targets)
    : latency(latency),
      targets(targets.data(), targets.data() + targets.size()) {}

bool PhotonPipelineResult::operator==(const PhotonPipelineResult& other) const {
  return latency == other.latency && targets == other.targets;
}

bool PhotonPipelineResult::operator!=(const PhotonPipelineResult& other) const {
  return !operator==(other);
}

Packet& operator<<(Packet& packet, const PhotonPipelineResult& result) {
  // Encode latency and number of targets.
  packet << result.latency.value() * 1000
         << static_cast<int8_t>(result.targets.size());

  // Encode the information of each target.
  for (auto& target : result.targets) packet << target;

  // Return the packet
  return packet;
}

Packet& operator>>(Packet& packet, PhotonPipelineResult& result) {
  // Decode latency, existence of targets, and number of targets.
  int8_t targetCount = 0;
  double latencyMillis = 0;
  packet >> latencyMillis >> targetCount;
  result.latency = units::second_t(latencyMillis / 1000.0);

  result.targets.clear();

  // Decode the information of each target.
  for (int i = 0; i < targetCount; ++i) {
    PhotonTrackedTarget target;
    packet >> target;
    result.targets.push_back(target);
  }
  return packet;
}

}  // namespace photonlib
