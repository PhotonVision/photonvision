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
