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

#include <map>
#include <optional>
#include <vector>

#include <gtsam/geometry/Cal3_S2.h>
#include <gtsam/geometry/Pose3.h>
#include <wpi/fields/Field.hpp>

#include "photon/estimation/TargetModel.h"

namespace photon::pvgtsam {

class FieldLayout {
 public:
  FieldLayout(
      const wpi::fields::Field& layout,
      const TargetModel& tagModel);  // Will throw if the the tag model does not
                                     // have exactly 4 vertices or is not planar

  /**
   * Get the world coordinates of the corners of a tag in our layout, given
   * its ID. Returns nullopt if the tag isn't in our layout
   */
  std::optional<std::array<gtsam::Point3, 4>> WorldToCorners(int id);

 private:
  std::map<int, gtsam::Pose3> worldTtags;
  std::array<gtsam::Point3, 4> tagToCorners;  // Offsets of tag corners in tag
                                              // frame, in "canonical" order
};
}  // namespace photon::pvgtsam
