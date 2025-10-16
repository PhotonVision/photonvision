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
