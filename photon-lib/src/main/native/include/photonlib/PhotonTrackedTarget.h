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
   * @Param corners The corners of the bounding rectangle.
   */
  PhotonTrackedTarget(
      double yaw, double pitch, double area, double skew, int fiducialID,
      const frc::Transform3d& pose,
      const wpi::SmallVector<std::pair<double, double>, 4> corners);

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
   * Returns the corners of the minimum area rectangle bounding this target.
   */
  wpi::SmallVector<std::pair<double, double>, 4> GetCorners() const {
    return corners;
  }

  /**
   * Get the ratio of pose reprojection errors, called ambiguity. Numbers above
   * 0.2 are likely to be ambiguous. -1 if invalid.
   */
  double GetPoseAmbiguity() const { return poseAmbiguity; }

  /**
   * Returns the pose of the target relative to the robot.
   * @return The pose of the target relative to the robot.
   */
  frc::Transform3d GetCameraToTarget() const { return cameraToTarget; }

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
  frc::Transform3d cameraToTarget;
  double poseAmbiguity;
  wpi::SmallVector<std::pair<double, double>, 4> corners;
};
}  // namespace photonlib
