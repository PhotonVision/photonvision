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

#include <org_photonvision_jni_ArucoNanoV5Detector.h>

#include <vector>

#include <opencv2/core/mat.hpp>
#include <opencv2/core/types.hpp>
#include <opencv2/core/utility.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/objdetect/aruco_dictionary.hpp>
#include <wpi/jni_util.h>

#include "photon/aruco_nano.h"

using namespace wpi::java;
using namespace aruconano;

/*
 * Class:     org_photonvision_jni_ArucoNanoV5Detector
 * Method:    detect
 * Signature: (J)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_org_photonvision_jni_ArucoNanoV5Detector_detect
  (JNIEnv* env, jclass, jlong matPtr)
{
  cv::Mat* mat = reinterpret_cast<cv::Mat*>(matPtr);
  unsigned int maxAttemptsPerCandidate = 10;
  auto markers = MarkerDetector::detect(*mat, maxAttemptsPerCandidate,
                                        TagDicts::APRILTAG_36h11);

  std::vector<double> corners;
  for (auto& marker : markers) {
    corners.insert(corners.end(),
                   {marker[0].x, marker[0].y, marker[1].x, marker[1].y,
                    marker[2].x, marker[2].y, marker[3].x, marker[3].y,
                    static_cast<double>(marker.id)});
  }
  return MakeJDoubleArray(env, corners);
}
