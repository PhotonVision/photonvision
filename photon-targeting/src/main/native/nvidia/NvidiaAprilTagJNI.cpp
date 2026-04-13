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

#include <jni.h>

#include <cmath>
#include <memory>
#include <string>
#include <vector>

#include <opencv2/core/mat.hpp>

#include "nvidia/NvidiaAprilTagApi.h"

namespace {

constexpr uint32_t kMaxTags = 1024;

struct DetectorContext {
  cuAprilTagsHandle detector = nullptr;
  void* deviceBuffer = nullptr;
  size_t devicePitch = 0;
  uint32_t width = 0;
  uint32_t height = 0;
};

void ThrowRuntimeException(JNIEnv* env, const std::string& message) {
  auto* exceptionClass = env->FindClass("java/lang/RuntimeException");
  if (exceptionClass != nullptr) {
    env->ThrowNew(exceptionClass, message.c_str());
  }
}

bool CheckCuda(JNIEnv* env, cudaError_t status, const std::string& action) {
  if (status == cudaSuccess) {
    return true;
  }

  ThrowRuntimeException(
      env, action + " failed: " + std::string(cudaGetErrorString(status)));
  return false;
}

void ComputeCenter(const cuAprilTagsID_t& detection, double* centerX,
                   double* centerY) {
  const double averageCenterX =
      (detection.corners[0].x + detection.corners[1].x +
       detection.corners[2].x + detection.corners[3].x) /
      4.0;
  const double averageCenterY =
      (detection.corners[0].y + detection.corners[1].y +
       detection.corners[2].y + detection.corners[3].y) /
      4.0;

  const double x1 = detection.corners[0].x;
  const double y1 = detection.corners[0].y;
  const double x2 = detection.corners[2].x;
  const double y2 = detection.corners[2].y;
  const double x3 = detection.corners[1].x;
  const double y3 = detection.corners[1].y;
  const double x4 = detection.corners[3].x;
  const double y4 = detection.corners[3].y;

  const double denominator = ((x1 - x2) * (y3 - y4)) -
                             ((y1 - y2) * (x3 - x4));
  if (std::abs(denominator) < 1e-6) {
    *centerX = averageCenterX;
    *centerY = averageCenterY;
    return;
  }

  const double determinant1 = (x1 * y2) - (y1 * x2);
  const double determinant2 = (x3 * y4) - (y3 * x4);
  *centerX = ((determinant1 * (x3 - x4)) - ((x1 - x2) * determinant2)) /
             denominator;
  *centerY = ((determinant1 * (y3 - y4)) - ((y1 - y2) * determinant2)) /
             denominator;
}

}  // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_photonvision_jni_NvidiaAprilTagJNI_isRuntimeSupported(JNIEnv* env,
                                                               jclass) {
  int deviceCount = 0;
  if (!CheckCuda(env, cudaGetDeviceCount(&deviceCount),
                 "Checking available CUDA devices")) {
    return JNI_FALSE;
  }

  return deviceCount > 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_NvidiaAprilTagJNI_createDetector(JNIEnv* env, jclass,
                                                           jint width,
                                                           jint height,
                                                           jint tileSize) {
  if (width <= 0 || height <= 0 || tileSize <= 0) {
    ThrowRuntimeException(env, "NVIDIA AprilTag detector dimensions are invalid");
    return 0;
  }

  auto context = std::make_unique<DetectorContext>();
  context->width = static_cast<uint32_t>(width);
  context->height = static_cast<uint32_t>(height);

  if (!CheckCuda(env,
                 cudaMallocPitch(&context->deviceBuffer, &context->devicePitch,
                                 sizeof(uchar3) * context->width,
                                 context->height),
                 "Allocating CUDA input buffer")) {
    return 0;
  }

  const int error = nvCreateAprilTagsDetector(
      &context->detector, context->width, context->height,
      static_cast<uint32_t>(tileSize), NVAT_TAG36H11, nullptr, 0.0f);
  if (error != 0) {
    cudaFree(context->deviceBuffer);
    ThrowRuntimeException(env, "Failed to create NVIDIA AprilTag detector");
    return 0;
  }

  return reinterpret_cast<jlong>(context.release());
}

JNIEXPORT void JNICALL
Java_org_photonvision_jni_NvidiaAprilTagJNI_destroyDetector(JNIEnv*, jclass,
                                                            jlong handle) {
  auto* context = reinterpret_cast<DetectorContext*>(handle);
  if (context == nullptr) {
    return;
  }

  if (context->detector != nullptr) {
    cuAprilTagsDestroy(context->detector);
  }

  if (context->deviceBuffer != nullptr) {
    cudaFree(context->deviceBuffer);
  }

  delete context;
}

JNIEXPORT jobjectArray JNICALL
Java_org_photonvision_jni_NvidiaAprilTagJNI_detect(JNIEnv* env, jclass,
                                                   jlong handle,
                                                   jlong inputMatPtr) {
  auto* context = reinterpret_cast<DetectorContext*>(handle);
  auto* inputMat = reinterpret_cast<cv::Mat*>(inputMatPtr);

  auto* detectionClass =
      env->FindClass("org/photonvision/jni/NvidiaAprilTagDetection");
  if (detectionClass == nullptr) {
    return nullptr;
  }

  if (context == nullptr || inputMat == nullptr || inputMat->empty()) {
    return env->NewObjectArray(0, detectionClass, nullptr);
  }

  if (inputMat->channels() != 3) {
    ThrowRuntimeException(env,
                          "NVIDIA AprilTag detector requires a 3-channel BGR image");
    return nullptr;
  }

  if (static_cast<uint32_t>(inputMat->cols) != context->width ||
      static_cast<uint32_t>(inputMat->rows) != context->height) {
    ThrowRuntimeException(
        env,
        "NVIDIA AprilTag detector input dimensions do not match the allocated detector");
    return nullptr;
  }

  if (!CheckCuda(env,
                 cudaMemcpy2D(context->deviceBuffer, context->devicePitch,
                              inputMat->data, inputMat->step[0],
                              sizeof(uchar3) * context->width, context->height,
                              cudaMemcpyHostToDevice),
                 "Uploading the AprilTag frame to CUDA")) {
    return nullptr;
  }

  cuAprilTagsImageInput_t inputImage;
  inputImage.dev_ptr = reinterpret_cast<uchar3*>(context->deviceBuffer);
  inputImage.pitch = context->devicePitch;
  inputImage.width = static_cast<uint16_t>(context->width);
  inputImage.height = static_cast<uint16_t>(context->height);

  std::vector<cuAprilTagsID_t> tags(kMaxTags);
  uint32_t numTags = 0;
  const int error =
      cuAprilTagsDetect(context->detector, &inputImage, tags.data(), &numTags,
                        static_cast<uint32_t>(tags.size()), nullptr);
  if (error != 0) {
    ThrowRuntimeException(env, "NVIDIA AprilTag detection failed");
    return nullptr;
  }

  auto detectionConstructor =
      env->GetMethodID(detectionClass, "<init>", "(IIDD[D)V");
  if (detectionConstructor == nullptr) {
    return nullptr;
  }

  auto output = env->NewObjectArray(static_cast<jsize>(numTags), detectionClass,
                                    nullptr);
  for (uint32_t i = 0; i < numTags; i++) {
    const auto& detection = tags[i];
    double centerX = 0.0;
    double centerY = 0.0;
    ComputeCenter(detection, &centerX, &centerY);

    jdoubleArray corners = env->NewDoubleArray(8);
    double cornersData[8];
    for (int cornerIdx = 0; cornerIdx < 4; cornerIdx++) {
      cornersData[cornerIdx * 2] = detection.corners[cornerIdx].x;
      cornersData[cornerIdx * 2 + 1] = detection.corners[cornerIdx].y;
    }
    env->SetDoubleArrayRegion(corners, 0, 8, cornersData);

    auto detectionObject =
        env->NewObject(detectionClass, detectionConstructor,
                       static_cast<jint>(detection.id),
                       static_cast<jint>(detection.hamming_error), centerX,
                       centerY, corners);
    env->SetObjectArrayElement(output, static_cast<jsize>(i), detectionObject);
    env->DeleteLocalRef(corners);
    env->DeleteLocalRef(detectionObject);
  }

  return output;
}

}  // extern "C"
