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
#include <photon/estimation/OpenCVHelp.h>
#include <units/frequency.h>
#include <units/time.h>

namespace photon {

/**
 * Calibration and performance values for this camera.
 *
 * The resolution will affect the accuracy of projected(3d to 2d) target
 * corners and similarly the severity of image noise on estimation(2d to 3d).
 *
 * The camera intrinsics and distortion coefficients describe the results of
 * calibration, and how to map between 3d field points and 2d image points.
 *
 * The performance values (framerate/exposure time, latency) determine how
 * often results should be updated and with how much latency in simulation. High
 * exposure time causes motion blur which can inhibit target detection while
 * moving. Note that latency estimation does not account for network latency and
 * the latency reported will always be perfect.
 */
class SimCameraProperties {
 public:
  /** Default constructor which is the same as PERFECT_90DEG */
  SimCameraProperties() { SetCalibration(960, 720, frc::Rotation2d{90_deg}); }

  /**
   * Reads camera properties from a PhotonVision config.json file.
   * This is only the resolution, camera intrinsics, distortion coefficients,
   * and average/std. dev. pixel error. Other camera properties must be set.
   *
   * @param path Path to the config.json file
   * @param width The width of the desired resolution in the JSON
   * @param height The height of the desired resolution in the JSON
   */
  SimCameraProperties(std::string path, int width, int height) {}

  void SetCalibration(int width, int height, frc::Rotation2d fovDiag);
  void SetCalibration(int width, int height,
                      const Eigen::Matrix<double, 3, 3>& newCamIntrinsics,
                      const Eigen::Matrix<double, 8, 1>& newDistCoeffs);

  void SetCalibError(double newAvgErrorPx, double newErrorStdDevPx) {
    avgErrorPx = newAvgErrorPx;
    errorStdDevPx = newErrorStdDevPx;
  }

  /**
   * Sets the simulated FPS for this camera.
   *
   * @param fps The average frames per second the camera should process at.
   * **Exposure time limits FPS if set!**
   */
  void SetFPS(units::hertz_t fps) {
    frameSpeed = units::math::max(1 / fps, exposureTime);
  }

  /**
   * Sets the simulated exposure time for this camera.
   *
   * @param exposureTime The amount of time the "shutter" is open for one frame.
   * Affects motion blur. **Frame speed(from FPS) is limited to this!**
   */
  void SetExposureTime(units::second_t exposureTime) {
    this->exposureTime = exposureTime;
    frameSpeed = units::math::max(frameSpeed, this->exposureTime);
  }

  /**
   * Sets the simulated latency for this camera.
   *
   * @param avgLatency The average latency (from image capture to data
   * published) a frame should have
   */
  void SetAvgLatency(units::second_t avgLatency) {
    this->avgLatency = avgLatency;
  }

  /**
   * Sets the simulated latency variation for this camera.
   *
   * @param latencyStdDev The standard deviation of the latency
   */
  void SetLatencyStdDev(units::second_t latencyStdDev) {
    this->latencyStdDev = latencyStdDev;
  }

  /**
   * Gets the width of the simulated camera image.
   *
   * @return The width in pixels
   */
  int GetResWidth() const { return resWidth; }

  /**
   * Gets the height of the simulated camera image.
   *
   * @return The height in pixels
   */
  int GetResHeight() const { return resHeight; }

  /**
   * Gets the area of the simulated camera image.
   *
   * @return The area in pixels
   */
  int GetResArea() const { return resWidth * resHeight; }

  double GetAspectRatio() const {
    return static_cast<double>(resWidth) / static_cast<double>(resHeight);
  }

  Eigen::Matrix<double, 3, 3> GetIntrinsics() const { return camIntrinsics; }

  /**
   * Returns the camera calibration's distortion coefficients, in OPENCV8 form.
   * Higher-order terms are set to 0
   *
   * @return The distortion coefficients in an 8x1 matrix
   */
  Eigen::Matrix<double, 8, 1> GetDistCoeffs() const { return distCoeffs; }

  /**
   * Gets the FPS of the simulated camera.
   *
   * @return The FPS
   */
  units::hertz_t GetFPS() const { return 1 / frameSpeed; }

  /**
   * Gets the time per frame of the simulated camera.
   *
   * @return The time per frame
   */
  units::second_t GetFrameSpeed() const { return frameSpeed; }

