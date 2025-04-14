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

#include <fmt/core.h>
#include <fmt/ranges.h>

#include <iostream>
#include <span>
#include <vector>

#include "org_photonvision_jni_ConstrainedSolvepnpJni.h"
#include "photon/constrained_solvepnp/wrap/casadi_wrapper.h"

std::vector<double> convertJDoubleArray(JNIEnv* env, jdoubleArray array) {
  jsize length = env->GetArrayLength(array);
  std::vector<double> result(length);
  env->GetDoubleArrayRegion(array, 0, length, result.data());
  return result;
}

jdoubleArray createJDoubleArray(JNIEnv* env, const std::span<double> vec) {
  jdoubleArray array = env->NewDoubleArray(vec.size());
  env->SetDoubleArrayRegion(array, 0, vec.size(), vec.data());
  return array;
}

extern "C" {
/*
 * Class:     org_photonvision_jni_ConstrainedSolvepnpJni_do
 * Method:    1optimization
 * Signature: (ZI[D[D[D[D[DDD)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_org_photonvision_jni_ConstrainedSolvepnpJni_do_1optimization
  (JNIEnv* env, jclass, jboolean headingFree, jint nTags,
   jdoubleArray cameraCal, jdoubleArray robot2camera, jdoubleArray xGuess,
   jdoubleArray field2points, jdoubleArray pointObservations, jdouble gyro_θ,
   jdouble gyro_error_scale_fac)
{
  auto cameraCalVec = convertJDoubleArray(env, cameraCal);
  auto robot2cameraVec = convertJDoubleArray(env, robot2camera);
  auto xGuessVec = convertJDoubleArray(env, xGuess);
  auto field2pointsVec = convertJDoubleArray(env, field2points);
  auto pointObservationsVec = convertJDoubleArray(env, pointObservations);

  constrained_solvepnp::CameraCalibration cameraCal_{
      cameraCalVec[0],
      cameraCalVec[1],
      cameraCalVec[2],
      cameraCalVec[3],
  };
  Eigen::Map<Eigen::Matrix<double, 4, 4, Eigen::RowMajor>> robot2cameraMat(
      robot2cameraVec.data());
  Eigen::Map<Eigen::Matrix<double, 3, 1>> xGuessMat(xGuessVec.data());
  Eigen::Map<Eigen::Matrix<double, 4, Eigen::Dynamic, Eigen::RowMajor>>
      field2pointsMat(field2pointsVec.data(), 4, field2pointsVec.size() / 4);
  Eigen::Map<Eigen::Matrix<double, 2, Eigen::Dynamic, Eigen::RowMajor>>
      pointObservationsMat(pointObservationsVec.data(), 2,
                           pointObservationsVec.size() / 2);

#if 0
  fmt::println("======================================================");
  fmt::println("Got robot2camera raw {}", robot2cameraVec);
  fmt::println("Camera cal {} {} {} {}", cameraCal_.fx, cameraCal_.fy,
               cameraCal_.cx, cameraCal_.cy);
  fmt::println("{} tags", nTags);
  std::cout << "robot2camera:\n" << robot2cameraMat << std::endl;
  std::cout << "x guess:\n" << xGuessMat << std::endl;
  std::cout << "field2pt:\n" << field2pointsMat << std::endl;
  std::cout << "observations:\n" << pointObservationsMat << std::endl;
#endif

  wpi::expected<constrained_solvepnp::RobotStateMat,
                sleipnir::SolverExitCondition>
      result = constrained_solvepnp::do_optimization(
          headingFree, nTags, cameraCal_, robot2cameraMat, xGuessMat,
          field2pointsMat, pointObservationsMat, gyro_θ, gyro_error_scale_fac);

  if (result) {
    std::vector<double> resultVec{result->data(),
                                  result->data() + result->size()};
    return createJDoubleArray(env, resultVec);
  } else {
    return nullptr;
  }
}
}  // extern "C"
