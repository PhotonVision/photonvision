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

#include <Eigen/Core>
#include <sleipnir/optimization/SolverExitCondition.hpp>
#include <wpi/expected>

namespace constrained_solvepnp {
using casadi_real = double;
/**
 * Pinhole camera coefficients
 */
struct CameraCalibration {
  casadi_real fx;
  casadi_real fy;
  casadi_real cx;
  casadi_real cy;
};

using RobotStateMat = Eigen::Matrix<casadi_real, 3, 1>;

/**
 * Optimize x, where x is [x, y, theta]^T. Note points must be undistorted prior
 * to this. The number of columns in field2points and point_observations just be
 * exactly 4x nTags.
 */
wpi::expected<RobotStateMat, sleipnir::SolverExitCondition> do_optimization(
    bool heading_free, int nTags, CameraCalibration cameraCal,
    // Note that casadi is column major, apparently
    Eigen::Matrix<casadi_real, 4, 4, Eigen::ColMajor> robot2camera,
    RobotStateMat x_guess,
    Eigen::Matrix<casadi_real, 4, Eigen::Dynamic, Eigen::ColMajor> field2points,
    Eigen::Matrix<casadi_real, 2, Eigen::Dynamic, Eigen::ColMajor>
        point_observations,
    double gyroθ, double gyroErrorScaleFac);

}  // namespace constrained_solvepnp
