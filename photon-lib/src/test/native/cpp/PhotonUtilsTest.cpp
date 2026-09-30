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

#include "photon/PhotonUtils.h"

#include <catch2/catch_test_macros.hpp>
#include <catch2/matchers/catch_matchers_floating_point.hpp>
#include <wpi/math/geometry/Pose2d.hpp>
#include <wpi/math/geometry/Rotation2d.hpp>
#include <wpi/math/geometry/Transform2d.hpp>
#include <wpi/math/geometry/Translation2d.hpp>
#include <wpi/units/angle.hpp>
#include <wpi/units/length.hpp>

using namespace wpi::units;
using photon::PhotonUtils;
using wpi::math::Pose2d;
using wpi::math::Rotation2d;
using wpi::math::Transform2d;
using wpi::math::Translation2d;

static double Meters(const meter_t& value) { return unit_cast<double>(value); }

static double Degrees(const Rotation2d& rotation) {
  return unit_cast<double>(rotation.Degrees());
}

TEST_CASE("PhotonUtilsTest Include", "[photonlib]") {}

TEST_CASE("PhotonUtilsTest CalculateDistanceToTarget", "[photonlib]") {
  // 2 m of height difference, pitched 30 degrees up at the target.
  auto distance =
      PhotonUtils::CalculateDistanceToTarget(1_m, 3_m, 0_rad, 30_deg);
  CHECK_THAT(Meters(distance), Catch::Matchers::WithinAbs(3.464, 0.01));

  // Camera pitched up 20 degrees, target 10 degrees below the camera axis.
  distance = PhotonUtils::CalculateDistanceToTarget(1_m, 2_m, 20_deg, -10_deg);
  CHECK_THAT(Meters(distance), Catch::Matchers::WithinAbs(5.671, 0.01));
}

TEST_CASE("PhotonUtilsTest EstimateCameraToTargetTranslation", "[photonlib]") {
  // A target 1 m away and 30 degrees to the left of the camera axis.
  auto translation =
      PhotonUtils::EstimateCameraToTargetTranslation(1_m, Rotation2d{30_deg});
  CHECK_THAT(Meters(translation.X()), Catch::Matchers::WithinAbs(0.866, 0.01));
  CHECK_THAT(Meters(translation.Y()), Catch::Matchers::WithinAbs(0.5, 0.01));

  // Straight ahead leaves all of the distance on the x axis.
  translation =
      PhotonUtils::EstimateCameraToTargetTranslation(2_m, Rotation2d{});
  CHECK_THAT(Meters(translation.X()), Catch::Matchers::WithinAbs(2.0, 0.01));
  CHECK_THAT(Meters(translation.Y()), Catch::Matchers::WithinAbs(0.0, 0.01));
}

TEST_CASE("PhotonUtilsTest EstimateCameraToTarget", "[photonlib]") {
  // Target 1 m straight ahead of the camera, robot rotated 30 degrees CCW.
  Pose2d fieldToTarget{7_m, 3_m, Rotation2d{}};
  auto cameraToTarget = PhotonUtils::EstimateCameraToTarget(
      Translation2d{1_m, 0_m}, fieldToTarget, Rotation2d{30_deg});

  CHECK_THAT(Meters(cameraToTarget.X()), Catch::Matchers::WithinAbs(1.0, 0.01));
  CHECK_THAT(Meters(cameraToTarget.Y()), Catch::Matchers::WithinAbs(0.0, 0.01));
  // The rotation is -(gyro + fieldToTarget rotation)
  CHECK_THAT(Degrees(cameraToTarget.Rotation()),
             Catch::Matchers::WithinAbs(-30.0, 0.01));
}

TEST_CASE("PhotonUtilsTest EstimateFieldToCamera", "[photonlib]") {
  // The target sits 1 m straight ahead of the camera, and the field-to-target
  // pose puts it at x = 5 facing down +x.
  Transform2d cameraToTarget{Translation2d{1_m, 0_m}, Rotation2d{}};
  Pose2d fieldToTarget{5_m, 0_m, Rotation2d{}};

  auto fieldToCamera =
      PhotonUtils::EstimateFieldToCamera(cameraToTarget, fieldToTarget);

  CHECK_THAT(Meters(fieldToCamera.X()), Catch::Matchers::WithinAbs(4.0, 1e-6));
  CHECK_THAT(Meters(fieldToCamera.Y()), Catch::Matchers::WithinAbs(0.0, 1e-6));
  CHECK_THAT(Degrees(fieldToCamera.Rotation()),
             Catch::Matchers::WithinAbs(0.0, 1e-6));

  // Rotating the target 90 degrees puts the camera on the target's -y axis.
  Pose2d rotatedTarget{2_m, 2_m, Rotation2d{90_deg}};
  fieldToCamera =
      PhotonUtils::EstimateFieldToCamera(cameraToTarget, rotatedTarget);

  CHECK_THAT(Meters(fieldToCamera.X()), Catch::Matchers::WithinAbs(2.0, 1e-6));
  CHECK_THAT(Meters(fieldToCamera.Y()), Catch::Matchers::WithinAbs(1.0, 1e-6));
  CHECK_THAT(Degrees(fieldToCamera.Rotation()),
             Catch::Matchers::WithinAbs(90.0, 1e-6));
}

TEST_CASE("PhotonUtilsTest EstimateFieldToRobot", "[photonlib]") {
  Transform2d cameraToTarget{Translation2d{1_m, 0_m}, Rotation2d{}};
  Pose2d fieldToTarget{5_m, 0_m, Rotation2d{}};
  // The robot sits half a meter in front of the camera.
  Transform2d cameraToRobot{Translation2d{0.5_m, 0_m}, Rotation2d{}};

  auto fieldToRobot = PhotonUtils::EstimateFieldToRobot(
      cameraToTarget, fieldToTarget, cameraToRobot);

  // Camera at x = 4, robot half a meter further along +x.
  CHECK_THAT(Meters(fieldToRobot.X()), Catch::Matchers::WithinAbs(4.5, 1e-6));
  CHECK_THAT(Meters(fieldToRobot.Y()), Catch::Matchers::WithinAbs(0.0, 1e-6));
}

TEST_CASE("PhotonUtilsTest EstimateFieldToRobotFromMeasurements",
          "[photonlib]") {
  // Same as above, but through the overload that takes the heights and pitches
  // instead of a Transform2d.
  Pose2d fieldToTarget{5_m, 5_m, Rotation2d{}};
  Transform2d cameraToRobot{Translation2d{0.5_m, 0_m}, Rotation2d{}};
  auto fieldToRobot = PhotonUtils::EstimateFieldToRobot(
      1_m, 3_m, 0_rad, 30_deg, Rotation2d{}, Rotation2d{}, fieldToTarget,
      cameraToRobot);

  // Range is 3.464 m, so the camera lands at (1.536, 5.0), plus the 0.5 m
  // camera-to-robot offset.
  CHECK_THAT(Meters(fieldToRobot.X()), Catch::Matchers::WithinAbs(2.036, 0.01));
  CHECK_THAT(Meters(fieldToRobot.Y()), Catch::Matchers::WithinAbs(5.0, 0.01));
}
