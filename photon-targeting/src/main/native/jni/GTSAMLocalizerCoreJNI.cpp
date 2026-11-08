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

#include <string>
#include <vector>

#include <org_photonvision_jni_GTSAMLocalizerCore.h>
#include <wpi/fields/Field.hpp>
#include <wpi/fields/FieldTag.hpp>
#include <wpi/math/geometry/Pose3d.hpp>
#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/units/length.hpp>

#include "gtsam_jni_utils.h"
#include "photon/gtsam/FieldLayout.h"
#include "photon/gtsam/LocalizerCore.h"

extern "C" {

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    createJNI
 * Signature: ([I[DDD[D)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_createJNI
  (JNIEnv* env, jclass, jintArray tagIDs, jdoubleArray tagPoses,
   jdouble fieldWidth, jdouble fieldLength, jdoubleArray tagCorners)
{
  jsize tagIDsLength = env->GetArrayLength(tagIDs);
  jsize tagCornersLength = env->GetArrayLength(tagCorners);

  jint* tagIDsPtr = env->GetIntArrayElements(tagIDs, nullptr);
  jdouble* tagPosesPtr = env->GetDoubleArrayElements(tagPoses, nullptr);
  jdouble* tagCornersPtr = env->GetDoubleArrayElements(tagCorners, nullptr);

  std::vector<wpi::fields::FieldTag> tags;
  // TODO: Verify tag poses length is 6 * tagIDsLength
  for (jsize i = 0; i < tagIDsLength; ++i) {
    jint tagID = tagIDsPtr[i];
    jdouble* tagPosePtr = tagPosesPtr + i * 6;

    wpi::math::Pose3d pose{
        wpi::math::Translation3d(wpi::units::meter_t(tagPosePtr[0]),
                                 wpi::units::meter_t(tagPosePtr[1]),
                                 wpi::units::meter_t(tagPosePtr[2])),
        wpi::math::Rotation3d(wpi::units::radian_t(tagPosePtr[3]),
                              wpi::units::radian_t(tagPosePtr[4]),
                              wpi::units::radian_t(tagPosePtr[5]))};

    tags.emplace_back(tagID, jdoublePtrToPose3d(tagPosePtr));
  }

  wpi::fields::Field field("Photon Field", "2067", "Recycle Rush 2",
                           std::nullopt, wpi::units::meter_t(fieldLength),
                           wpi::units::meter_t(fieldWidth), "FRC", tags);

  std::vector<wpi::math::Translation3d> verts;
  for (jsize i = 0; i < tagCornersLength; i += 3) {
    jdouble* cornerPtr = tagCornersPtr + i;
    verts.emplace_back(wpi::units::meter_t(cornerPtr[0]),
                       wpi::units::meter_t(cornerPtr[1]),
                       wpi::units::meter_t(cornerPtr[2]));
  }

  photon::TargetModel tagModel(verts);

  photon::pvgtsam::FieldLayout fieldLayout =
      photon::pvgtsam::FieldLayout(field, tagModel);
  photon::pvgtsam::LocalizerCore* LocalizerCore_handle =
      new photon::pvgtsam::LocalizerCore(fieldLayout);

  env->ReleaseIntArrayElements(tagIDs, tagIDsPtr, JNI_ABORT);
  env->ReleaseDoubleArrayElements(tagPoses, tagPosesPtr, JNI_ABORT);
  env->ReleaseDoubleArrayElements(tagCorners, tagCornersPtr, JNI_ABORT);

  return reinterpret_cast<jlong>(LocalizerCore_handle);
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    destroyJNI
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_destroyJNI
  (JNIEnv*, jclass, jlong LocalizerCore_handle)
{
  delete reinterpret_cast<photon::pvgtsam::LocalizerCore*>(
      LocalizerCore_handle);
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    ResetJNI
 * Signature: (J[DJJ)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_ResetJNI
  (JNIEnv* env, jclass, jlong LocalizerCore_handle, jdoubleArray wTrArray,
   jlong noise_handle, jlong timeUs)
{
  jdouble* wTrPtr = env->GetDoubleArrayElements(wTrArray, nullptr);
  try {
    reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
        ->Reset(photon::pvgtsam::ResetData{
            jdoublePtrToGtsamPose3(wTrPtr),
            *reinterpret_cast<gtsam::SharedNoiseModel*>(noise_handle),
            static_cast<uint64_t>(timeUs)});
  } catch (const std::exception& e) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
  } catch (...) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                  "Unknown native error");
  }
  env->ReleaseDoubleArrayElements(wTrArray, wTrPtr, JNI_ABORT);
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    AddOdometryJNI
 * Signature: (J[DJJ)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_AddOdometryJNI
  (JNIEnv* env, jclass, jlong LocalizerCore_handle, jdoubleArray wTrArray,
   jlong noise_handle, jlong timeUs)
{
  jdouble* wTrPtr = env->GetDoubleArrayElements(wTrArray, nullptr);
  try {
    reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
        ->AddOdometry(photon::pvgtsam::OdometryObservation{
            static_cast<uint64_t>(timeUs), jdoublePtrToGtsamPose3(wTrPtr),
            *reinterpret_cast<gtsam::SharedNoiseModel*>(noise_handle)});
  } catch (const std::exception& e) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
  } catch (...) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                  "Unknown native error");
  }
  env->ReleaseDoubleArrayElements(wTrArray, wTrPtr, JNI_ABORT);
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    AddTagObservationJNI
 * Signature: (JJI[DJ[DJ)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_AddTagObservationJNI
  (JNIEnv* env, jclass, jlong LocalizerCore_handle, jlong timeUs, jint tagID,
   jdoubleArray corners, jlong cameraCal_handle, jdoubleArray robotTcamera,
   jlong cameraNoise_handle)
{
  jdouble* cornersPtr = env->GetDoubleArrayElements(corners, nullptr);
  jdouble* robotTcameraPtr = env->GetDoubleArrayElements(robotTcamera, nullptr);
  std::vector<gtsam::Point2> cornersVec;
  for (jsize i = 0; i < 4; ++i) {
    cornersVec.emplace_back(
        gtsam::Vector2{cornersPtr[2 * i], cornersPtr[2 * i + 1]});
  }
  try {
    reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
        ->AddTagObservation(photon::pvgtsam::CameraVisionObservation{
            static_cast<uint64_t>(timeUs), tagID, cornersVec,
            *reinterpret_cast<gtsam::Cal3_S2_*>(cameraCal_handle),
            jdoublePtrToGtsamPose3(robotTcameraPtr),
            *reinterpret_cast<gtsam::SharedNoiseModel*>(cameraNoise_handle)});
  } catch (const std::exception& e) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
  } catch (...) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                  "Unknown native error");
  }
  env->ReleaseDoubleArrayElements(corners, cornersPtr, JNI_ABORT);
  env->ReleaseDoubleArrayElements(robotTcamera, robotTcameraPtr, JNI_ABORT);
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    OptimizeJNI
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_OptimizeJNI
  (JNIEnv* env, jclass, jlong LocalizerCore_handle)
{
  try {
    reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
        ->Optimize();
  } catch (const std::exception& e) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
  } catch (...) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"),
                  "Unknown native error");
  }
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    GetLatestWorldToBodyJNI
 * Signature: (J)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_GetLatestWorldToBodyJNI
  (JNIEnv* env, jclass, jlong LocalizerCore_handle)
{
  gtsam::Pose3 latestWorldToBody =
      reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
          ->GetLatestWorldToBody();
  jdouble buf[6];
  writeGtsamPose3ToArray(latestWorldToBody, buf);
  jdoubleArray out = env->NewDoubleArray(6);
  env->SetDoubleArrayRegion(out, 0, 6, buf);
  return out;  // Placeholder I hate JNI
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    GetLastOdomTimeJNI
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_GetLastOdomTimeJNI
  (JNIEnv*, jclass, jlong LocalizerCore_handle)
{
  return reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
      ->GetLastOdomTime();
}

/*
 * Class:     org_photonvision_jni_GTSAMLocalizerCore
 * Method:    GetPoseComponentStdDevsJNI
 * Signature: (J)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_org_photonvision_jni_GTSAMLocalizerCore_GetPoseComponentStdDevsJNI
  (JNIEnv* env, jclass, jlong LocalizerCore_handle)
{
  auto stdDevs =
      reinterpret_cast<photon::pvgtsam::LocalizerCore*>(LocalizerCore_handle)
          ->GetPoseComponentStdDevs();
  // Todo: serialize and return through JNI
  jdoubleArray out = env->NewDoubleArray(6);
  if (out == nullptr)
    return nullptr;  // OOM; a Java exception is already pending
  env->SetDoubleArrayRegion(out, 0, 6, stdDevs.data());
  return out;
}

}  // extern "C"
