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
/** Describes a vision target located somewhere on the field that your vision
 * system can detect. */
class VisionTargetSim {
 public:
  /**
   * Describes a retro-reflective/colored shape vision target located somewhere
   * on the field that your vision system can detect.
   *
   * @param pose Pose3d of the tag in field-relative coordinates
   * @param model TargetModel which describes the geometry of the target
   */
  VisionTargetSim(const frc::Pose3d& pose, const TargetModel& model)
      : fiducialId(-1),
        objDetClassId(-1),
        objDetConf(-1),
        pose(pose),
        model(model) {}

  /**
   * Describes a fiducial tag located somewhere on the field that your vision
   * system can detect.
   *
   * @param pose Pose3d of the tag in field-relative coordinates
   * @param model TargetModel which describes the geometry of the target(tag)
   * @param id The ID of this fiducial tag
   */
  VisionTargetSim(const frc::Pose3d& pose, const TargetModel& model, int id)
      : fiducialId(id),
        objDetClassId(-1),
        objDetConf(-1),
        pose(pose),
        model(model) {}

  /**
   * Describes an object-detection vision target located somewhere on the field
   * that your vision system can detect. Class ID is the (zero-indexed) index of
   * the object's class ID in the list of all classes. Confidence can be
   * specified, or pass -1 to estimate confidence based on 2 * sqrt(target area
   * / total image area)
   *
   * @param pose Pose3d of the target in field-relative coordinates
   * @param model TargetModel which describes the geometry of the target
   * @param objDetClassId The object detection class ID, if -1 it will not be
   * detected by object detection
   * @param objDetConf The object detection confidence, or -1 in which case the
   * simulation will compute a confidence based on the area of the target in the
   * camera's field of view
   */
  VisionTargetSim(const frc::Pose3d& pose, const TargetModel& model,
                  int objDetClassId, float objDetConf)
      : fiducialId(-1),
        objDetClassId(objDetClassId),
        objDetConf(objDetConf),
        pose(pose),
        model(model) {}

  /**
   * Sets the pose of this target on the field.
   *
   * @param newPose The pose in field-relative coordinates
   */
  void SetPose(const frc::Pose3d& newPose) { pose = newPose; }

  /**
   * Sets the model describing this target's geometry.
   *
   * @param newModel The model of the target
   */
  void SetModel(const TargetModel& newModel) { model = newModel; }

  /**
   * Returns the pose of this target on the field.
   *
   * @return The pose in field-relative coordinates
   */
  frc::Pose3d GetPose() const { return pose; }

  /**
   * Returns the model describing this target's geometry.
   *
   * @return The model of the target
   */
  TargetModel GetModel() const { return model; }

  /**
   * This target's vertices offset from its field pose.
   * @return A vector of Translation3d representing the vertices of the target
   */
  std::vector<frc::Translation3d> GetFieldVertices() const {
    return model.GetFieldVertices(pose);
  }

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
  int fiducialId;
  int objDetClassId;
  float objDetConf;
  frc::Pose3d pose;
  TargetModel model;
};
}  // namespace photon
