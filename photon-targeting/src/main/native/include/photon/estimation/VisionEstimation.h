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

#include <utility>
#include <vector>

#include <Eigen/Core>
#include <frc/apriltag/AprilTag.h>
#include <frc/apriltag/AprilTagFieldLayout.h>

#include "OpenCVHelp.h"
#include "TargetModel.h"
#include "photon/targeting/MultiTargetPNPResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

namespace photon {
namespace VisionEstimation {

static std::vector<frc::AprilTag> GetVisibleLayoutTags(
    const std::vector<PhotonTrackedTarget>& visTags,
    const frc::AprilTagFieldLayout& layout) {
  std::vector<frc::AprilTag> retVal{};
  for (const auto& tag : visTags) {
    int id = tag.GetFiducialId();
    auto maybePose = layout.GetTagPose(id);
    if (maybePose) {
      retVal.emplace_back(frc::AprilTag{id, maybePose.value()});
    }
  }
  return retVal;
}

static PnpResult EstimateCamPosePNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const std::vector<PhotonTrackedTarget>& visTags,
    const frc::AprilTagFieldLayout& layout, const TargetModel& tagModel) {
  if (visTags.size() == 0) {
    return PnpResult();
  }

  std::vector<std::pair<float, float>> corners{};
  std::vector<frc::AprilTag> knownTags{};

  for (const auto& tgt : visTags) {
    int id = tgt.GetFiducialId();
    auto maybePose = layout.GetTagPose(id);
    if (maybePose) {
      knownTags.emplace_back(frc::AprilTag{id, maybePose.value()});
      auto currentCorners = tgt.GetDetectedCorners();
      corners.insert(corners.end(), currentCorners.begin(),
                     currentCorners.end());
    }
  }
  if (knownTags.size() == 0 || corners.size() == 0 || corners.size() % 4 != 0) {
    return PnpResult{};
  }

  std::vector<cv::Point2f> points = OpenCVHelp::CornersToPoints(corners);

  if (knownTags.size() == 1) {
    PnpResult camToTag = OpenCVHelp::SolvePNP_Square(
        cameraMatrix, distCoeffs, tagModel.GetVertices(), points);
    if (!camToTag.isPresent) {
      return PnpResult{};
    }
    frc::Pose3d bestPose =
        knownTags[0].pose.TransformBy(camToTag.best.Inverse());
    frc::Pose3d altPose{};
    if (camToTag.ambiguity != 0) {
      altPose = knownTags[0].pose.TransformBy(camToTag.alt.Inverse());
    }
    frc::Pose3d o{};
    PnpResult result{};
    result.best = frc::Transform3d{o, bestPose};
    result.alt = frc::Transform3d{o, altPose};
    result.ambiguity = camToTag.ambiguity;
    result.bestReprojErr = camToTag.bestReprojErr;
    result.altReprojErr = camToTag.altReprojErr;
    return result;
  } else {
    std::vector<frc::Translation3d> objectTrls{};
    for (const auto& tag : knownTags) {
      auto verts = tagModel.GetFieldVertices(tag.pose);
      objectTrls.insert(objectTrls.end(), verts.begin(), verts.end());
    }
    PnpResult camToOrigin = OpenCVHelp::SolvePNP_SQPNP(cameraMatrix, distCoeffs,
                                                       objectTrls, points);
    if (!camToOrigin.isPresent) {
      return PnpResult{};
    } else {
      PnpResult result{};
      result.best = camToOrigin.best.Inverse(),
      result.alt = camToOrigin.alt.Inverse(),
      result.ambiguity = camToOrigin.ambiguity;
      result.bestReprojErr = camToOrigin.bestReprojErr;
      result.altReprojErr = camToOrigin.altReprojErr;
      result.isPresent = true;
      return result;
    }
  }
}

}  // namespace VisionEstimation
}  // namespace photon
