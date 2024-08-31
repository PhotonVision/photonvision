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

#include "photon/targeting/proto/PNPResultProto.h"

#include "photon.pb.h"

google::protobuf::Message* wpi::Protobuf<photon::PnpResult>::New(
    google::protobuf::Arena* arena) {
  return google::protobuf::Arena::CreateMessage<
      photonvision::proto::ProtobufPNPResult>(arena);
}

photon::PnpResult wpi::Protobuf<photon::PnpResult>::Unpack(
    const google::protobuf::Message& msg) {
  auto m = static_cast<const photonvision::proto::ProtobufPNPResult*>(&msg);

  return photon::PnpResult{photon::PnpResult_PhotonStruct{
      wpi::UnpackProtobuf<frc::Transform3d>(m->best()),
      wpi::UnpackProtobuf<frc::Transform3d>(m->alt()), m->best_reproj_err(),
      m->alt_reproj_err(), m->ambiguity()}};
}

void wpi::Protobuf<photon::PnpResult>::Pack(google::protobuf::Message* msg,
                                            const photon::PnpResult& value) {
  auto m = static_cast<photonvision::proto::ProtobufPNPResult*>(msg);

  // m->set_is_present(value.isPresent);
  wpi::PackProtobuf(m->mutable_best(), value.best);
  m->set_best_reproj_err(value.bestReprojErr);
  wpi::PackProtobuf(m->mutable_alt(), value.alt);
  m->set_alt_reproj_err(value.altReprojErr);
  m->set_ambiguity(value.ambiguity);
}
