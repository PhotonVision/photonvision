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

#include "photon/gtsam/gtsam_utils.h"

using namespace gtsam;

namespace photon::pvgtsam {
Pose3 Pose3dToGtsamPose3(wpi::math::Pose3d pose) {
  const auto q = pose.Rotation().GetQuaternion();
  return Pose3{Rot3(q.W(), q.X(), q.Y(), q.Z()),
               Point3(pose.X().to<double>(), pose.Y().to<double>(),
                      pose.Z().to<double>())};
}

Pose3 Transform3dToGtsamPose3(wpi::math::Transform3d pose) {
  const auto q = pose.Rotation().GetQuaternion();
  return Pose3{Rot3(q.W(), q.X(), q.Y(), q.Z()),
               Point3(pose.X().to<double>(), pose.Y().to<double>(),
                      pose.Z().to<double>())};
}

gtsam::Point2_ PredictLandmarkImageLocation(gtsam::Pose3_ worldTbody_fac,
                                            gtsam::Pose3 bodyPcamera,
                                            gtsam::Cal3_S2_ cameraCal,
                                            gtsam::Point3 worldPcorner) {
  using namespace gtsam;

  // world->camera pose as a composition of world->body factory and
  // body->camera factor
  const Pose3_ worldTcamera_fac =
      Pose3_(worldTbody_fac, &Pose3::transformPoseFrom, Pose3_(bodyPcamera));
  // Camera->tag corner vector
  const Point3_ camPcorner = transformTo(worldTcamera_fac, worldPcorner);
  // project from vector down to pinhole location, then uncalibrate to pixel
  // locations
  const Point2_ prediction =
      uncalibrate<Cal3_S2>(cameraCal, project(camPcorner));

  return prediction;
}

wpi::math::Pose3d GtsamToFrcPose3d(gtsam::Pose3 pose) {
  return wpi::math::Pose3d{
      wpi::math::Translation3d{wpi::units::meter_t{pose.x()},
                               wpi::units::meter_t{pose.y()},
                               wpi::units::meter_t{pose.z()}},
      wpi::math::Rotation3d{pose.rotation().matrix()}};
}
}  // namespace photon::pvgtsam
