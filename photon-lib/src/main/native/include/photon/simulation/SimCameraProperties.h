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

#include <photon/estimation/OpenCVHelp.h>

#include <algorithm>
#include <random>
#include <string>
#include <utility>
#include <vector>

#include <Eigen/Core>
#include <frc/Errors.h>
#include <frc/MathUtil.h>
#include <frc/geometry/Rotation2d.h>
#include <frc/geometry/Translation3d.h>
#include <units/frequency.h>
#include <units/time.h>

namespace photon {
class SimCameraProperties {
 public:
  SimCameraProperties() { SetCalibration(960, 720, frc::Rotation2d{90_deg}); }
  SimCameraProperties(std::string path, int width, int height) {}

  void SetCalibration(int width, int height, frc::Rotation2d fovDiag);
  void SetCalibration(int width, int height,
                      const Eigen::Matrix<double, 3, 3>& newCamIntrinsics,
                      const Eigen::Matrix<double, 8, 1>& newDistCoeffs);

  void SetCalibError(double newAvgErrorPx, double newErrorStdDevPx) {
    avgErrorPx = newAvgErrorPx;
    errorStdDevPx = newErrorStdDevPx;
  }

  void SetFPS(units::hertz_t fps) {
    frameSpeed = units::math::max(1 / fps, exposureTime);
  }

  void SetExposureTime(units::second_t newExposureTime) {
    exposureTime = newExposureTime;
    frameSpeed = units::math::max(frameSpeed, exposureTime);
  }

  void SetAvgLatency(units::second_t newAvgLatency) {
    avgLatency = newAvgLatency;
  }

  void SetLatencyStdDev(units::second_t newLatencyStdDev) {
    latencyStdDev = newLatencyStdDev;
  }

  int GetResWidth() const { return resWidth; }

  int GetResHeight() const { return resHeight; }

  int GetResArea() const { return resWidth * resHeight; }

  double GetAspectRatio() const {
    return static_cast<double>(resWidth) / static_cast<double>(resHeight);
  }

  Eigen::Matrix<double, 3, 3> GetIntrinsics() const { return camIntrinsics; }

  Eigen::Matrix<double, 8, 1> GetDistCoeffs() const { return distCoeffs; }

  units::hertz_t GetFPS() const { return 1 / frameSpeed; }

  units::second_t GetFrameSpeed() const { return frameSpeed; }

  units::second_t GetExposureTime() const { return exposureTime; }

  units::second_t GetAverageLatency() const { return avgLatency; }

  units::second_t GetLatencyStdDev() const { return latencyStdDev; }

  double GetContourAreaPercent(const std::vector<cv::Point2f>& points) {
    return cv::contourArea(photon::OpenCVHelp::GetConvexHull(points)) /
           GetResArea() * 100;
  }

  frc::Rotation2d GetPixelYaw(double pixelX) const {
    double fx = camIntrinsics(0, 0);
    double cx = camIntrinsics(0, 2);
    double xOffset = cx - pixelX;
    return frc::Rotation2d{fx, xOffset};
  }

  frc::Rotation2d GetPixelPitch(double pixelY) const {
    double fy = camIntrinsics(1, 1);
    double cy = camIntrinsics(1, 2);
    double yOffset = cy - pixelY;
    return frc::Rotation2d{fy, -yOffset};
  }

  frc::Rotation3d GetPixelRot(const cv::Point2d& point) const {
    return frc::Rotation3d{0_rad, GetPixelPitch(point.y).Radians(),
                           GetPixelYaw(point.x).Radians()};
  }

  frc::Rotation3d GetCorrectedPixelRot(const cv::Point2d& point) const {
    double fx = camIntrinsics(0, 0);
    double cx = camIntrinsics(0, 2);
    double xOffset = cx - point.x;

    double fy = camIntrinsics(1, 1);
    double cy = camIntrinsics(1, 2);
    double yOffset = cy - point.y;

    frc::Rotation2d yaw{fx, xOffset};
    frc::Rotation2d pitch{fy / std::cos(std::atan(xOffset / fx)), -yOffset};
    return frc::Rotation3d{0_rad, pitch.Radians(), yaw.Radians()};
  }

  frc::Rotation2d GetHorizFOV() const {
    frc::Rotation2d left = GetPixelYaw(0);
    frc::Rotation2d right = GetPixelYaw(resWidth);
    return left - right;
  }

  frc::Rotation2d GetVertFOV() const {
    frc::Rotation2d above = GetPixelPitch(0);
    frc::Rotation2d below = GetPixelPitch(resHeight);
    return below - above;
  }

  frc::Rotation2d GetDiagFOV() const {
    return frc::Rotation2d{
        units::math::hypot(GetHorizFOV().Radians(), GetVertFOV().Radians())};
  }

  std::pair<std::optional<double>, std::optional<double>> GetVisibleLine(
      const RotTrlTransform3d& camRt, const frc::Translation3d& a,
      const frc::Translation3d& b) const;

  std::vector<cv::Point2f> EstPixelNoise(
      const std::vector<cv::Point2f>& points) {
    if (avgErrorPx == 0 && errorStdDevPx == 0) {
      return points;
    }

    std::vector<cv::Point2f> noisyPts;
    noisyPts.reserve(points.size());
    for (size_t i = 0; i < points.size(); i++) {
      cv::Point2f p = points[i];
      float error = avgErrorPx + gaussian(generator) * errorStdDevPx;
      float errorAngle =
          uniform(generator) * 2 * std::numbers::pi - std::numbers::pi;
      noisyPts.emplace_back(cv::Point2f{p.x + error * std::cos(errorAngle),
                                        p.y + error * std::sin(errorAngle)});
    }
    return noisyPts;
  }

