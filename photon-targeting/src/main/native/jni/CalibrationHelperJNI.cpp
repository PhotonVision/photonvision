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

#include <org_photonvision_jni_CalibrationHelper.h>

#include <cstdio>

#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgcodecs.hpp>



extern "C" {

/*
 * Class:     org_photonvision_jni_CalibrationHelper
 * Method:    Create
 * Signature: (IIJD)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_CalibrationHelper_Create
  (JNIEnv*, jclass, jint, jint, jlong, jdouble)
{
  
  cv::Mat mat = cv::imread(
      "/home/matt/Documents/GitHub/photonvision/test-resources/testimages/2022/"
      "WPI/FarLaunchpad13ft10in.png");

  std::printf("mat size %i %i\n", mat.rows, mat.cols);

  return 0;
}

}  // extern "C"
