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

#include "photon/simulation/SimCameraProperties.h"

#include <algorithm>
#include <utility>
#include <vector>

using namespace photon;

void SimCameraProperties::SetCalibration(int width, int height,
                                         frc::Rotation2d fovDiag) {
  if (fovDiag.Degrees() < 1_deg || fovDiag.Degrees() > 179_deg) {
    fovDiag = frc::Rotation2d{
        std::clamp<units::degree_t>(fovDiag.Degrees(), 1_deg, 179_deg)};
    FRC_ReportError(
        frc::err::Error,
        "Requested invalid FOV! Clamping between (1, 179) degrees...");
  }
  double resDiag = std::sqrt(width * width + height * height);
  double diagRatio = units::math::tan(fovDiag.Radians() / 2.0);
  frc::Rotation2d fovWidth{
      units::radian_t{std::atan(diagRatio * (width / resDiag)) * 2}};
  frc::Rotation2d fovHeight{
      units::radian_t{std::atan(diagRatio * (height / resDiag)) * 2}};

  Eigen::Matrix<double, 8, 1> newDistCoeffs =
      Eigen::Matrix<double, 8, 1>::Zero();

  double cx = width / 2.0 - 0.5;
  double cy = height / 2.0 - 0.5;

  double fx = cx / units::math::tan(fovWidth.Radians() / 2.0);
  double fy = cy / units::math::tan(fovHeight.Radians() / 2.0);

  Eigen::Matrix<double, 3, 3> newCamIntrinsics;
  newCamIntrinsics << fx, 0.0, cx, 0.0, fy, cy, 0.0, 0.0, 1.0;
  SetCalibration(width, height, newCamIntrinsics, newDistCoeffs);
}

void SimCameraProperties::SetCalibration(
    int width, int height, const Eigen::Matrix<double, 3, 3>& newCamIntrinsics,
    const Eigen::Matrix<double, 8, 1>& newDistCoeffs) {
  resWidth = width;
  resHeight = height;
  camIntrinsics = newCamIntrinsics;
  distCoeffs = newDistCoeffs;

  std::array<frc::Translation3d, 4> p{
      frc::Translation3d{1_m, frc::Rotation3d{0_rad, 0_rad,
                                              (GetPixelYaw(0) +
                                               frc::Rotation2d{units::radian_t{
                                                   -std::numbers::pi / 2.0}})
                                                  .Radians()}},
      frc::Translation3d{1_m, frc::Rotation3d{0_rad, 0_rad,
                                              (GetPixelYaw(width) +
                                               frc::Rotation2d{units::radian_t{
                                                   std::numbers::pi / 2.0}})
                                                  .Radians()}},
      frc::Translation3d{1_m, frc::Rotation3d{0_rad,
                                              (GetPixelPitch(0) +
                                               frc::Rotation2d{units::radian_t{
                                                   std::numbers::pi / 2.0}})
                                                  .Radians(),
                                              0_rad}},
      frc::Translation3d{1_m, frc::Rotation3d{0_rad,
                                              (GetPixelPitch(height) +
                                               frc::Rotation2d{units::radian_t{
                                                   -std::numbers::pi / 2.0}})
                                                  .Radians(),
                                              0_rad}},
  };
  viewplanes.clear();
  for (size_t i = 0; i < p.size(); i++) {
    viewplanes.emplace_back(Eigen::Matrix<double, 3, 1>{
        p[i].X().to<double>(), p[i].Y().to<double>(), p[i].Z().to<double>()});
  }
}

std::pair<std::optional<double>, std::optional<double>>
SimCameraProperties::GetVisibleLine(const RotTrlTransform3d& camRt,
                                    const frc::Translation3d& a,
                                    const frc::Translation3d& b) const {
  frc::Translation3d relA = camRt.Apply(a);
  frc::Translation3d relB = camRt.Apply(b);

  if (relA.X() <= 0_m && relB.X() <= 0_m) {
    return {std::nullopt, std::nullopt};
  }

  Eigen::Matrix<double, 3, 1> av{relA.X().to<double>(), relA.Y().to<double>(),
                                 relA.Z().to<double>()};
  Eigen::Matrix<double, 3, 1> bv{relB.X().to<double>(), relB.Y().to<double>(),
                                 relB.Z().to<double>()};
  Eigen::Matrix<double, 3, 1> abv = bv - av;

  bool aVisible = true;
  bool bVisible = true;
  for (size_t i = 0; i < viewplanes.size(); i++) {
    Eigen::Matrix<double, 3, 1> normal = viewplanes[i];
    double aVisibility = av.dot(normal);
    if (aVisibility < 0) {
      aVisible = false;
    }
    double bVisibility = bv.dot(normal);
    if (bVisibility < 0) {
      bVisible = false;
    }
    if (aVisibility <= 0 && bVisibility <= 0) {
      return {std::nullopt, std::nullopt};
    }
  }

  if (aVisible && bVisible) {
    return {0, 1};
  }

  std::array<double, 4> intersections{std::nan(""), std::nan(""), std::nan(""),
                                      std::nan("")};
  std::vector<std::optional<Eigen::Matrix<double, 3, 1>>> ipts{
      {std::nullopt, std::nullopt, std::nullopt, std::nullopt}};

  for (size_t i = 0; i < viewplanes.size(); i++) {
    Eigen::Matrix<double, 3, 1> normal = viewplanes[i];
    Eigen::Matrix<double, 3, 1> a_projn{};
    a_projn = (av.dot(normal) / normal.dot(normal)) * normal;

    if (std::abs(abv.dot(normal)) < 1e-5) {
      continue;
    }
    intersections[i] = a_projn.dot(a_projn) / -(abv.dot(a_projn));

    Eigen::Matrix<double, 3, 1> apv{};
    apv = intersections[i] * abv;
    Eigen::Matrix<double, 3, 1> intersectpt{};
    intersectpt = av + apv;
    ipts[i] = intersectpt;

    for (size_t j = 1; j < viewplanes.size(); j++) {
      int oi = (i + j) % viewplanes.size();
      Eigen::Matrix<double, 3, 1> onormal = viewplanes[oi];
      if (intersectpt.dot(onormal) < 0) {
        intersections[i] = std::nan("");
        ipts[i] = std::nullopt;
        break;
      }
    }

    if (!ipts[i]) {
      continue;
    }

    for (int j = i - 1; j >= 0; j--) {
      std::optional<Eigen::Matrix<double, 3, 1>> oipt = ipts[j];
      if (!oipt) {
        continue;
      }
      Eigen::Matrix<double, 3, 1> diff{};
      diff = oipt.value() - intersectpt;
      if (diff.cwiseAbs().maxCoeff() < 1e-4) {
        intersections[i] = std::nan("");
        ipts[i] = std::nullopt;
        break;
      }
    }
  }

  double inter1 = std::nan("");
  double inter2 = std::nan("");
  for (double inter : intersections) {
    if (!std::isnan(inter)) {
      if (std::isnan(inter1)) {
        inter1 = inter;
      } else {
        inter2 = inter;
      }
    }
  }

  if (!std::isnan(inter2)) {
    double max = std::max(inter1, inter2);
    double min = std::min(inter1, inter2);
    if (aVisible) {
      min = 0;
    }
    if (bVisible) {
      max = 1;
    }
    return {min, max};
  } else if (!std::isnan(inter1)) {
    if (aVisible) {
      return {0, inter1};
    }
    if (bVisible) {
      return {inter1, 1};
    }
    return {inter1, std::nullopt};
  } else {
    return {std::nullopt, std::nullopt};
  }
}