  units::second_t EstLatency() {
    return units::math::max(avgLatency + gaussian(generator) * latencyStdDev,
                            0_s);
  }

  units::second_t EstSecUntilNextFrame() {
    return frameSpeed + units::math::max(0_s, EstLatency() - frameSpeed);
  }

  static SimCameraProperties PERFECT_90DEG() { return SimCameraProperties{}; }

  static SimCameraProperties PI4_LIFECAM_320_240() {
    SimCameraProperties prop{};
    prop.SetCalibration(
        320, 240,
        (Eigen::MatrixXd(3, 3) << 328.2733242048587, 0.0, 164.8190261141906,
         0.0, 318.0609794305216, 123.8633838438093, 0.0, 0.0, 1.0)
            .finished(),
        Eigen::Matrix<double, 8, 1>{
            0.09957946553445934, -0.9166265114485799, 0.0019519890627236526,
            -0.0036071725380870333, 1.5627234622420942, 0, 0, 0});
    prop.SetCalibError(0.21, 0.0124);
    prop.SetFPS(30_Hz);
    prop.SetAvgLatency(30_ms);
    prop.SetLatencyStdDev(10_ms);
    return prop;
  }

  static SimCameraProperties PI4_LIFECAM_640_480() {
    SimCameraProperties prop{};
    prop.SetCalibration(
        640, 480,
        (Eigen::MatrixXd(3, 3) << 669.1428078983059, 0.0, 322.53377249329213,
         0.0, 646.9843137061716, 241.26567383784163, 0.0, 0.0, 1.0)
            .finished(),
        Eigen::Matrix<double, 8, 1>{
            0.12788470750464645, -1.2350335805796528, 0.0024990767286192732,
            -0.0026958287600230705, 2.2951386729115537, 0, 0, 0});
    prop.SetCalibError(0.26, 0.046);
    prop.SetFPS(15_Hz);
    prop.SetAvgLatency(65_ms);
    prop.SetLatencyStdDev(15_ms);
    return prop;
  }

  static SimCameraProperties LL2_640_480() {
    SimCameraProperties prop{};
    prop.SetCalibration(
        640, 480,
        (Eigen::MatrixXd(3, 3) << 511.22843367007755, 0.0, 323.62049380211096,
         0.0, 514.5452336723849, 261.8827920543568, 0.0, 0.0, 1.0)
            .finished(),
        Eigen::Matrix<double, 8, 1>{0.1917469998873756, -0.5142936883324216,
                                    0.012461562046896614, 0.0014084973492408186,
                                    0.35160648971214437, 0, 0, 0});
    prop.SetCalibError(0.25, 0.05);
    prop.SetFPS(15_Hz);
    prop.SetAvgLatency(35_ms);
    prop.SetLatencyStdDev(8_ms);
    return prop;
  }

  static SimCameraProperties LL2_960_720() {
    SimCameraProperties prop{};
    prop.SetCalibration(
        960, 720,
        (Eigen::MatrixXd(3, 3) << 769.6873145148892, 0.0, 486.1096609458122,
         0.0, 773.8164483705323, 384.66071662358354, 0.0, 0.0, 1.0)
            .finished(),
        Eigen::Matrix<double, 8, 1>{0.189462064814501, -0.49903003669627627,
                                    0.007468423590519429, 0.002496885298683693,
                                    0.3443122090208624, 0, 0, 0});
    prop.SetCalibError(0.35, 0.10);
    prop.SetFPS(10_Hz);
    prop.SetAvgLatency(50_ms);
    prop.SetLatencyStdDev(15_ms);
    return prop;
  }

  static SimCameraProperties LL2_1280_720() {
    SimCameraProperties prop{};
    prop.SetCalibration(
        1280, 720,
        (Eigen::MatrixXd(3, 3) << 1011.3749416937393, 0.0, 645.4955139388737,
         0.0, 1008.5391755084075, 508.32877656020196, 0.0, 0.0, 1.0)
            .finished(),
        Eigen::Matrix<double, 8, 1>{0.13730101577061535, -0.2904345656989261,
                                    8.32475714507539E-4, -3.694397782014239E-4,
                                    0.09487962227027584, 0, 0, 0});
    prop.SetCalibError(0.37, 0.06);
    prop.SetFPS(7_Hz);
    prop.SetAvgLatency(60_ms);
    prop.SetLatencyStdDev(20_ms);
    return prop;
  }

 private:
  std::mt19937 generator{std::random_device{}()};
  std::normal_distribution<float> gaussian{0.0, 1.0};
  std::uniform_real_distribution<float> uniform{0.0, 1.0};

  int resWidth;
  int resHeight;
  Eigen::Matrix<double, 3, 3> camIntrinsics;
  Eigen::Matrix<double, 8, 1> distCoeffs;
  double avgErrorPx;
  double errorStdDevPx;
  units::second_t frameSpeed{0};
  units::second_t exposureTime{0};
  units::second_t avgLatency{0};
  units::second_t latencyStdDev{0};
  std::vector<Eigen::Matrix<double, 3, 1>> viewplanes{};
};
}  // namespace photon
