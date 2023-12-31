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

#include "photon/targeting/proto/PhotonTrackedTargetProto.h"

#include "photon.pb.h"

google::protobuf::Message* wpi::Protobuf<photon::PhotonTrackedTarget>::New(
    google::protobuf::Arena* arena) {
  return google::protobuf::Arena::CreateMessage<
      photonvision::proto::ProtobufPhotonTrackedTarget>(arena);
}

photon::PhotonTrackedTarget wpi::Protobuf<photon::PhotonTrackedTarget>::Unpack(
    const google::protobuf::Message& msg) {
  auto m = static_cast<const photonvision::proto::ProtobufPhotonTrackedTarget*>(
      &msg);

  wpi::SmallVector<std::pair<double, double>, 4> minAreaRectCorners;
  for (const auto& t : m->min_area_rect_corners()) {
    minAreaRectCorners.emplace_back(t.x(), t.y());
  }

  std::vector<std::pair<double, double>> detectedCorners;
  detectedCorners.reserve(m->detected_corners_size());
  for (const auto& t : m->detected_corners()) {
    detectedCorners.emplace_back(t.x(), t.y());
  }

  return photon::PhotonTrackedTarget{
      m->yaw(),
      m->pitch(),
      m->area(),
      m->skew(),
      m->fiducial_id(),
      wpi::UnpackProtobuf<frc::Transform3d>(m->best_camera_to_target()),
      wpi::UnpackProtobuf<frc::Transform3d>(m->alt_camera_to_target()),
      m->pose_ambiguity(),
      minAreaRectCorners,
      detectedCorners};
}

void wpi::Protobuf<photon::PhotonTrackedTarget>::Pack(
    google::protobuf::Message* msg, const photon::PhotonTrackedTarget& value) {
  auto m = static_cast<photonvision::proto::ProtobufPhotonTrackedTarget*>(msg);

  m->set_yaw(value.yaw);
  m->set_pitch(value.pitch);
  m->set_area(value.area);
  m->set_skew(value.skew);
  m->set_fiducial_id(value.fiducialId);
  wpi::PackProtobuf(m->mutable_best_camera_to_target(),
                    value.bestCameraToTarget);
  wpi::PackProtobuf(m->mutable_alt_camera_to_target(), value.altCameraToTarget);
  m->set_pose_ambiguity(value.poseAmbiguity);

  m->clear_min_area_rect_corners();
  for (const auto& t : value.GetMinAreaRectCorners()) {
    auto* corner = m->add_min_area_rect_corners();
    corner->set_x(t.first);
    corner->set_y(t.second);
  }

  m->clear_detected_corners();
  for (const auto& t : value.GetDetectedCorners()) {
    auto* corner = m->add_detected_corners();
    corner->set_x(t.first);
    corner->set_y(t.second);
  }
}
