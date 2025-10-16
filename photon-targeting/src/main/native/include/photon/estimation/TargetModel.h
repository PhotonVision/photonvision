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

#include <frc/geometry/Pose3d.h>
#include <frc/geometry/Translation3d.h>

#include "RotTrlTransform3d.h"

namespace photon {
class TargetModel {
 public:
  TargetModel(units::meter_t width, units::meter_t height)
      : vertices({frc::Translation3d{0_m, -width / 2.0, -height / 2.0},
                  frc::Translation3d{0_m, width / 2.0, -height / 2.0},
                  frc::Translation3d{0_m, width / 2.0, height / 2.0},
                  frc::Translation3d{0_m, -width / 2.0, height / 2.0}}),
        isPlanar(true),
        isSpherical(false) {}

  TargetModel(units::meter_t length, units::meter_t width,
              units::meter_t height)
      : TargetModel({
            frc::Translation3d{length / 2.0, -width / 2.0, -height / 2.0},
            frc::Translation3d{length / 2.0, width / 2.0, -height / 2.0},
            frc::Translation3d{length / 2.0, width / 2.0, height / 2.0},
            frc::Translation3d{length / 2.0, -width / 2.0, height / 2.0},
            frc::Translation3d{-length / 2.0, -width / 2.0, height / 2.0},
            frc::Translation3d{-length / 2.0, width / 2.0, height / 2.0},
            frc::Translation3d{-length / 2.0, width / 2.0, -height / 2.0},
            frc::Translation3d{-length / 2.0, -width / 2.0, -height / 2.0},
        }) {}

  explicit TargetModel(units::meter_t diameter)
      : vertices({
            frc::Translation3d{0_m, -diameter / 2.0, 0_m},
            frc::Translation3d{0_m, 0_m, -diameter / 2.0},
            frc::Translation3d{0_m, diameter / 2.0, 0_m},
            frc::Translation3d{0_m, 0_m, diameter / 2.0},
        }),
        isPlanar(false),
        isSpherical(true) {}

  explicit TargetModel(const std::vector<frc::Translation3d>& verts)
      : isSpherical(false) {
    if (verts.size() <= 2) {
      vertices = std::vector<frc::Translation3d>();
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

  std::vector<frc::Translation3d> GetFieldVertices(
      const frc::Pose3d& targetPose) const {
    RotTrlTransform3d basisChange{targetPose.Rotation(),
                                  targetPose.Translation()};
    std::vector<frc::Translation3d> retVal;
    retVal.reserve(vertices.size());
    for (const auto& vert : vertices) {
      retVal.emplace_back(basisChange.Apply(vert));
    }
    return retVal;
  }

  static frc::Pose3d GetOrientedPose(const frc::Translation3d& tgtTrl,
                                     const frc::Translation3d& cameraTrl) {
    frc::Translation3d relCam = cameraTrl - tgtTrl;
    frc::Rotation3d orientToCam = frc::Rotation3d{
        0_rad,
        frc::Rotation2d{units::math::hypot(relCam.X(), relCam.Y()).to<double>(),
                        -relCam.Z().to<double>()}
            .Radians(),
        frc::Rotation2d{relCam.X().to<double>(), relCam.Y().to<double>()}
            .Radians()};
    return frc::Pose3d{tgtTrl, orientToCam};
  }

  std::vector<frc::Translation3d> GetVertices() const { return vertices; }
  bool GetIsPlanar() const { return isPlanar; }
  bool GetIsSpherical() const { return isSpherical; }

 private:
  std::vector<frc::Translation3d> vertices;
  bool isPlanar;
  bool isSpherical;
};

static const TargetModel kAprilTag16h5{6_in, 6_in};
static const TargetModel kAprilTag36h11{6.5_in, 6.5_in};
}  // namespace photon
