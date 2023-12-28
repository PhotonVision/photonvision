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

#include <cscore_cv.h>

#include <algorithm>
#include <numeric>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

#include <frc/apriltag/AprilTag.h>
#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/objdetect.hpp>
#include <units/length.h>

#include "SimCameraProperties.h"
#include "photon/estimation/RotTrlTransform3d.h"

namespace mathutil {
template <typename T>
int sgn(T val) {
  return (T(0) < val) - (val < T(0));
}
}  // namespace mathutil

namespace photon {
namespace VideoSimUtil {
static constexpr int kNumTags36h11 = 30;

static constexpr units::meter_t fieldLength{16.54175_m};
static constexpr units::meter_t fieldWidth{8.0137_m};

static cv::Mat Get36h11TagImage(int id) {
  wpi::RawFrame frame;
  frc::AprilTag::Generate36h11AprilTagImage(&frame, id);
  cv::Mat markerImage{frame.height, frame.width, CV_8UC1, frame.data,
                      static_cast<size_t>(frame.stride)};
  cv::Mat markerClone = markerImage.clone();
  return markerClone;
}

static std::unordered_map<int, cv::Mat> LoadAprilTagImages() {
  std::unordered_map<int, cv::Mat> retVal{};
  for (int i = 0; i < kNumTags36h11; i++) {
    cv::Mat tagImage = Get36h11TagImage(i);
    retVal[i] = tagImage;
  }
  return retVal;
}

static std::vector<cv::Point2f> GetImageCorners(const cv::Size& size) {
  std::vector<cv::Point2f> retVal{};
  retVal.emplace_back(cv::Point2f{-0.5f, -0.5f});
  retVal.emplace_back(cv::Point2f{size.width - 0.5f, -0.5f});
  retVal.emplace_back(cv::Point2f{size.width - 0.5f, size.height - 0.5f});
  retVal.emplace_back(cv::Point2f{-0.5f, size.height - 0.5f});
  return retVal;
}

static std::vector<cv::Point2f> Get36h11MarkerPts(int scale) {
  cv::Rect2f roi36h11{cv::Point2f{1, 1}, cv::Point2f{8, 8}};
  roi36h11.x *= scale;
  roi36h11.y *= scale;
  roi36h11.width *= scale;
  roi36h11.height *= scale;
  std::vector<cv::Point2f> pts = GetImageCorners(roi36h11.size());
  for (size_t i = 0; i < pts.size(); i++) {
    cv::Point2f pt = pts[i];
    pts[i] = cv::Point2f{roi36h11.tl().x + pt.x, roi36h11.tl().y + pt.y};
  }
  return pts;
}

static std::vector<cv::Point2f> Get36h11MarkerPts() {
  return Get36h11MarkerPts(1);
}

static const std::unordered_map<int, cv::Mat> kTag36h11Images =
    LoadAprilTagImages();
static const std::vector<cv::Point2f> kTag36h11MarkPts = Get36h11MarkerPts();

[[maybe_unused]] static void UpdateVideoProp(cs::CvSource& video,
                                             const SimCameraProperties& prop) {
  video.SetResolution(prop.GetResWidth(), prop.GetResHeight());
  video.SetFPS(prop.GetFPS().to<int>());
}

[[maybe_unused]] static void Warp165h5TagImage(
    int tagId, const std::vector<cv::Point2f>& dstPoints, bool antialiasing,
    cv::Mat& destination) {
  if (!kTag36h11Images.contains(tagId)) {
    return;
  }
  cv::Mat tagImage = kTag36h11Images.at(tagId);
  std::vector<cv::Point2f> tagPoints{kTag36h11MarkPts};
  std::vector<cv::Point2f> tagImageCorners{GetImageCorners(tagImage.size())};
  std::vector<cv::Point2f> dstPointMat = dstPoints;
  cv::Rect boundingRect = cv::boundingRect(dstPointMat);
  cv::Mat perspecTrf = cv::getPerspectiveTransform(tagPoints, dstPointMat);
  std::vector<cv::Point2f> extremeCorners{};
  cv::perspectiveTransform(tagImageCorners, extremeCorners, perspecTrf);
  boundingRect = cv::boundingRect(extremeCorners);

  double warpedContourArea = cv::contourArea(extremeCorners);
  double warpedTagUpscale =
      std::sqrt(warpedContourArea) / std::sqrt(tagImage.size().area());
  int warpStrat = cv::INTER_NEAREST;

  int supersampling = 6;
  supersampling = static_cast<int>(std::ceil(supersampling / warpedTagUpscale));
  supersampling = std::max(std::min(supersampling, 10), 1);

  cv::Mat scaledTagImage{};
  if (warpedTagUpscale > 2.0) {
    warpStrat = cv::INTER_LINEAR;
    int scaleFactor = static_cast<int>(warpedTagUpscale / 3.0) + 2;
    scaleFactor = std::max(std::min(scaleFactor, 40), 1);
    scaleFactor *= supersampling;
    cv::resize(tagImage, scaledTagImage, cv::Size{}, scaleFactor, scaleFactor,
               cv::INTER_NEAREST);
    tagPoints = Get36h11MarkerPts(scaleFactor);
  } else {
    scaledTagImage = tagImage;
  }

  boundingRect.x -= 1;
  boundingRect.y -= 1;
  boundingRect.width += 2;
  boundingRect.height += 2;
  if (boundingRect.x < 0) {
    boundingRect.width += boundingRect.x;
    boundingRect.x = 0;
  }
  if (boundingRect.y < 0) {
    boundingRect.height += boundingRect.y;
    boundingRect.y = 0;
  }
  boundingRect.width =
      std::min(destination.size().width - boundingRect.x, boundingRect.width);
  boundingRect.height =
      std::min(destination.size().height - boundingRect.y, boundingRect.height);
  if (boundingRect.width <= 0 || boundingRect.height <= 0) {
    return;
  }

  std::vector<cv::Point2f> scaledDstPts{};
  if (supersampling > 1) {
    cv::multiply(dstPointMat,
                 cv::Scalar{static_cast<double>(supersampling),
                            static_cast<double>(supersampling)},
                 scaledDstPts);
    boundingRect.x *= supersampling;
    boundingRect.y *= supersampling;
    boundingRect.width *= supersampling;
    boundingRect.height *= supersampling;
  } else {
    scaledDstPts = dstPointMat;
  }

  cv::subtract(scaledDstPts,
               cv::Scalar{static_cast<double>(boundingRect.tl().x),
                          static_cast<double>(boundingRect.tl().y)},
               scaledDstPts);
  perspecTrf = cv::getPerspectiveTransform(tagPoints, scaledDstPts);

  cv::Mat tempRoi{};
  cv::warpPerspective(scaledTagImage, tempRoi, perspecTrf, boundingRect.size(),
                      warpStrat);

  if (supersampling > 1) {
    boundingRect.x /= supersampling;
    boundingRect.y /= supersampling;
    boundingRect.width /= supersampling;
    boundingRect.height /= supersampling;
    cv::resize(tempRoi, tempRoi, boundingRect.size(), 0, 0, cv::INTER_AREA);
  }

  cv::Mat tempMask{cv::Mat::zeros(tempRoi.size(), CV_8UC1)};
  cv::subtract(extremeCorners,
               cv::Scalar{static_cast<float>(boundingRect.tl().x),
                          static_cast<float>(boundingRect.tl().y)},
               extremeCorners);
  cv::Point2f tempCenter{};
  tempCenter.x =
      std::accumulate(extremeCorners.begin(), extremeCorners.end(), 0.0,
                      [extremeCorners](float acc, const cv::Point2f& p2) {
                        return acc + p2.x / extremeCorners.size();
                      });
  tempCenter.y =
      std::accumulate(extremeCorners.begin(), extremeCorners.end(), 0.0,
                      [extremeCorners](float acc, const cv::Point2f& p2) {
                        return acc + p2.y / extremeCorners.size();
                      });

  for (auto& corner : extremeCorners) {
    float xDiff = corner.x - tempCenter.x;
    float yDiff = corner.y - tempCenter.y;
    xDiff += 1 * mathutil::sgn(xDiff);
    yDiff += 1 * mathutil::sgn(yDiff);
    corner = cv::Point2f{tempCenter.x + xDiff, tempCenter.y + yDiff};
  }

  std::vector<cv::Point> extremeCornerInt{extremeCorners.begin(),
                                          extremeCorners.end()};
  cv::fillConvexPoly(tempMask, extremeCornerInt, cv::Scalar{255});

  cv::copyTo(tempRoi, destination(boundingRect), tempMask);
}

static double GetScaledThickness(double thickness480p,
                                 const cv::Mat& destinationMat) {
  double scaleX = destinationMat.size().width / 640.0;
  double scaleY = destinationMat.size().height / 480.0;
  double minScale = std::min(scaleX, scaleY);
  return std::max(thickness480p * minScale, 1.0);
}

[[maybe_unused]] static void DrawInscribedEllipse(
    const std::vector<cv::Point2f>& dstPoints, const cv::Scalar& color,
    cv::Mat& destination) {
  cv::RotatedRect rect = OpenCVHelp::GetMinAreaRect(dstPoints);
  cv::ellipse(destination, rect, color, -1, cv::LINE_AA);
}

static void DrawPoly(const std::vector<cv::Point2f>& dstPoints, int thickness,
                     const cv::Scalar& color, bool isClosed,
                     cv::Mat& destination) {
  std::vector<cv::Point> intDstPoints{dstPoints.begin(), dstPoints.end()};
  std::vector<std::vector<cv::Point>> listOfListOfPoints;
  listOfListOfPoints.emplace_back(intDstPoints);
  if (thickness > 0) {
    cv::polylines(destination, listOfListOfPoints, isClosed, color, thickness,
                  cv::LINE_AA);
  } else {
    cv::fillPoly(destination, listOfListOfPoints, color, cv::LINE_AA);
  }
}

[[maybe_unused]] static void DrawTagDetection(
    int id, const std::vector<cv::Point2f>& dstPoints, cv::Mat& destination) {
  double thickness = GetScaledThickness(1, destination);
  DrawPoly(dstPoints, static_cast<int>(thickness), cv::Scalar{0, 0, 255}, true,
           destination);
  cv::Rect2d rect{cv::boundingRect(dstPoints)};
  cv::Point2d textPt{rect.x + rect.width, rect.y};
  textPt.x += thickness;
  textPt.y += thickness;
  cv::putText(destination, std::to_string(id), textPt, cv::FONT_HERSHEY_PLAIN,
              1.5 * thickness, cv::Scalar{0, 200, 0},
              static_cast<int>(thickness), cv::LINE_AA);
}

static std::vector<std::vector<frc::Translation3d>> GetFieldWallLines() {
  std::vector<std::vector<frc::Translation3d>> list;

  const units::meter_t sideHt = 19.5_in;
  const units::meter_t driveHt = 35_in;
  const units::meter_t topHt = 78_in;

  // field floor
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{0_m, 0_m, 0_m},
      frc::Translation3d{fieldLength, 0_m, 0_m},
      frc::Translation3d{fieldLength, fieldWidth, 0_m},
      frc::Translation3d{0_m, fieldWidth, 0_m},
      frc::Translation3d{0_m, 0_m, 0_m}});

