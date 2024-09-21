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

#include <org_photonvision_jni_TimeSyncClient.h>

#include <cstdio>
#include <string>

#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgcodecs.hpp>

#include "net/TimeSyncClientServer.h"

using namespace wpi;

extern "C" {

/*
 * Class:     org_photonvision_jni_TimeSyncClient
 * Method:    create
 * Signature: (Ljava/lang/String;ID)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_TimeSyncClient_create
  (JNIEnv* env, jclass, jstring name, jint port, jdouble interval)
{
  using namespace std::chrono_literals;

  const char* c_name{env->GetStringUTFChars(name, 0)};
  std::string cpp_name{c_name};
  jlong ret{reinterpret_cast<jlong>(
      new TimeSyncClient(cpp_name, static_cast<int>(port),
                         std::chrono::duration_cast<std::chrono::milliseconds>(
                             std::chrono::duration<double>(interval))))};
  env->ReleaseStringUTFChars(name, c_name);
  return ret;
}

/*
 * Class:     org_photonvision_jni_TimeSyncClient
 * Method:    start
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_TimeSyncClient_start
  (JNIEnv*, jclass, jlong ptr)
{
  TimeSyncClient* client = reinterpret_cast<TimeSyncClient*>(ptr);
  client->Start();
}

/*
 * Class:     org_photonvision_jni_TimeSyncClient
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_TimeSyncClient_stop
  (JNIEnv*, jclass, jlong ptr)
{
  TimeSyncClient* client = reinterpret_cast<TimeSyncClient*>(ptr);
  client->Stop();
  delete client;
}

/*
 * Class:     org_photonvision_jni_TimeSyncClient
 * Method:    getOffset
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_TimeSyncClient_getOffset
  (JNIEnv*, jclass, jlong ptr)
{
  TimeSyncClient* client = reinterpret_cast<TimeSyncClient*>(ptr);
  return client->GetOffset();
}

/*
 * Class:     org_photonvision_jni_TimeSyncClient
 * Method:    getLatestMetadata
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_org_photonvision_jni_TimeSyncClient_getLatestMetadata
  (JNIEnv*, jclass)
{
  return nullptr;
}

}  // extern "C"
