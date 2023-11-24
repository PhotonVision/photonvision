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

#include "photon.pb.h"

namespace photon {
bool PhotonPipelineResult::operator==(const PhotonPipelineResult& other) const {
  return latency == other.latency && targets == other.targets &&
         multitagResult == other.multitagResult;
}
}  // namespace photon

google::protobuf::Message* wpi::Protobuf<photon::PhotonPipelineResult>::New(
    google::protobuf::Arena* arena) {
  return google::protobuf::Arena::CreateMessage<
      photonvision::proto::ProtobufPhotonPipelineResult>(arena);
}

photon::PhotonPipelineResult
wpi::Protobuf<photon::PhotonPipelineResult>::Unpack(
    const google::protobuf::Message& msg) {
  auto m =
      static_cast<const photonvision::proto::ProtobufPhotonPipelineResult*>(
          &msg);

  std::vector<photon::PhotonTrackedTarget> targets;
  targets.reserve(m->targets_size());
  for (const auto& t : m->targets()) {
    targets.emplace_back(wpi::UnpackProtobuf<photon::PhotonTrackedTarget>(t));
  }

  if (m->has_multi_target_result()) {
    return photon::PhotonPipelineResult{
        units::millisecond_t{m->latency_ms()}, targets,
        wpi::UnpackProtobuf<photon::MultiTargetPNPResult>(
            m->multi_target_result())};
  } else {
    return photon::PhotonPipelineResult{units::millisecond_t{m->latency_ms()},
                                        targets};
  }
}

void wpi::Protobuf<photon::PhotonPipelineResult>::Pack(
    google::protobuf::Message* msg, const photon::PhotonPipelineResult& value) {
  auto m = static_cast<photonvision::proto::ProtobufPhotonPipelineResult*>(msg);

  m->set_latency_ms(value.latency.value());

  m->clear_targets();
  for (const auto& t : value.GetTargets()) {
    wpi::PackProtobuf(m->add_targets(), t);
  }

  if (value.multitagResult.has_value()) {
    wpi::PackProtobuf(m->mutable_multi_target_result(),
                      value.multitagResult.value());
  }
}
