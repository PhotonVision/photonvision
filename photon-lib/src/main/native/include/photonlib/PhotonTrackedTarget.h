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

#include <frc/geometry/Transform2d.h>
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
      double yaw, double pitch, double area, double skew,
      const frc::Transform2d& pose,
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
   * Returns the pose of the target relative to the robot.
   * @return The pose of the target relative to the robot.
   */
  frc::Transform2d GetCameraRelativePose() const { return cameraToTarget; }

  bool operator==(const PhotonTrackedTarget& other) const;
  bool operator!=(const PhotonTrackedTarget& other) const;

  friend Packet& operator<<(Packet& packet, const PhotonTrackedTarget& target);
  friend Packet& operator>>(Packet& packet, PhotonTrackedTarget& target);

 private:
  double yaw = 0;
  double pitch = 0;
  double area = 0;
  double skew = 0;
  frc::Transform2d cameraToTarget;
  wpi::SmallVector<std::pair<double, double>, 4> corners;
};
}  // namespace photonlib
