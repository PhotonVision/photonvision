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

#pragma once
#include <array>
#include <utility>
#include <variant>
#include <vector>

#include <gtsam/geometry/Pose3.h>
#include <gtsam/linear/NoiseModel.h>
#include <gtsam/slam/expressions.h>
#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Transform3d.hpp>

// TODO: Remove CameraVisionObservation and OdometryObservation, and just use
// the AddOdometry and AddTagObservation methods directly.
namespace photon::pvgtsam {
struct CameraVisionObservation {
  // Tag observation timestamp
  uint64_t timeUs;
  // ID of observed tag, to later index into layout map
  int tagID;
  // Detected tag corners, in "canonical" order
  std::vector<gtsam::Point2> corners;
  // Calibration of camera observing this
  // TODO: see if I can switch this to a shared-ptr, not sure if that's any
  // faster
  // TODO: maybe just unprojecting points to pinhole -1,1 would mean we could
  // get rid of this entirely?
  gtsam::Cal3_S2_ cameraCal;
  // Offset from robot kinematic center -> camera optical center
  gtsam::Pose3 robotTcamera;
  // Pixel noise in camera
  gtsam::SharedNoiseModel cameraNoise;
};

struct OdometryObservation {
  uint64_t timeUs;
  gtsam::Pose3 poseDelta;
  gtsam::SharedNoiseModel odometryNoise;
};

enum class DataSubmissionType { Reset, Odometry, TagObservation };

struct ResetData {
  gtsam::Pose3 wTr;
  gtsam::SharedNoiseModel noise;
  uint64_t timeUs;
};

struct DataSubmission {
  DataSubmissionType type;
  std::variant<ResetData, OdometryObservation, CameraVisionObservation> data;
};

template <typename T>
struct Timestamped {
  uint64_t time;
  T value;
};

struct Pose3WithNoise {
  gtsam::Pose3 pose;
  gtsam::SharedNoiseModel noise;
};

gtsam::Pose3 Pose3dToGtsamPose3(wpi::math::Pose3d pose);
gtsam::Pose3 Transform3dToGtsamPose3(wpi::math::Transform3d pose);
wpi::math::Pose3d GtsamToFrcPose3d(gtsam::Pose3 pose);

gtsam::Point2_ PredictLandmarkImageLocation(gtsam::Pose3_ worldTbody_fac,
                                            gtsam::Pose3 bodyPcamera,
                                            gtsam::Cal3_S2_ cameraCal,
                                            gtsam::Point3 worldPcorner);

}  // namespace photon::pvgtsam
