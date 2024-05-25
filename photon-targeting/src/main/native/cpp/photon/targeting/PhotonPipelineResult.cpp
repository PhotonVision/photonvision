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

#include "photon/targeting/PhotonPipelineResult.h"

namespace photon {
PhotonPipelineResult::PhotonPipelineResult(
    int64_t sequenceID, units::microsecond_t captureTimestamp,
    units::microsecond_t publishTimestamp,
    std::span<const PhotonTrackedTarget> targets,
    MultiTargetPNPResult multitagResult)
    : sequenceID(sequenceID),
      captureTimestamp(captureTimestamp),
      publishTimestamp(publishTimestamp),
      targets(targets.data(), targets.data() + targets.size()),
      multitagResult(multitagResult) {}

bool PhotonPipelineResult::operator==(const PhotonPipelineResult& other) const {
  return sequenceID == other.sequenceID &&
         captureTimestamp == other.captureTimestamp &&
         publishTimestamp == other.publishTimestamp &&
         ntRecieveTimestamp == other.ntRecieveTimestamp &&
         targets == other.targets && multitagResult == other.multitagResult;
}

Packet& operator<<(Packet& packet, const PhotonPipelineResult& result) {
  // Encode latency and number of targets.
  packet << result.sequenceID
         << static_cast<int64_t>(result.captureTimestamp.value())
         << static_cast<int64_t>(result.publishTimestamp.value())
         << static_cast<int8_t>(result.targets.size());

  // Encode the information of each target.
  for (auto& target : result.targets) packet << target;

  packet << result.multitagResult;
  // Return the packet
  return packet;
}

Packet& operator>>(Packet& packet, PhotonPipelineResult& result) {
  // Decode latency, existence of targets, and number of targets.
  int64_t sequenceID = 0;
  int64_t capTS = 0;
  int64_t pubTS = 0;
  int8_t targetCount = 0;
  std::vector<PhotonTrackedTarget> targets;
  MultiTargetPNPResult multitagResult;

  packet >> sequenceID >> capTS >> pubTS >> targetCount;

  targets.clear();
  targets.reserve(targetCount);

  // Decode the information of each target.
  for (int i = 0; i < targetCount; ++i) {
    PhotonTrackedTarget target;
    packet >> target;
    targets.push_back(target);
  }

  packet >> multitagResult;

  units::microsecond_t captureTS =
      units::microsecond_t{static_cast<double>(capTS)};
  units::microsecond_t publishTS =
      units::microsecond_t{static_cast<double>(pubTS)};

  result = PhotonPipelineResult{sequenceID, captureTS, publishTS, targets,
                                multitagResult};

  return packet;
}
}  // namespace photon
