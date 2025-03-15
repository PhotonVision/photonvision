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

[[maybe_unused]] static std::vector<frc::AprilTag> GetVisibleLayoutTags(
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

#include <iostream>

[[maybe_unused]] static std::optional<PnpResult> EstimateCamPosePNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const std::vector<PhotonTrackedTarget>& visTags,
    const frc::AprilTagFieldLayout& layout, const TargetModel& tagModel) {
  if (visTags.size() == 0) {
    return PnpResult();
  }

  std::vector<photon::TargetCorner> corners{};
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
    auto camToTag = OpenCVHelp::SolvePNP_Square(cameraMatrix, distCoeffs,
                                                tagModel.GetVertices(), points);
    if (!camToTag) {
      return PnpResult{};
    }
    frc::Pose3d bestPose =
        knownTags[0].pose.TransformBy(camToTag->best.Inverse());
    frc::Pose3d altPose{};
    if (camToTag->ambiguity != 0) {
      altPose = knownTags[0].pose.TransformBy(camToTag->alt.Inverse());
    }
    frc::Pose3d o{};
    PnpResult result{};
    result.best = frc::Transform3d{o, bestPose};
    result.alt = frc::Transform3d{o, altPose};
    result.ambiguity = camToTag->ambiguity;
    result.bestReprojErr = camToTag->bestReprojErr;
    result.altReprojErr = camToTag->altReprojErr;
    return result;
  } else {
    std::vector<frc::Translation3d> objectTrls{};
    for (const auto& tag : knownTags) {
      auto verts = tagModel.GetFieldVertices(tag.pose);
      objectTrls.insert(objectTrls.end(), verts.begin(), verts.end());
    }
    auto ret = OpenCVHelp::SolvePNP_SQPNP(cameraMatrix, distCoeffs, objectTrls,
                                          points);
    if (ret) {
      // Invert best/alt transforms
      ret->best = ret->best.Inverse();
      ret->alt = ret->alt.Inverse();
    }

    return ret;
  }
}

}  // namespace VisionEstimation
}  // namespace photon
