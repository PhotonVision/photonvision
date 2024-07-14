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

#include <utility>
#include <vector>

#include <Eigen/Core>
#include <opencv2/calib3d.hpp>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#include "RotTrlTransform3d.h"

#define OPENCV_DISABLE_EIGEN_TENSOR_SUPPORT
#include <opencv2/core/eigen.hpp>
#include "photon/targeting/PnpResult.h"
#include "photon/targeting/MultiTargetPNPResult.h"

namespace photon {
namespace OpenCVHelp {

static frc::Rotation3d NWU_TO_EDN{
    (Eigen::Matrix3d() << 0, -1, 0, 0, 0, -1, 1, 0, 0).finished()};
static frc::Rotation3d EDN_TO_NWU{
    (Eigen::Matrix3d() << 0, 0, 1, -1, 0, 0, 0, -1, 0).finished()};

static std::vector<cv::Point2f> GetConvexHull(
    const std::vector<cv::Point2f>& points) {
  std::vector<int> outputHull{};
  cv::convexHull(points, outputHull);
  std::vector<cv::Point2f> convexPoints;
  for (size_t i = 0; i < outputHull.size(); i++) {
    convexPoints.push_back(points[outputHull[i]]);
  }
  return convexPoints;
}

static cv::RotatedRect GetMinAreaRect(const std::vector<cv::Point2f>& points) {
  return cv::minAreaRect(points);
}

static frc::Translation3d TranslationNWUtoEDN(const frc::Translation3d& trl) {
  return trl.RotateBy(NWU_TO_EDN);
}

static frc::Rotation3d RotationNWUtoEDN(const frc::Rotation3d& rot) {
  return -NWU_TO_EDN + (rot + NWU_TO_EDN);
}

static std::vector<cv::Point3f> TranslationToTVec(
    const std::vector<frc::Translation3d>& translations) {
  std::vector<cv::Point3f> retVal;
  retVal.reserve(translations.size());
  for (size_t i = 0; i < translations.size(); i++) {
    frc::Translation3d trl = TranslationNWUtoEDN(translations[i]);
    retVal.emplace_back(cv::Point3f{trl.X().to<float>(), trl.Y().to<float>(),
                                    trl.Z().to<float>()});
  }
  return retVal;
}

static std::vector<cv::Point3f> RotationToRVec(
    const frc::Rotation3d& rotation) {
  std::vector<cv::Point3f> retVal{};
  frc::Rotation3d rot = RotationNWUtoEDN(rotation);
  retVal.emplace_back(cv::Point3d{
      rot.GetQuaternion().ToRotationVector()(0),
      rot.GetQuaternion().ToRotationVector()(1),
      rot.GetQuaternion().ToRotationVector()(2),
  });
  return retVal;
}

[[maybe_unused]] static cv::Point2f AvgPoint(std::vector<cv::Point2f> points) {
  if (points.size() == 0) {
    return cv::Point2f{};
  }
  cv::reduce(points, points, 0, cv::REDUCE_AVG);
  return points[0];
}

[[maybe_unused]] static std::vector<std::pair<float, float>> PointsToCorners(
    const std::vector<cv::Point2f>& points) {
  std::vector<std::pair<float, float>> retVal;
  retVal.reserve(points.size());
  for (size_t i = 0; i < points.size(); i++) {
    retVal.emplace_back(std::make_pair(points[i].x, points[i].y));
  }
  return retVal;
}

[[maybe_unused]] static std::vector<cv::Point2f> CornersToPoints(
    const std::vector<std::pair<float, float>>& corners) {
  std::vector<cv::Point2f> retVal;
  retVal.reserve(corners.size());
  for (size_t i = 0; i < corners.size(); i++) {
    retVal.emplace_back(cv::Point2f{corners[i].first, corners[i].second});
  }
  return retVal;
}

[[maybe_unused]] static cv::Rect GetBoundingRect(
    const std::vector<cv::Point2f>& points) {
  return cv::boundingRect(points);
}

static std::vector<cv::Point2f> ProjectPoints(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    const RotTrlTransform3d& camRt,
    const std::vector<frc::Translation3d>& objectTranslations) {
  std::vector<cv::Point3f> objectPoints = TranslationToTVec(objectTranslations);
  std::vector<cv::Point3f> rvec = RotationToRVec(camRt.GetRotation());
  std::vector<cv::Point3f> tvec = TranslationToTVec({camRt.GetTranslation()});
  cv::Mat cameraMat(cameraMatrix.rows(), cameraMatrix.cols(), CV_64F);
  cv::eigen2cv(cameraMatrix, cameraMat);
  cv::Mat distCoeffsMat(distCoeffs.rows(), distCoeffs.cols(), CV_64F);
  cv::eigen2cv(distCoeffs, distCoeffsMat);
  std::vector<cv::Point2f> imagePoints{};
  cv::projectPoints(objectPoints, rvec, tvec, cameraMat, distCoeffsMat,
                    imagePoints);
  return imagePoints;
}

template <typename T>
static std::vector<T> ReorderCircular(const std::vector<T> elements,
                                      bool backwards, int shiftStart) {
  size_t size = elements.size();
  int dir = backwards ? -1 : 1;
  std::vector<T> reordered{elements};
  for (size_t i = 0; i < size; i++) {
    int index = (i * dir + shiftStart * dir) % size;
    if (index < 0) {
      index = size + index;
    }
    reordered[i] = elements[index];
  }
  return reordered;
}

static frc::Translation3d TranslationEDNToNWU(const frc::Translation3d& trl) {
  return trl.RotateBy(EDN_TO_NWU);
}

static frc::Rotation3d RotationEDNToNWU(const frc::Rotation3d& rot) {
  return -EDN_TO_NWU + (rot + EDN_TO_NWU);
}

static frc::Translation3d TVecToTranslation(const cv::Mat& tvecInput) {
  cv::Vec3f data{};
  cv::Mat wrapped{tvecInput.rows, tvecInput.cols, CV_32F};
  tvecInput.convertTo(wrapped, CV_32F);
  data = wrapped.at<cv::Vec3f>(cv::Point{0, 0});
  return TranslationEDNToNWU(frc::Translation3d{units::meter_t{data[0]},
                                                units::meter_t{data[1]},
                                                units::meter_t{data[2]}});
}

static frc::Rotation3d RVecToRotation(const cv::Mat& rvecInput) {
  cv::Vec3f data{};
  cv::Mat wrapped{rvecInput.rows, rvecInput.cols, CV_32F};
  rvecInput.convertTo(wrapped, CV_32F);
  data = wrapped.at<cv::Vec3f>(cv::Point{0, 0});
  return RotationEDNToNWU(frc::Rotation3d{units::radian_t{data[0]},
                                          units::radian_t{data[1]},
                                          units::radian_t{data[2]}});
}

[[maybe_unused]] static photon::PnpResult SolvePNP_Square(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    std::vector<frc::Translation3d> modelTrls,
    std::vector<cv::Point2f> imagePoints) {
  modelTrls = ReorderCircular(modelTrls, true, -1);
  imagePoints = ReorderCircular(imagePoints, true, -1);
  std::vector<cv::Point3f> objectMat = TranslationToTVec(modelTrls);
  std::vector<cv::Mat> rvecs;
  std::vector<cv::Mat> tvecs;
  cv::Mat rvec = cv::Mat::zeros(3, 1, CV_32F);
  cv::Mat tvec = cv::Mat::zeros(3, 1, CV_32F);
  cv::Mat reprojectionError = cv::Mat::zeros(2, 1, CV_32F);

  cv::Mat cameraMat(cameraMatrix.rows(), cameraMatrix.cols(), CV_32F);
  cv::eigen2cv(cameraMatrix, cameraMat);
  cv::Mat distCoeffsMat(distCoeffs.rows(), distCoeffs.cols(), CV_32F);
  cv::eigen2cv(distCoeffs, distCoeffsMat);

  cv::Vec2d errors{};
  frc::Transform3d best{};
  std::optional<frc::Transform3d> alt{std::nullopt};

  for (int tries = 0; tries < 2; tries++) {
    cv::solvePnPGeneric(objectMat, imagePoints, cameraMat, distCoeffsMat, rvecs,
                        tvecs, false, cv::SOLVEPNP_IPPE_SQUARE, rvec, tvec,
                        reprojectionError);

    errors = reprojectionError.at<cv::Vec2f>(cv::Point{0, 0});
    best = frc::Transform3d{TVecToTranslation(tvecs.at(0)),
                            RVecToRotation(rvecs[0])};

    if (tvecs.size() > 1) {
      alt = frc::Transform3d{TVecToTranslation(tvecs.at(1)),
                             RVecToRotation(rvecs[1])};
    }

    if (!std::isnan(errors[0])) {
      break;
    } else {
      cv::Point2f pt = imagePoints[0];
      pt.x -= 0.001f;
      pt.y -= 0.001f;
      imagePoints[0] = pt;
    }
  }

  if (std::isnan(errors[0])) {
    fmt::print("SolvePNP_Square failed!\n");
  }
  if (alt) {
    photon::PnpResult result;
    result.best = best;
    result.alt = alt.value();
    result.ambiguity = errors[0] / errors[1];
    result.bestReprojErr = errors[0];
    result.altReprojErr = errors[1];
    result.isPresent = true;
    return result;
  } else {
    photon::PnpResult result;
    result.best = best;
    result.bestReprojErr = errors[0];
    result.isPresent = true;
    return result;
  }
}

[[maybe_unused]] static photon::PnpResult SolvePNP_SQPNP(
    const Eigen::Matrix<double, 3, 3>& cameraMatrix,
    const Eigen::Matrix<double, 8, 1>& distCoeffs,
    std::vector<frc::Translation3d> modelTrls,
    std::vector<cv::Point2f> imagePoints) {
  std::vector<cv::Point3f> objectMat = TranslationToTVec(modelTrls);
  std::vector<cv::Mat> rvecs{};
  std::vector<cv::Mat> tvecs{};
  cv::Mat rvec = cv::Mat::zeros(3, 1, CV_32F);
  cv::Mat tvec = cv::Mat::zeros(3, 1, CV_32F);
  cv::Mat reprojectionError = cv::Mat::zeros(2, 1, CV_32F);

  cv::Mat cameraMat(cameraMatrix.rows(), cameraMatrix.cols(), CV_64F);
  cv::eigen2cv(cameraMatrix, cameraMat);
  cv::Mat distCoeffsMat(distCoeffs.rows(), distCoeffs.cols(), CV_64F);
  cv::eigen2cv(distCoeffs, distCoeffsMat);

  float error = 0;
  frc::Transform3d best{};

  cv::solvePnPGeneric(objectMat, imagePoints, cameraMat, distCoeffsMat, rvecs,
                      tvecs, false, cv::SOLVEPNP_SQPNP, rvec, tvec,
                      reprojectionError);

  error = reprojectionError.at<float>(cv::Point{0, 0});
  best = frc::Transform3d{TVecToTranslation(tvecs.at(0)),
                          RVecToRotation(rvecs[0])};

  if (std::isnan(error)) {
    fmt::print("SolvePNP_Square failed!\n");
  }
  photon::PnpResult result;
  result.best = best;
  result.bestReprojErr = error;
  result.isPresent = true;
  return result;
}
}  // namespace OpenCVHelp
}  // namespace photon
