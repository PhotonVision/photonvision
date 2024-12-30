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

#include <opencv2/core/mat.hpp>
#include <wpi/jni_util.h>

#include "cscore_raw.h"
#include "org_photonvision_jni_CscoreExtras.h"

// from wpilib, licensed under the wpilib BSD license
using namespace wpi::java;
static JException videoEx;
static const JExceptionInit exceptions[] = {
    {"edu/wpi/first/cscore/VideoException", &videoEx}};
static void ReportError(JNIEnv* env, CS_Status status) {
  if (status == CS_OK) {
    return;
  }
  std::string_view msg;
  std::string msgBuf;
  switch (status) {
    case CS_PROPERTY_WRITE_FAILED:
      msg = "property write failed";
      break;
    case CS_INVALID_HANDLE:
      msg = "invalid handle";
      break;
    case CS_WRONG_HANDLE_SUBTYPE:
      msg = "wrong handle subtype";
      break;
    case CS_INVALID_PROPERTY:
      msg = "invalid property";
      break;
    case CS_WRONG_PROPERTY_TYPE:
      msg = "wrong property type";
      break;
    case CS_READ_FAILED:
      msg = "read failed";
      break;
    case CS_SOURCE_IS_DISCONNECTED:
      msg = "source is disconnected";
      break;
    case CS_EMPTY_VALUE:
      msg = "empty value";
      break;
    case CS_BAD_URL:
      msg = "bad URL";
      break;
    case CS_TELEMETRY_NOT_ENABLED:
      msg = "telemetry not enabled";
      break;
    default: {
      msgBuf = fmt::format("unknown error code={}", status);
      msg = msgBuf;
      break;
    }
  }
  videoEx.Throw(env, msg);
}
static inline bool CheckStatus(JNIEnv* env, CS_Status status) {
  if (status != CS_OK) {
    ReportError(env, status);
  }
  return status == CS_OK;
}

static inline int GetCVFormat(int wpiFormat) {
  auto format = static_cast<WPI_PixelFormat>(wpiFormat);

  switch (format) {
    case WPI_PIXFMT_YUYV:
    case WPI_PIXFMT_RGB565:
    case WPI_PIXFMT_Y16:
    case WPI_PIXFMT_UYVY:
      return CV_8UC2;
    case WPI_PIXFMT_BGR:
      return CV_8UC3;
    case WPI_PIXFMT_BGRA:
      return CV_8UC4;
    case WPI_PIXFMT_GRAY:
    case WPI_PIXFMT_MJPEG:
    case WPI_PIXFMT_UNKNOWN:
    default:
      return CV_8UC1;
  }
}

#include <cstdio>

extern "C" {

/*
 * Class:     org_photonvision_jni_CscoreExtras
 * Method:    grabRawSinkFrameTimeoutLastTime
 * Signature: (IJDJ)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_CscoreExtras_grabRawSinkFrameTimeoutLastTime
  (JNIEnv* env, jclass, jint sink, jlong framePtr, jdouble timeout,
   jlong lastFrameTimeout)
{
  auto* frame = reinterpret_cast<wpi::RawFrame*>(framePtr);
  CS_Status status = 0;

  // fill frame with a copy of the latest frame from the Source
  auto rv = cs::GrabSinkFrameTimeoutLastTime(
      static_cast<CS_Sink>(sink), *frame, timeout, lastFrameTimeout, &status);
  if (!CheckStatus(env, status)) {
    return 0;
  }

  return rv;
}

/*
 * Class:     org_photonvision_jni_CscoreExtras
 * Method:    wrapRawFrame
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_CscoreExtras_wrapRawFrame
  (JNIEnv*, jclass, jlong framePtr)
{
  auto* frame = reinterpret_cast<wpi::RawFrame*>(framePtr);

  return reinterpret_cast<jlong>(new cv::Mat(frame->height, frame->width,
                                             GetCVFormat(frame->pixelFormat),
                                             frame->data, frame->stride));
}

/*
 * Class:     org_photonvision_jni_CscoreExtras
 * Method:    getTimestampSourceNative
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_org_photonvision_jni_CscoreExtras_getTimestampSourceNative
  (JNIEnv*, jclass, jlong framePtr)
{
  auto* frame = reinterpret_cast<wpi::RawFrame*>(framePtr);
  return frame->timestampSrc;
}

}  // extern "C"
