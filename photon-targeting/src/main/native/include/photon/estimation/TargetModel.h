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

#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Translation3d.hpp>

#include "RotTrlTransform3d.h"

namespace photon {
class TargetModel {
 public:
  TargetModel(wpi::units::meter_t width, wpi::units::meter_t height)
      : vertices({wpi::math::Translation3d{0_m, -width / 2.0, -height / 2.0},
                  wpi::math::Translation3d{0_m, width / 2.0, -height / 2.0},
                  wpi::math::Translation3d{0_m, width / 2.0, height / 2.0},
                  wpi::math::Translation3d{0_m, -width / 2.0, height / 2.0}}),
        isPlanar(true),
        isSpherical(false) {}

  TargetModel(wpi::units::meter_t length, wpi::units::meter_t width,
              wpi::units::meter_t height)
      : TargetModel({
            wpi::math::Translation3d{length / 2.0, -width / 2.0, -height / 2.0},
            wpi::math::Translation3d{length / 2.0, width / 2.0, -height / 2.0},
            wpi::math::Translation3d{length / 2.0, width / 2.0, height / 2.0},
            wpi::math::Translation3d{length / 2.0, -width / 2.0, height / 2.0},
            wpi::math::Translation3d{-length / 2.0, -width / 2.0, height / 2.0},
            wpi::math::Translation3d{-length / 2.0, width / 2.0, height / 2.0},
            wpi::math::Translation3d{-length / 2.0, width / 2.0, -height / 2.0},
            wpi::math::Translation3d{-length / 2.0, -width / 2.0,
                                     -height / 2.0},
        }) {}

  explicit TargetModel(wpi::units::meter_t diameter)
      : vertices({
            wpi::math::Translation3d{0_m, -diameter / 2.0, 0_m},
            wpi::math::Translation3d{0_m, 0_m, -diameter / 2.0},
            wpi::math::Translation3d{0_m, diameter / 2.0, 0_m},
            wpi::math::Translation3d{0_m, 0_m, diameter / 2.0},
        }),
        isPlanar(false),
        isSpherical(true) {}

  explicit TargetModel(const std::vector<wpi::math::Translation3d>& verts)
      : isSpherical(false) {
    if (verts.size() <= 2) {
      vertices = std::vector<wpi::math::Translation3d>();
      isPlanar = false;
    } else {
      bool cornersPlanar = true;
      for (const auto& corner : verts) {
        if (corner.X() != 0_m) {
          cornersPlanar = false;
        }
      }
      isPlanar = cornersPlanar;
    }
    vertices = verts;
  }

  std::vector<wpi::math::Translation3d> GetFieldVertices(
      const wpi::math::Pose3d& targetPose) const {
    RotTrlTransform3d basisChange{targetPose.Rotation(),
                                  targetPose.Translation()};
    std::vector<wpi::math::Translation3d> retVal;
    retVal.reserve(vertices.size());
    for (const auto& vert : vertices) {
      retVal.emplace_back(basisChange.Apply(vert));
    }
    return retVal;
  }

  static wpi::math::Pose3d GetOrientedPose(
      const wpi::math::Translation3d& tgtTrl,
      const wpi::math::Translation3d& cameraTrl) {
    wpi::math::Translation3d relCam = cameraTrl - tgtTrl;
    wpi::math::Rotation3d orientToCam = wpi::math::Rotation3d{
        0_rad,
        wpi::math::Rotation2d{
            wpi::units::math::hypot(relCam.X(), relCam.Y()).to<double>(),
            -relCam.Z().to<double>()}
            .Radians(),
        wpi::math::Rotation2d{relCam.X().to<double>(), relCam.Y().to<double>()}
            .Radians()};
    return wpi::math::Pose3d{tgtTrl, orientToCam};
  }

  std::vector<wpi::math::Translation3d> GetVertices() const { return vertices; }
  bool GetIsPlanar() const { return isPlanar; }
  bool GetIsSpherical() const { return isSpherical; }

 private:
  std::vector<wpi::math::Translation3d> vertices;
  bool isPlanar;
  bool isSpherical;
};

static const TargetModel kAprilTag16h5{6_in, 6_in};
static const TargetModel kAprilTag36h11{6.5_in, 6.5_in};
}  // namespace photon
