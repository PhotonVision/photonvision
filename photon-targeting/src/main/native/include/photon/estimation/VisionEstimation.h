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
#include <frc/apriltag/AprilTag.h>
#include <frc/apriltag/AprilTagFieldLayout.h>

#include "TargetModel.h"
#include "photon/targeting/PhotonTrackedTarget.h"
#include "photon/targeting/PnpResult.h"

namespace photon {
namespace VisionEstimation {

std::vector<frc::AprilTag> GetVisibleLayoutTags(
    const std::vector<PhotonTrackedTarget>& visTags,
    const frc::AprilTagFieldLayout& layout);

std::optional<photon::PnpResult> EstimateCamPosePNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const std::vector<PhotonTrackedTarget>& visTags,
    const frc::AprilTagFieldLayout& layout, const TargetModel& tagModel);

std::optional<photon::PnpResult> EstimateRobotPoseConstrainedSolvePNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const std::vector<photon::PhotonTrackedTarget>& visTags,
    const frc::Transform3d& robot2Camera, const frc::Pose3d& robotPoseSeed,
    const frc::AprilTagFieldLayout& layout, const photon::TargetModel& tagModel,
    bool headingFree, frc::Rotation2d gyroTheta, double gyroErrorScaleFac);

}  // namespace VisionEstimation
}  // namespace photon
