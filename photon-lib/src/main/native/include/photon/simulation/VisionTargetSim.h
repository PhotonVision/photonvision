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

#pragma once

#include <vector>

#include <frc/geometry/Pose3d.h>

#include "photon/estimation/TargetModel.h"

namespace photon {
class VisionTargetSim {
 public:
  VisionTargetSim(const frc::Pose3d& pose, const TargetModel& model)
      : fiducialId(-1), pose(pose), model(model) {}
  VisionTargetSim(const frc::Pose3d& pose, const TargetModel& model, int id)
      : fiducialId(id), pose(pose), model(model) {}
  void SetPose(const frc::Pose3d& newPose) { pose = newPose; }
  void SetModel(const TargetModel& newModel) { model = newModel; }
  frc::Pose3d GetPose() const { return pose; }
  TargetModel GetModel() const { return model; }
  std::vector<frc::Translation3d> GetFieldVertices() const {
    return model.GetFieldVertices(pose);
  }
  int fiducialId;

  int objDetClassId = -1;
  float objDetConf = -1;

  bool operator<(const VisionTargetSim& right) const {
    return pose.Translation().Norm() < right.pose.Translation().Norm();
  }

  bool operator==(const VisionTargetSim& other) const {
    return units::math::abs(pose.Translation().X() -
                            other.GetPose().Translation().X()) < 1_in &&
           units::math::abs(pose.Translation().Y() -
                            other.GetPose().Translation().Y()) < 1_in &&
           units::math::abs(pose.Translation().Z() -
                            other.GetPose().Translation().Z()) < 1_in &&
           units::math::abs(pose.Rotation().X() -
                            other.GetPose().Rotation().X()) < 1_deg &&
           units::math::abs(pose.Rotation().Y() -
                            other.GetPose().Rotation().Y()) < 1_deg &&
           units::math::abs(pose.Rotation().Z() -
                            other.GetPose().Rotation().Z()) < 1_deg &&
           model.GetIsPlanar() == other.GetModel().GetIsPlanar();
  }

 private:
  frc::Pose3d pose;
  TargetModel model;
};
}  // namespace photon
