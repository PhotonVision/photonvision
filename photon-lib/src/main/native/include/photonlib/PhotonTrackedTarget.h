/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

#include <cstddef>
#include <string>
#include <utility>
#include <vector>

#include <frc/geometry/Transform3d.h>
#include <wpi/SmallVector.h>

#include "photonlib/Packet.h"

namespace photonlib {
/**
 * Represents a tracked target within a pipeline.
 */
class PhotonTrackedTarget {
 public:
  /**
   * Constructs an empty target.
   */
  PhotonTrackedTarget() = default;

  /**
   * Constructs a target.
   * @param yaw The yaw of the target.
   * @param pitch The pitch of the target.
   * @param area The area of the target.
   * @param skew The skew of the target.
   * @param pose The camera-relative pose of the target.
   * @param alternatePose The alternate camera-relative pose of the target.
   * @Param corners The corners of the bounding rectangle.
   */
  PhotonTrackedTarget(
      double yaw, double pitch, double area, double skew, int fiducialID,
      const frc::Transform3d& pose, const frc::Transform3d& alternatePose,
      double ambiguity,
      const wpi::SmallVector<std::pair<double, double>, 4> corners,
      const std::vector<std::pair<double, double>> detectedCorners);

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
   * Return a list of the 4 corners in image space (origin top left, x right, y
   * down), in no particular order, of the minimum area bounding rectangle of
   * this target
   */
  wpi::SmallVector<std::pair<double, double>, 4> GetMinAreaRectCorners() const {
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
  std::vector<std::pair<double, double>> GetDetectedCorners() {
    return detectedCorners;
  }

  /**
   * Get the ratio of pose reprojection errors, called ambiguity. Numbers above
   * 0.2 are likely to be ambiguous. -1 if invalid.
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

  bool operator==(const PhotonTrackedTarget& other) const;
  bool operator!=(const PhotonTrackedTarget& other) const;

  friend Packet& operator<<(Packet& packet, const PhotonTrackedTarget& target);
  friend Packet& operator>>(Packet& packet, PhotonTrackedTarget& target);

 private:
  double yaw = 0;
  double pitch = 0;
  double area = 0;
  double skew = 0;
  int fiducialId;
  frc::Transform3d bestCameraToTarget;
  frc::Transform3d altCameraToTarget;
  double poseAmbiguity;
  wpi::SmallVector<std::pair<double, double>, 4> minAreaRectCorners;
  std::vector<std::pair<double, double>> detectedCorners;
};
}  // namespace photonlib
