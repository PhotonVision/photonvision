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

#include <gtest/gtest.h>

#include <chrono>
#include <cstdio>
#include <iostream>
#include <vector>

#include <wpi/timestamp.h>

#include "photon/constrained_solvepnp/wrap/casadi_wrapper.h"

#define TAG_COUNT 6
#if TAG_COUNT < 1
#error TAG_COUNT cannot be less than 1!
#endif

using casadi_real = double;

void print_cost(casadi_real robot_x, casadi_real robot_y,
                casadi_real robot_theta) {
  casadi_real fx = 600;
  casadi_real fy = 600;
  casadi_real cx = 300;
  casadi_real cy = 150;

  constexpr int NUM_LANDMARKS = 4 * TAG_COUNT;

  // Note that casadi is column major, apparently
  Eigen::Matrix<casadi_real, 4, 4, Eigen::ColMajor> robot2camera;
  // clang-format off
  robot2camera <<
    0, 0, 1, 0,
    -1, 0, 0, 0,
    0, -1, 0, 0,
    0, 0, 0, 1;
  // clang-format on

  Eigen::Matrix<casadi_real, NUM_LANDMARKS, 4, Eigen::ColMajor> field2points_;
  // clang-format off
    field2points_ <<
        #if TAG_COUNT > 7
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        #if TAG_COUNT > 6
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        #if TAG_COUNT > 5
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        #if TAG_COUNT > 4
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        #if TAG_COUNT > 3
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        #if TAG_COUNT > 2
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        #if TAG_COUNT > 1
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1,
        #endif
        2.5, 0 - 0.08255, 0.5 - 0.08255, 1,
        2.5, 0 - 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 + 0.08255, 1,
        2.5, 0 + 0.08255, 0.5 - 0.08255, 1;
  // clang-format on
  Eigen::Matrix<casadi_real, 4, NUM_LANDMARKS, Eigen::ColMajor> field2points =
      field2points_.transpose();

  Eigen::Matrix<casadi_real, NUM_LANDMARKS, 2, Eigen::ColMajor>
      point_observations_;
  // clang-format off
    point_observations_ <<
        #if TAG_COUNT > 7
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        #if TAG_COUNT > 6
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        #if TAG_COUNT > 5
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        #if TAG_COUNT > 4
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        #if TAG_COUNT > 3
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        #if TAG_COUNT > 2
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        #if TAG_COUNT > 1
        333, -17,
        333, -83,
        267, -83,
        267, -17,
        #endif
        333, -17,
        333, -83,
        267, -83,
        267, -17;
  // clang-format on
  Eigen::Matrix<casadi_real, 2, NUM_LANDMARKS, Eigen::ColMajor>
      point_observations = point_observations_.transpose();

  Eigen::Vector3d x_guess;
  x_guess << robot_x, robot_y, robot_theta;

  for (int i = 0; i < 10; i++) {
    auto start = wpi::Now();
    auto x_out = constrained_solvepnp::do_optimization(
        true, TAG_COUNT,
        constrained_solvepnp::CameraCalibration{fx, fy, cx, cy}, robot2camera,
        x_guess, field2points, point_observations, 0, 0);
    auto end = wpi::Now();

    std::cout << "iter "
              << i
              // << "\nGuess:\n" << x_guess << "\n Optimized ->\n"
              //   << std::endl <<
              //   x_out.value_or(constrained_solvepnp::RobotStateMat::Zero())
              << "\nSucceeded? " << static_cast<bool>(x_out) << "\nIn "
              << (end - start) << "uS" << std::endl;
  }
}

TEST(CasadiWrapperTest, smoketest) { print_cost(0.1, 0.1, 0.0); }
