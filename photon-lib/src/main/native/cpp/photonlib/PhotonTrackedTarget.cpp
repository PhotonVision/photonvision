/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

#include "photonlib/PhotonTrackedTarget.h"

#include <iostream>
#include <utility>

#include <frc/geometry/Translation2d.h>
#include <wpi/SmallVector.h>

#include "photon_types.pb.h"

static constexpr const uint8_t MAX_CORNERS = 8;

namespace photonlib {

PhotonTrackedTarget::PhotonTrackedTarget(
    double yaw, double pitch, double area, double skew, int id,
    const frc::Transform3d& pose, const frc::Transform3d& alternatePose,
    double ambiguity,
    const wpi::SmallVector<std::pair<double, double>, 4> minAreaRectCorners,
    const std::vector<std::pair<double, double>> detectedCorners)
    : yaw(yaw),
      pitch(pitch),
      area(area),
      skew(skew),
      fiducialId(id),
      bestCameraToTarget(pose),
      altCameraToTarget(alternatePose),
      poseAmbiguity(ambiguity),
      minAreaRectCorners(minAreaRectCorners),
      detectedCorners(detectedCorners) {}

bool PhotonTrackedTarget::operator==(const PhotonTrackedTarget& other) const {
  return other.yaw == yaw && other.pitch == pitch && other.area == area &&
         other.skew == skew && other.bestCameraToTarget == bestCameraToTarget &&
         other.minAreaRectCorners == minAreaRectCorners;
}

bool PhotonTrackedTarget::operator!=(const PhotonTrackedTarget& other) const {
  return !operator==(other);
}

}  // namespace photonlib

google::protobuf::Message* wpi::Protobuf<photonlib::PhotonTrackedTarget>::New(
    google::protobuf::Arena* arena) {
  return google::protobuf::Arena::CreateMessage<
      photonvision::proto::ProtobufPhotonTrackedTarget>(arena);
}

photonlib::PhotonTrackedTarget
wpi::Protobuf<photonlib::PhotonTrackedTarget>::Unpack(
    const google::protobuf::Message& msg) {
  using namespace photonlib;
  using photonvision::proto::ProtobufPhotonTrackedTarget;

  auto m = static_cast<const ProtobufPhotonTrackedTarget*>(&msg);

  wpi::SmallVector<std::pair<double, double>, 4> minAreaRectCorners;
  for (const auto& t : m->minarearectcorners()) {
    minAreaRectCorners.emplace_back(t.x(), t.y());
  }

  std::vector<std::pair<double, double>> detectedCorners;
  detectedCorners.reserve(m->detectedcorners_size());
  for (const auto& t : m->detectedcorners()) {
    detectedCorners.emplace_back(t.x(), t.y());
  }

  return photonlib::PhotonTrackedTarget(
      m->yaw(), m->pitch(), m->area(), m->skew(), m->fiducialid(),
      wpi::UnpackProtobuf<frc::Transform3d>(m->bestcameratotarget()),
      wpi::UnpackProtobuf<frc::Transform3d>(m->altcameratotarget()),
      m->poseambiguity(), minAreaRectCorners, detectedCorners);
}

void wpi::Protobuf<photonlib::PhotonTrackedTarget>::Pack(
    google::protobuf::Message* msg,
    const photonlib::PhotonTrackedTarget& value) {
  using namespace photonlib;
  using photonvision::proto::ProtobufPhotonTrackedTarget;

  auto m = static_cast<ProtobufPhotonTrackedTarget*>(msg);

#define SET(proto, getter) m->set_##proto(value.Get##getter())
  SET(yaw, Yaw);
  SET(pitch, Yaw);
  SET(area, Area);
  SET(skew, Skew);
  SET(fiducialid, FiducialId);
  SET(poseambiguity, PoseAmbiguity);
#undef SET

  m->clear_minarearectcorners();
  for (const auto& t : value.GetMinAreaRectCorners()) {
    auto* corner = m->add_minarearectcorners();
    corner->set_x(t.first);
    corner->set_y(t.second);
  }

  m->clear_detectedcorners();
  for (const std::pair<double, double>& t : value.GetDetectedCorners()) {
    auto* corner = m->add_detectedcorners();
    corner->set_x(t.first);
    corner->set_y(t.second);
  }

  wpi::PackProtobuf(m->mutable_bestcameratotarget(),
                    value.GetBestCameraToTarget());
  wpi::PackProtobuf(m->mutable_altcameratotarget(),
                    value.GetAlternateCameraToTarget());

  // TODO -- multi-target
}
