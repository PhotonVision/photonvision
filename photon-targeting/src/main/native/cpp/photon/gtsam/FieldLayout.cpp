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

#include "photon/gtsam/FieldLayout.h"

#include <stdexcept>

#include "photon/gtsam/gtsam_utils.h"

namespace photon::pvgtsam {

FieldLayout::FieldLayout(const wpi::fields::Field& layout,
                         const photon::TargetModel& tagModel) {
  if (tagModel.GetVertices().size() != 4) {
    throw std::runtime_error(
        "Tag model must have exactly 4 vertices for now, since we assume "
        "corners are in a specific order");
  }

  if (!tagModel.GetIsPlanar()) {
    throw std::runtime_error(
        "Tag model must be planar, since we assume corners are in a single "
        "plane");
  }

  auto tagVertices = tagModel.GetVertices();

  tagToCorners = {
      gtsam::Point3{tagVertices[0].X().value(), tagVertices[0].Y().value(),
                    tagVertices[0].Z().value()},
      gtsam::Point3{tagVertices[1].X().value(), tagVertices[1].Y().value(),
                    tagVertices[1].Z().value()},
      gtsam::Point3{tagVertices[2].X().value(), tagVertices[2].Y().value(),
                    tagVertices[2].Z().value()},
      gtsam::Point3{tagVertices[3].X().value(), tagVertices[3].Y().value(),
                    tagVertices[3].Z().value()},
  };

  for (const auto& tag : layout.GetTags()) {
    int id = tag.ID;
    const auto& pose = tag.pose;

    worldTtags[id] = Pose3dToGtsamPose3(pose);
  }
}

std::optional<std::array<gtsam::Point3, 4>> FieldLayout::WorldToCorners(
    int id) {
  auto maybePose = worldTtags.find(id);
  if (maybePose == worldTtags.end()) {
    return std::nullopt;
  }

  const gtsam::Pose3 worldTtag = maybePose->second;

  std::array<gtsam::Point3, 4> worldToCorners{};

  std::transform(
      tagToCorners.begin(), tagToCorners.end(), worldToCorners.begin(),
      [&worldTtag](const auto& p) { return worldTtag.transformFrom(p); });

  return worldToCorners;
}

}  // namespace photon::pvgtsam