  // right side wall
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{0_m, 0_m, 0_m}, frc::Translation3d{0_m, 0_m, sideHt},
      frc::Translation3d{fieldLength, 0_m, sideHt},
      frc::Translation3d{fieldLength, 0_m, 0_m}});

  // red driverstation
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{fieldLength, 0_m, sideHt},
      frc::Translation3d{fieldLength, 0_m, topHt},
      frc::Translation3d{fieldLength, fieldWidth, topHt},
      frc::Translation3d{fieldLength, fieldWidth, sideHt},
  });
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{fieldLength, 0_m, driveHt},
      frc::Translation3d{fieldLength, fieldWidth, driveHt}});

  // left side wall
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{0_m, fieldWidth, 0_m},
      frc::Translation3d{0_m, fieldWidth, sideHt},
      frc::Translation3d{fieldLength, fieldWidth, sideHt},
      frc::Translation3d{fieldLength, fieldWidth, 0_m}});

  // blue driverstation
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{0_m, 0_m, sideHt},
      frc::Translation3d{0_m, 0_m, topHt},
      frc::Translation3d{0_m, fieldWidth, topHt},
      frc::Translation3d{0_m, fieldWidth, sideHt},
  });
  list.emplace_back(std::vector<frc::Translation3d>{
      frc::Translation3d{0_m, 0_m, driveHt},
      frc::Translation3d{0_m, fieldWidth, driveHt}});

  return list;
}

