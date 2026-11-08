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

#include "photon/gtsam/ConcurrentLocalizer.h"

#include <utility>
#include <vector>

namespace photon::pvgtsam {

void ConcurrentLocalizer::SubmitReset(ResetData data) {
  Enqueue(DataSubmission{DataSubmissionType::Reset, data});
}

void ConcurrentLocalizer::SubmitOdometry(OdometryObservation odom) {
  Enqueue(DataSubmission{DataSubmissionType::Odometry, odom});
}

void ConcurrentLocalizer::SubmitTagObservation(CameraVisionObservation obs) {
  Enqueue(DataSubmission{DataSubmissionType::TagObservation, obs});
}

void ConcurrentLocalizer::Enqueue(DataSubmission submission) {
  std::lock_guard<std::mutex> lock(queue_mtx);
  submissionQueue.push(std::move(submission));
}

void ConcurrentLocalizer::Process(const DataSubmission& submission) {
  switch (submission.type) {
    case DataSubmissionType::Reset: {
      const ResetData& data = std::get<ResetData>(submission.data);
      core.Reset(data);
      break;
    }
    case DataSubmissionType::Odometry: {
      const OdometryObservation& odom =
          std::get<OdometryObservation>(submission.data);
      core.AddOdometry(odom);
      break;
    }
    case DataSubmissionType::TagObservation: {
      const CameraVisionObservation& obs =
          std::get<CameraVisionObservation>(submission.data);
      core.AddTagObservation(obs);
      break;
    }
  }
}

void ConcurrentLocalizer::Step() {
  {
    std::lock_guard<std::mutex> data_lock(queue_mtx);
    std::lock_guard<std::mutex> isam_lock(isam_mtx);
    while (!submissionQueue.empty()) {
      DataSubmission submission = std::move(submissionQueue.front());
      submissionQueue.pop();
      Process(submission);
    }
  }
  std::lock_guard lock(isam_mtx);
  core.Optimize();
}

void ConcurrentLocalizer::Print(const std::string_view prefix) {
  std::lock_guard lock(isam_mtx);
  core.Print(prefix);
}

gtsam::Matrix ConcurrentLocalizer::GetLatestMarginals() const {
  std::lock_guard lock(isam_mtx);
  return core.GetLatestMarginals();
}

gtsam::Vector6 ConcurrentLocalizer::GetPoseComponentStdDevs() const {
  std::lock_guard lock(isam_mtx);
  return core.GetPoseComponentStdDevs();
}

std::vector<wpi::math::Pose3d> ConcurrentLocalizer::GetPoseHistory() const {
  std::lock_guard lock(isam_mtx);
  return core.GetPoseHistory();
}

}  // namespace photon::pvgtsam
