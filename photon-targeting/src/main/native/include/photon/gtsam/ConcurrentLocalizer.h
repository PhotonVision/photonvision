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

// We keep this in Photontargeting rather than photonlib so that there is one
// implementation of multithreading behavior across all languages

#pragma once

#include <mutex>
#include <queue>
#include <string_view>
#include <utility>
#include <vector>

#include <wpi/math/geometry/Pose3d.hpp>

#include "photon/gtsam/LocalizerCore.h"

namespace photon::pvgtsam {

class ConcurrentLocalizer {
 public:
  explicit ConcurrentLocalizer(const wpi::fields::Field& layout,
                               const TargetModel& tagModel)
      : core(layout, tagModel) {}

  explicit ConcurrentLocalizer(FieldLayout fieldLayout)
      : core(std::move(fieldLayout)) {}

  /**
   * Threadsafe way to reset
   */
  void SubmitReset(ResetData data);

  /**
   * Threadsafe way to submit odometry
   */
  void SubmitOdometry(OdometryObservation odom);

  void SubmitTagObservation(CameraVisionObservation obs);

  /**
   * The main loop function. When multithreading, this function should be called
   * in the worker thread
   */
  void Step();

  /**
   * Should be safe to run multithreaded? Use at your own risk
   */
  void Print(const std::string_view prefix = "");

  inline gtsam::Pose3 GetLatestWorldToBody() const {
    return core.GetLatestWorldToBody();
  }

  gtsam::Matrix GetLatestMarginals() const;
  // standard deviations on rx ry rz tx ty tz
  gtsam::Vector6 GetPoseComponentStdDevs() const;

  inline gtsam::Key GetCurrStateIdx() const { return core.GetCurrStateIdx(); }

  inline uint64_t GetLastOdomTime() const { return core.GetLastOdomTime(); }

  std::vector<wpi::math::Pose3d> GetPoseHistory() const;

 protected:
  void Enqueue(DataSubmission submission);
  void Process(const DataSubmission& submission);

  LocalizerCore core;

  mutable std::mutex queue_mtx;
  mutable std::mutex isam_mtx;

  std::queue<DataSubmission> submissionQueue;
};

}  // namespace photon::pvgtsam