  /**
   * Gets the exposure time of the simulated camera.
   *
   * @return The exposure time
   */
  units::second_t GetExposureTime() const { return exposureTime; }

  /**
   * Gets the average latency of the simulated camera.
   *
   * @return The average latency
   */
  units::second_t GetAverageLatency() const { return avgLatency; }

  /**
   * Gets the time per frame of the simulated camera.
   *
   * @return The time per frame
   */
  units::second_t GetLatencyStdDev() const { return latencyStdDev; }

  /**
   * The percentage (0 - 100) of this camera's resolution the contour takes up
   * in pixels of the image.
   *
   * @param points Points of the contour
   * @return The percentage
   */
  double GetContourAreaPercent(const std::vector<cv::Point2f>& points) {
    return cv::contourArea(photon::OpenCVHelp::GetConvexHull(points)) /
           GetResArea() * 100;
  }

  /** The yaw from the principal point of this camera to the pixel x value.
   * Positive values left. */
  frc::Rotation2d GetPixelYaw(double pixelX) const {
    double fx = camIntrinsics(0, 0);
    double cx = camIntrinsics(0, 2);
    double xOffset = cx - pixelX;
    return frc::Rotation2d{fx, xOffset};
  }

  /**
   * The pitch from the principal point of this camera to the pixel y value.
   * Pitch is positive down.
   *
   * Note that this angle is naively computed and may be incorrect. See
   * #getCorrectedPixelRot(const cv::Point2d).
   */
  frc::Rotation2d GetPixelPitch(double pixelY) const {
    double fy = camIntrinsics(1, 1);
    double cy = camIntrinsics(1, 2);
    double yOffset = cy - pixelY;
    return frc::Rotation2d{fy, -yOffset};
  }
  /**
   * Finds the yaw and pitch to the given image point. Yaw is positive left, and
   * pitch is positive down.
   *
   * Note that pitch is naively computed and may be incorrect. See
   * #getCorrectedPixelRot(const cv::Point2d).
   */
  frc::Rotation3d GetPixelRot(const cv::Point2d& point) const {
    return frc::Rotation3d{0_rad, GetPixelPitch(point.y).Radians(),
                           GetPixelYaw(point.x).Radians()};
  }

  /**
   * Gives the yaw and pitch of the line intersecting the camera lens and the
   * given pixel coordinates on the sensor. Yaw is positive left, and pitch
   * positive down.
   *
   * The pitch traditionally calculated from pixel offsets do not correctly
   * account for non-zero values of yaw because of perspective distortion (not
   * to be confused with lens distortion)-- for example, the pitch angle is
   * naively calculated as:
   *
   * <pre>pitch = arctan(pixel y offset / focal length y)<pre>
   *
   * However, using focal length as a side of the associated right triangle is
   * not correct when the pixel x value is not 0, because the distance from this
   * pixel (projected on the x-axis) to the camera lens increases. Projecting a
   * line back out of the camera with these naive angles will not intersect the
   * 3d point that was originally projected into this 2d pixel. Instead, this
   * length should be:
   *
   * <pre>focal length y ⟶ (focal length y / cos(arctan(pixel x offset / focal
   * length x)))</pre>
   *
   * @return Rotation3d with yaw and pitch of the line projected out of the
   * camera from the given pixel (roll is zero).
   */
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

  /**
   * Determines where the line segment defined by the two given translations
   * intersects the camera's frustum/field-of-vision, if at all.
   *
   * <p>The line is parametrized so any of its points <code>p = t * (b - a) +
   * a</code>. This method returns these values of t, minimum first, defining
   * the region of the line segment which is visible in the frustum. If both
   * ends of the line segment are visible, this simply returns {0, 1}. If, for
   * example, point b is visible while a is not, and half of the line segment is
   * inside the camera frustum, {0.5, 1} would be returned.
   *
   * @param camRt The change in basis from world coordinates to camera
   * coordinates. See RotTrlTransform3d#makeRelativeTo(frc::Pose3d).
   * @param a The initial translation of the line
   * @param b The final translation of the line
   * @return A Pair of Doubles. The values may be empty:
   *       - {double, double} : Two parametrized values(t), minimum first,
   * representing which segment of the line is visible in the camera frustum.
   *       - {double, std::nullopt} : One value(t) representing a single
   * intersection point. For example, the line only intersects the intersection
   * of two adjacent viewplanes.
   *       - {std::nullopt, std::nullopt} : No values. The line segment is not
   * visible in the camera frustum.
   */
  std::pair<std::optional<double>, std::optional<double>> GetVisibleLine(
      const RotTrlTransform3d& camRt, const frc::Translation3d& a,
      const frc::Translation3d& b) const;