static std::vector<std::vector<frc::Translation3d>> GetFieldFloorLines(
    int subdivisions) {
  std::vector<std::vector<frc::Translation3d>> list;
  const units::meter_t subLength = fieldLength / subdivisions;
  const units::meter_t subWidth = fieldWidth / subdivisions;

  for (int i = 0; i < subdivisions; i++) {
    list.emplace_back(std::vector<frc::Translation3d>{
        frc::Translation3d{0_m, subWidth * (i + 1), 0_m},
        frc::Translation3d{fieldLength, subWidth * (i + 1), 0_m}});
    list.emplace_back(std::vector<frc::Translation3d>{
        frc::Translation3d{subLength * (i + 1), 0_m, 0_m},
        frc::Translation3d{subLength * (i + 1), fieldWidth, 0_m}});
  }
  return list;
}

static std::vector<std::vector<cv::Point2f>> PolyFrom3dLines(
    const RotTrlTransform3d& camRt, const SimCameraProperties& prop,
    const std::vector<frc::Translation3d>& trls, double resolution,
    bool isClosed, cv::Mat& destination) {
  resolution = std::hypot(destination.size().height, destination.size().width) *
               resolution;
  std::vector<frc::Translation3d> pts{trls};
  if (isClosed) {
    pts.emplace_back(pts[0]);
  }
  std::vector<std::vector<cv::Point2f>> polyPointList{};

  for (size_t i = 0; i < pts.size() - 1; i++) {
    frc::Translation3d pta = pts[i];
    frc::Translation3d ptb = pts[i + 1];

    std::pair<std::optional<double>, std::optional<double>> inter =
        prop.GetVisibleLine(camRt, pta, ptb);
    if (!inter.second) {
      continue;
    }

    double inter1 = inter.first.value();
    double inter2 = inter.second.value();
    frc::Translation3d baseDelta = ptb - pta;
    frc::Translation3d old_pta = pta;
    if (inter1 > 0) {
      pta = old_pta + baseDelta * inter1;
    }
    if (inter2 < 1) {
      ptb = old_pta + baseDelta * inter2;
    }
    baseDelta = ptb - pta;

    std::vector<cv::Point2f> poly = OpenCVHelp::ProjectPoints(
        prop.GetIntrinsics(), prop.GetDistCoeffs(), camRt, {pta, ptb});
    cv::Point2d pxa = poly[0];
    cv::Point2d pxb = poly[1];

    double pxDist = std::hypot(pxb.x - pxa.x, pxb.y - pxa.y);
    int subdivisions = static_cast<int>(pxDist / resolution);
    frc::Translation3d subDelta = baseDelta / (subdivisions + 1);
    std::vector<frc::Translation3d> subPts{};
    for (int j = 0; j < subdivisions; j++) {
      subPts.emplace_back(pta + (subDelta * (j + 1)));
    }
    if (subPts.size() > 0) {
      std::vector<cv::Point2f> toAdd = OpenCVHelp::ProjectPoints(
          prop.GetIntrinsics(), prop.GetDistCoeffs(), camRt, subPts);
      poly.insert(poly.begin() + 1, toAdd.begin(), toAdd.end());
    }

    polyPointList.emplace_back(poly);
  }

  return polyPointList;
}

[[maybe_unused]] static void DrawFieldWireFrame(
    const RotTrlTransform3d& camRt, const SimCameraProperties& prop,
    double resolution, double wallThickness, const cv::Scalar& wallColor,
    int floorSubdivisions, double floorThickness, const cv::Scalar& floorColor,
    cv::Mat& destination) {
  for (const auto& trls : GetFieldFloorLines(floorSubdivisions)) {
    auto polys =
        PolyFrom3dLines(camRt, prop, trls, resolution, false, destination);
    for (const auto& poly : polys) {
      DrawPoly(poly,
               static_cast<int>(
                   std::round(GetScaledThickness(floorThickness, destination))),
               floorColor, false, destination);
    }
  }
  for (const auto& trls : GetFieldWallLines()) {
    auto polys =
        PolyFrom3dLines(camRt, prop, trls, resolution, false, destination);
    for (const auto& poly : polys) {
      DrawPoly(poly,
               static_cast<int>(
                   std::round(GetScaledThickness(wallThickness, destination))),
               wallColor, false, destination);
    }
  }
}
}  // namespace VideoSimUtil
}  // namespace photon
