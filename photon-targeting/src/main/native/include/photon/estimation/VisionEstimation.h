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

#include <Eigen/Core>
#include <wpi/apriltag/AprilTag.hpp>
#include <wpi/apriltag/AprilTagFieldLayout.hpp>

#include "TargetModel.h"
#include "photon/targeting/PhotonTrackedTarget.h"
#include "photon/targeting/PnpResult.h"

namespace photon {
namespace VisionEstimation {

std::vector<wpi::apriltag::AprilTag> GetVisibleLayoutTags(
    const std::vector<PhotonTrackedTarget>& visTags,
    const wpi::apriltag::AprilTagFieldLayout& layout);

std::optional<photon::PnpResult> EstimateCamPosePNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const std::vector<PhotonTrackedTarget>& visTags,
    const wpi::apriltag::AprilTagFieldLayout& layout,
    const TargetModel& tagModel);

std::optional<photon::PnpResult> EstimateRobotPoseConstrainedSolvePNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const std::vector<photon::PhotonTrackedTarget>& visTags,
    const wpi::math::Transform3d& robot2Camera,
    const wpi::math::Pose3d& robotPoseSeed,
    const wpi::apriltag::AprilTagFieldLayout& layout,
    const photon::TargetModel& tagModel, bool headingFree,
    wpi::math::Rotation2d gyroTheta, double gyroErrorScaleFac);

}  // namespace VisionEstimation
}  // namespace photon