  /**
   * Returns these points after applying this camera's estimated noise.
   *
   * @param points The points to add noise to
   * @return The points with noise
   */
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

  /**
   * Returns an estimation of a frame's processing latency with noise added.
   *
   * @return The latency estimate
   */
  units::second_t EstLatency() {
    return units::math::max(avgLatency + gaussian(generator) * latencyStdDev,
                            0_s);
  }

  /**
   * Estimates how long until the next frame should be processed.
   *
   * @return The estimated time until the next frame
   */
  units::second_t EstSecUntilNextFrame() {
    return frameSpeed + units::math::max(0_s, EstLatency() - frameSpeed);
  }

  /**
   * Creates a set of camera properties where the camera has a 960x720
   * resolution, 90 degree FOV, and is a "perfect" lagless camera.
   *
   * @return The properties for this theoretical camera
   */
  static SimCameraProperties PERFECT_90DEG() { return SimCameraProperties{}; }

  /**
   * Creates a set of camera properties matching those of Microsoft Lifecam
   * running on a Raspberry Pi 4 at 320x240 resolution.
   *
   * Note that this set of properties represents *a camera setup*, not *your
   * camera setup*. Do not use these camera properties for any non-sim vision
   * calculations, especially the calibration data. Always use your camera's
   * calibration data to do vision calculations in non-sim environments. These
   * properties exist as a sample that may be used to get representative data in
   * sim.
   *
   * @return The properties for this camera setup
   */
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

  /**
   * Creates a set of camera properties matching those of Microsoft Lifecam
   * running on a Raspberry Pi 4 at 640x480 resolution.
   *
   * <p>Note that this set of properties represents *a camera setup*, not *your
   * camera setup*. Do not use these camera properties for any non-sim vision
   * calculations, especially the calibration data. Always use your camera's
   * calibration data to do vision calculations in non-sim environments. These
   * properties exist as a sample that may be used to get representative data in
   * sim.
   *
   * @return The properties for this camera setup
   */
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

  /**
   * Creates a set of camera properties matching those of a Limelight 2 running
   * at 640x480 resolution.
   *
   * <p>Note that this set of properties represents *a camera setup*, not *your
   * camera setup*. Do not use these camera properties for any non-sim vision
   * calculations, especially the calibration data. Always use your camera's
   * calibration data to do vision calculations in non-sim environments. These
   * properties exist as a sample that may be used to get representative data in
   * sim.
   *
   * @return The properties for this camera setup
   */
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

  /**
   * Creates a set of camera properties matching those of a Limelight 2 running
   * at 960x720 resolution.
   *
   * <p>Note that this set of properties represents *a camera setup*, not *your
   * camera setup*. Do not use these camera properties for any non-sim vision
   * calculations, especially the calibration data. Always use your camera's
   * calibration data to do vision calculations in non-sim environments. These
   * properties exist as a sample that may be used to get representative data in
   * sim.
   *
   * @return The properties for this camera setup
   */
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

  /**
   * Creates a set of camera properties matching those of a Limelight 2 running
   * at 1280x720 resolution.
   *
   * <p>Note that this set of properties represents *a camera setup*, not *your
   * camera setup*. Do not use these camera properties for any non-sim vision
   * calculations, especially the calibration data. Always use your camera's
   * calibration data to do vision calculations in non-sim environments. These
   * properties exist as a sample that may be used to get representative data in
   * sim.
   *
   * @return The properties for this camera setup
   */
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
  double avgErrorPx{0};
  double errorStdDevPx{0};
  units::second_t frameSpeed{0};
  units::second_t exposureTime{0};
  units::second_t avgLatency{0};
  units::second_t latencyStdDev{0};
  std::vector<Eigen::Matrix<double, 3, 1>> viewplanes{};
};
}  // namespace photon
