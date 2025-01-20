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

#include <cstddef>
#include <string>
#include <utility>
#include <vector>

#include <frc/geometry/Transform3d.h>
#include <wpi/SmallVector.h>

#include "photon/struct/PhotonTrackedTargetStruct.h"

namespace photon {
/**
 * Represents a tracked target within a pipeline.
 */
class PhotonTrackedTarget : public PhotonTrackedTarget_PhotonStruct {
  using Base = PhotonTrackedTarget_PhotonStruct;

 public:
  PhotonTrackedTarget() = default;

  explicit PhotonTrackedTarget(Base&& data) : Base(data) {}

  template <typename... Args>
  explicit PhotonTrackedTarget(Args&&... args)
      : Base{std::forward<Args>(args)...} {}

  /**
   * Returns the target yaw (positive-left).
   * @return The target yaw.
   */
  double GetYaw() const { return yaw; }

  /**
   * Returns the target pitch (positive-up)
   * @return The target pitch.
   */
  double GetPitch() const { return pitch; }

  /**
   * Returns the target area (0-100).
   * @return The target area.
   */
  double GetArea() const { return area; }

  /**
   * Returns the target skew (counter-clockwise positive).
   * @return The target skew.
   */
  double GetSkew() const { return skew; }

  /**
   * Get the Fiducial ID of the target currently being tracked,
   * or -1 if not set.
   */
  int GetFiducialId() const { return fiducialId; }

  /**
   * Get the Fiducial ID of the target currently being tracked,
   * or -1 if not set.
   */
  int GetDetectedObjectClassID() const { return objDetectId; }

  /**
   * Get the object detection confidence, or -1 if not set. This will be between
   * 0 and 1, with 1 indicating most confidence, and 0 least.
   */
  float GetDetectedObjectConfidence() const { return objDetectConf; }

  /**
   * Return a list of the 4 corners in image space (origin top left, x right, y
   * down), in no particular order, of the minimum area bounding rectangle of
   * this target
   */
  const std::vector<photon::TargetCorner>& GetMinAreaRectCorners() const {
    return minAreaRectCorners;
  }

  /**
   * Return a list of the n corners in image space (origin top left, x right, y
   * down), in no particular order, detected for this target.
   * For fiducials, the order is known and is always counter-clock wise around
   * the tag, like so
   *
   * -> +X     3 ----- 2
   * |         |       |
   * V + Y     |       |
   *           0 ----- 1
   */
  const std::vector<photon::TargetCorner>& GetDetectedCorners() const {
    return detectedCorners;
  }

  /**
   * Get the ratio of best:alternate pose reprojection errors, called ambiguity.
   * This is between 0 and 1 (0 being no ambiguity, and 1 meaning both have the
   * same reprojection error). Numbers above 0.2 are likely to be ambiguous. -1
   * if invalid.
   */
  double GetPoseAmbiguity() const { return poseAmbiguity; }

  /**
   * Get the transform that maps camera space (X = forward, Y = left, Z = up) to
   * object/fiducial tag space (X forward, Y left, Z up) with the lowest
   * reprojection error. The ratio between this and the alternate target's
   * reprojection error is the ambiguity, which is between 0 and 1.
   * @return The pose of the target relative to the robot.
   */
  frc::Transform3d GetBestCameraToTarget() const { return bestCameraToTarget; }

  /**
   * Get the transform that maps camera space (X = forward, Y = left, Z = up) to
   * object/fiducial tag space (X forward, Y left, Z up) with the highest
   * reprojection error
   */
  frc::Transform3d GetAlternateCameraToTarget() const {
    return altCameraToTarget;
  }

  friend bool operator==(PhotonTrackedTarget const&,
                         PhotonTrackedTarget const&) = default;
};
}  // namespace photon

#include "photon/serde/PhotonTrackedTargetSerde.h"
