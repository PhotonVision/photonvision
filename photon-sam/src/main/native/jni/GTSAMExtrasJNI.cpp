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

#include <cstdint>

#include <Eigen/Dense>
#include <gtsam/geometry/Cal3_S2.h>
#include <gtsam/linear/NoiseModel.h>
#include <gtsam/slam/expressions.h>
#include <org_photonvision_jni_GTSAMLocalizerCore.h>
#include <wpi/fields/Field.hpp>
#include <wpi/fields/FieldTag.hpp>
#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/units/length.hpp>

extern "C" {
/*
 * Class:     org_photonvision_jni_GTSAMExtras
 * Method:    CreateGaussianNoiseModelJNI
 * Signature: (I[D)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_GTSAMExtras_CreateGaussianNoiseModelJNI
  (JNIEnv* env, jclass, jint matsize, jdoubleArray covariances)
{
  jdouble* covariancesPtr = env->GetDoubleArrayElements(covariances, nullptr);
  Eigen::MatrixXd covarianceMatrix =
      Eigen::Map<Eigen::MatrixXd>(covariancesPtr, matsize, matsize);
  gtsam::noiseModel::Gaussian::shared_ptr noiseModel =
      gtsam::noiseModel::Gaussian::Covariance(covarianceMatrix);
  env->ReleaseDoubleArrayElements(covariances, covariancesPtr, 0);
  return reinterpret_cast<jlong>(
      new gtsam::noiseModel::Gaussian::shared_ptr(noiseModel));
}

/*
 * Class:     org_photonvision_jni_GTSAMExtras
 * Method:    DestroyGaussianNoiseModelJNI
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMExtras_DestroyGaussianNoiseModelJNI
  (JNIEnv* env, jclass, jlong handle)
{
  delete reinterpret_cast<gtsam::noiseModel::Gaussian::shared_ptr*>(handle);
}

/*
 * Class:     org_photonvision_jni_GTSAMExtras
 * Method:    CreateCal3S2JNI
 * Signature: (DDDDD)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_GTSAMExtras_CreateCal3S2JNI
  (JNIEnv* env, jclass, jdouble fx, jdouble fy, jdouble s, jdouble u0,
   jdouble v0)
{
  return reinterpret_cast<jlong>(
      new gtsam::Cal3_S2_(gtsam::Cal3_S2(fx, fy, s, u0, v0)));
}

/*
 * Class:     org_photonvision_jni_GTSAMExtras
 * Method:    DestroyCal3S2JNI
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMExtras_DestroyCal3S2JNI
  (JNIEnv* env, jclass, jlong handle)
{
  delete reinterpret_cast<gtsam::Cal3_S2_*>(handle);
}
}  // extern "C"
