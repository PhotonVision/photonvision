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

#include "photon/targeting/MultiTargetPNPResult.h"

#include "photon.pb.h"

namespace photon {

bool MultiTargetPNPResult::operator==(const MultiTargetPNPResult& other) const {
  return other.result == result && other.fiducialIdsUsed == fiducialIdsUsed;
}
}  // namespace photon

google::protobuf::Message* wpi::Protobuf<photon::MultiTargetPNPResult>::New(
    google::protobuf::Arena* arena) {
  return google::protobuf::Arena::CreateMessage<
      photonvision::proto::ProtobufMultiTargetPNPResult>(arena);
}

photon::MultiTargetPNPResult
wpi::Protobuf<photon::MultiTargetPNPResult>::Unpack(
    const google::protobuf::Message& msg) {
  auto m =
      static_cast<const photonvision::proto::ProtobufMultiTargetPNPResult*>(
          &msg);

  wpi::SmallVector<int16_t, 32> fiducialIdsUsed;
  for (int i = 0; i < m->fiducial_ids_used_size(); i++) {
    fiducialIdsUsed.push_back(m->fiducial_ids_used(i));
  }

  return photon::MultiTargetPNPResult{
      wpi::UnpackProtobuf<photon::PNPResult>(m->estimated_pose()),
      fiducialIdsUsed};
}

void wpi::Protobuf<photon::MultiTargetPNPResult>::Pack(
    google::protobuf::Message* msg, const photon::MultiTargetPNPResult& value) {
  auto m = static_cast<photonvision::proto::ProtobufMultiTargetPNPResult*>(msg);

  wpi::PackProtobuf(m->mutable_estimated_pose(), value.result);

  m->clear_fiducial_ids_used();
  for (const auto& t : value.fiducialIdsUsed) {
    m->add_fiducial_ids_used(t);
  }
}
