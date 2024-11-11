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

#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Rotation3d.h>
#include <frc/geometry/Translation3d.h>

namespace photon {
class RotTrlTransform3d {
 public:
  RotTrlTransform3d(const frc::Rotation3d& newRot,
                    const frc::Translation3d& newTrl)
      : trl{newTrl}, rot{newRot} {}

  RotTrlTransform3d(const frc::Pose3d& initial, const frc::Pose3d& last)
      : trl{last.Translation() - initial.Translation().RotateBy(
                                     last.Rotation() - initial.Rotation())},
        rot{last.Rotation() - initial.Rotation()} {}
  explicit RotTrlTransform3d(const frc::Transform3d& trf)
      : RotTrlTransform3d(trf.Rotation(), trf.Translation()) {}
  RotTrlTransform3d()
      : RotTrlTransform3d(frc::Rotation3d{}, frc::Translation3d{}) {}

  static RotTrlTransform3d MakeRelativeTo(const frc::Pose3d& pose) {
    return RotTrlTransform3d{pose.Rotation(), pose.Translation()}.Inverse();
  }

  RotTrlTransform3d Inverse() const {
    frc::Rotation3d invRot = -rot;
    frc::Translation3d invTrl = -(trl.RotateBy(invRot));
    return RotTrlTransform3d{invRot, invTrl};
  }

  frc::Transform3d GetTransform() const { return frc::Transform3d{trl, rot}; }

  frc::Translation3d GetTranslation() const { return trl; }

  frc::Rotation3d GetRotation() const { return rot; }

  frc::Translation3d Apply(const frc::Translation3d& trlToApply) const {
    return trlToApply.RotateBy(rot) + trl;
  }

  std::vector<frc::Translation3d> ApplyTrls(
      const std::vector<frc::Translation3d>& trls) const {
    std::vector<frc::Translation3d> retVal;
    retVal.reserve(trls.size());
    for (const auto& currentTrl : trls) {
      retVal.emplace_back(Apply(currentTrl));
    }
    return retVal;
  }

  frc::Rotation3d Apply(const frc::Rotation3d& rotToApply) const {
    return rotToApply + rot;
  }

  std::vector<frc::Rotation3d> ApplyTrls(
      const std::vector<frc::Rotation3d>& rots) const {
    std::vector<frc::Rotation3d> retVal;
    retVal.reserve(rots.size());
    for (const auto& currentRot : rots) {
      retVal.emplace_back(Apply(currentRot));
    }
    return retVal;
  }

  frc::Pose3d Apply(const frc::Pose3d& poseToApply) const {
    return frc::Pose3d{Apply(poseToApply.Translation()),
                       Apply(poseToApply.Rotation())};
  }

  std::vector<frc::Pose3d> ApplyPoses(
      const std::vector<frc::Pose3d>& poses) const {
    std::vector<frc::Pose3d> retVal;
    retVal.reserve(poses.size());
    for (const auto& currentPose : poses) {
      retVal.emplace_back(Apply(currentPose));
    }
    return retVal;
  }

 private:
  const frc::Translation3d trl;
  const frc::Rotation3d rot;
};
}  // namespace photon
