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

#include <vector>

#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Rotation3d.hpp>
#include <wpi/math/geometry/Translation3d.hpp>

namespace photon {
class RotTrlTransform3d {
 public:
  RotTrlTransform3d(const wpi::math::Rotation3d& newRot,
                    const wpi::math::Translation3d& newTrl)
      : trl{newTrl}, rot{newRot} {}

  RotTrlTransform3d(const wpi::math::Pose3d& initial,
                    const wpi::math::Pose3d& last)
      : trl{last.Translation() - initial.Translation().RotateBy(
                                     last.Rotation().relativeTo(initial.Rotation()))},
        rot{last.Rotation() - initial.Rotation()} {}
  explicit RotTrlTransform3d(const wpi::math::Transform3d& trf)
      : RotTrlTransform3d(trf.Rotation(), trf.Translation()) {}
  RotTrlTransform3d()
      : RotTrlTransform3d(wpi::math::Rotation3d{}, wpi::math::Translation3d{}) {
  }

  static RotTrlTransform3d MakeRelativeTo(const wpi::math::Pose3d& pose) {
    return RotTrlTransform3d{pose.Rotation(), pose.Translation()}.Inverse();
  }

  RotTrlTransform3d Inverse() const {
    wpi::math::Rotation3d invRot = -rot;
    wpi::math::Translation3d invTrl = -(trl.RotateBy(invRot));
    return RotTrlTransform3d{invRot, invTrl};
  }

  wpi::math::Transform3d GetTransform() const {
    return wpi::math::Transform3d{trl, rot};
  }

  wpi::math::Translation3d GetTranslation() const { return trl; }

  wpi::math::Rotation3d GetRotation() const { return rot; }

  wpi::math::Translation3d Apply(
      const wpi::math::Translation3d& trlToApply) const {
    return trlToApply.RotateBy(rot) + trl;
  }

  std::vector<wpi::math::Translation3d> ApplyTrls(
      const std::vector<wpi::math::Translation3d>& trls) const {
    std::vector<wpi::math::Translation3d> retVal;
    retVal.reserve(trls.size());
    for (const auto& currentTrl : trls) {
      retVal.emplace_back(Apply(currentTrl));
    }
    return retVal;
  }

  wpi::math::Rotation3d Apply(const wpi::math::Rotation3d& rotToApply) const {
    return rotToApply + rot;
  }

  std::vector<wpi::math::Rotation3d> ApplyTrls(
      const std::vector<wpi::math::Rotation3d>& rots) const {
    std::vector<wpi::math::Rotation3d> retVal;
    retVal.reserve(rots.size());
    for (const auto& currentRot : rots) {
      retVal.emplace_back(Apply(currentRot));
    }
    return retVal;
  }

  wpi::math::Pose3d Apply(const wpi::math::Pose3d& poseToApply) const {
    return wpi::math::Pose3d{Apply(poseToApply.Translation()),
                             Apply(poseToApply.Rotation())};
  }

  std::vector<wpi::math::Pose3d> ApplyPoses(
      const std::vector<wpi::math::Pose3d>& poses) const {
    std::vector<wpi::math::Pose3d> retVal;
    retVal.reserve(poses.size());
    for (const auto& currentPose : poses) {
      retVal.emplace_back(Apply(currentPose));
    }
    return retVal;
  }

 private:
  const wpi::math::Translation3d trl;
  const wpi::math::Rotation3d rot;
};
}  // namespace photon
