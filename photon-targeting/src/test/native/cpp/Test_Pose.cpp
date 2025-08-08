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

#include <random>
		
#include "cpnp/constrained_pnp.h"
#include <gtest/gtest.h>

#include <iostream>
#include <vector>

#include <frc/geometry/Transform2d.h>
#include <frc/geometry/Translation2d.h>
#include <units/length.h>

/// @brief Get the positions of the corners of a tag relative to the tag origin, in homogeneous coordinates.
/// @return A 4x4 matrix where the first three rows are the x, y, and z coordinates of the corners, and the last row is all ones.
Eigen::Matrix4d tagCenterToCorners() {
  const units::meter_t width{6.0_in};
  const units::meter_t height{6.0_in};

  Eigen::Matrix<double, 4, 3> corners {};
  corners << 0, -width.value(), -height.value(), 
             0, width.value(), -height.value(), 
             0, width.value(), height.value(), 
             0, -width.value(), height.value();

  Eigen::Matrix4d ret {};
  ret.block(0, 0, 3, 4) = corners.transpose();
  ret.row(3) = Eigen::Matrix<double, 1, 4>::Ones();

  return ret;
}

/// @brief Get a list of test tags' corners in homogeneous coordinates. The locations of these tags is hard-coded, because I'm lazy
/// @return A 4xN matrix where the first three rows are the x, y, and z coordinates of the corners, and the last row is all ones.
Eigen::Matrix<double, 4, Eigen::Dynamic> getTestTags() {
  // change me to add more tags
  Eigen::Matrix<double, 4, Eigen::Dynamic> ret(4, 8);

  auto tagCorners = tagCenterToCorners();

  // Add all the corners of tag 1, located at (2, 0, 1) and rotated 180 degrees
  // about +Z
  auto tag1pose = frc::Pose3d{frc::Translation3d{2_m, 0_m, 1_m},
                              frc::Rotation3d{0_deg, 0_deg, 180_deg}}
                      .ToMatrix();

  ret.block(0, 0, 4, 4) = tag1pose * tagCorners;

  // Add all the corners of tag 2, located at (2, 1, 1) and rotated 180 degrees
  // about +Z
  auto tag2pose = frc::Pose3d{frc::Translation3d{2_m, 1_m, 1_m},
                              frc::Rotation3d{0_deg, 0_deg, 180_deg}}
                      .ToMatrix();

  ret.block(0, 4, 4, 4) = tag2pose * tagCorners;

  return ret;
}


/// @brief Project the corners of the tags into the camera frame.
/// @param K OpenCV camera calibration matrix
/// @param field2camera_wpi The location of the camera in the field frame. This is the "wpi" camera pose, with X forward, Y left, and Z up.
/// @param field2corners The locations of the corners of the tags in the field frame
/// @return Observed pixel locations
Eigen::Matrix<double, 2, Eigen::Dynamic>
projectPoints(double f_x,
              double f_y,
              double c_x,
              double c_y,
              Eigen::Matrix4d field2camera_wpi,
              Eigen::Matrix<double, 4, Eigen::Dynamic> field2corners) {
  // robot is ENU, cameras are SDE
  Eigen::Matrix4d camera2opencv{
      {0, 0, 1, 0},
      {-1, 0, 0, 0},
      {0, -1, 0, 0},
      {0, 0, 0, 1},
  };
  Eigen::Matrix4d field2camera = field2camera_wpi * camera2opencv;

  // transform the points to camera space
  auto camera2corners = field2camera.inverse() * field2corners; 

  // project the points. This is verbose but whatever
  Eigen::Matrix<double, 3, 3> K;
  K << f_x, 0, c_x, 0, f_y, c_y, 0, 0, 1;
  auto pointsUnnormalized =
      K * camera2corners.block(0, 0, 3, camera2corners.cols());
  auto u =
      pointsUnnormalized.row(0).array() / pointsUnnormalized.row(2).array();
  auto v =
      pointsUnnormalized.row(1).array() / pointsUnnormalized.row(2).array();

  Eigen::Matrix<double, 2, Eigen::Dynamic> ret(2, camera2corners.cols());
  ret.row(0) = u;
  ret.row(1) = v;
  return ret;
}

TEST(PoseTest, Projection) {
  // cpnp::ProblemParams params(4);

  // params.K << 599.375, 0., 479.5, 0., 599.16666667, 359.5, 0., 0., 1.;
  // params.K << 100, 0., 0, 0., 100, 0, 0., 0., 1.;

  // params.f_x = 100;
  // params.f_y = 100;
  // params.c_x = 0;
  // params.c_y = 0;

  // params.worldPoints = getTestTags();

  // frc::Transform3d robot2camera {};
  // params.imagePoints = projectPoints(params.f_x, params.f_y, params.c_x, params.c_y, robot2camera.ToMatrix(), params.worldPoints);

  // std::cout << "world points:\n" << params.worldPoints << std::endl;
  // std::cout << "image points:\n" << params.imagePoints << std::endl;
}

TEST(PoseTest, Naive) {
  for (int j = 0; j < 20; j++) {

  cpnp::ProblemParams params;

  params.f_x = 100;
  params.f_y = 100;
  params.c_x = 0;
  params.c_y = 0;

  auto tags = getTestTags();
  frc::Transform3d robot2camera {frc::Pose3d{}, frc::Pose3d{frc::Translation3d{-1.3_m, 0.25_m, 0_m}, frc::Rotation3d{0_rad, 0_rad, -1.5_rad}}};
  auto imgpoints = projectPoints(params.f_x, params.f_y, params.c_x, params.c_y, robot2camera.ToMatrix(), tags);

  // Add +-1 px of noise to the image points
  std::random_device rd;
  std::mt19937 rng(rd());
  std::uniform_real_distribution<double> dist(-1.0, 1.0);

  for (int i = 0; i < tags.cols(); ++i) {
    // Add some noise to imgpoints, and add it to our list
    params.imagePoints.push_back(imgpoints(0, i) + dist(rng));
    params.imagePoints.push_back(imgpoints(1, i) + dist(rng));

    params.worldPoints.push_back(tags(0, i));
    params.worldPoints.push_back(tags(1, i));
    params.worldPoints.push_back(tags(2, i));
  }

  auto t0 = std::chrono::high_resolution_clock::now();
  auto polynomial_ret = cpnp::solve_polynomial(params);
  auto t1 = std::chrono::high_resolution_clock::now();

  fmt::println("Polynomial solve time: {}ms", std::chrono::duration_cast<std::chrono::nanoseconds>(t1-t0).count() / 1e6);
  
  // auto t2 = std::chrono::high_resolution_clock::now();
  // auto naive_ret = cpnp::solve_naive(params);
  // auto t3 = std::chrono::high_resolution_clock::now();

  // fmt::println("Naive solve time: {}ms", std::chrono::duration_cast<std::chrono::nanoseconds>(t3-t2).count() / 1e6);
  fmt::println("Polynomial method says robot is at:\n{}", polynomial_ret.ToMatrix());
  // fmt::println("Naive method says robot is at:\n{}", naive_ret.ToMatrix());
  }
}
