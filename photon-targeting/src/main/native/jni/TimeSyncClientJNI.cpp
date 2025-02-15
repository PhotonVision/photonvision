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

#include "jni_utils.h"
#include "net/TimeSyncClient.h"

using namespace wpi::tsp;

/**
 * Finds a class and keeps it as a global reference.
 *
 * Use with caution, as the destructor does NOT call DeleteGlobalRef due to
 * potential shutdown issues with doing so.
 */
class JClass {
 public:
  JClass() = default;

  JClass(JNIEnv* env, const char* name) {
    jclass local = env->FindClass(name);
    if (!local) {
      return;
    }
    m_cls = static_cast<jclass>(env->NewGlobalRef(local));
    env->DeleteLocalRef(local);
  }

  void free(JNIEnv* env) {
    if (m_cls) {
      env->DeleteGlobalRef(m_cls);
    }
    m_cls = nullptr;
  }

  explicit operator bool() const { return m_cls; }

  operator jclass() const { return m_cls; }

 protected:
  jclass m_cls = nullptr;
};

static JClass metadataClass;
static jmethodID metadataCtor;

// TODO - only one onload allowed
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  metadataClass =
      JClass(env, "org/photonvision/jni/TimeSyncClient$PingMetadata");

  if (!metadataClass) {
    std::printf("Couldn't find class!");
    return JNI_ERR;
  }

  metadataCtor = env->GetMethodID(metadataClass, "<init>", "(JJJJJ)V");
  if (!metadataCtor) {
    std::printf("Couldn't find constructor!");
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}

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
  CHECK_PTR(ptr);
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
  CHECK_PTR(ptr);
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
  CHECK_PTR_RETURN(ptr, 0);
  TimeSyncClient* client = reinterpret_cast<TimeSyncClient*>(ptr);
  return client->GetOffset();
}

/*
 * Class:     org_photonvision_jni_TimeSyncClient
 * Method:    getLatestMetadata
 * Signature: (J)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_org_photonvision_jni_TimeSyncClient_getLatestMetadata
  (JNIEnv* env, jclass, jlong ptr)
{
  CHECK_PTR_RETURN(ptr, nullptr);
  TimeSyncClient* client = reinterpret_cast<TimeSyncClient*>(ptr);
  auto m{client->GetMetadata()};
  auto ret = env->NewObject(metadataClass, metadataCtor, m.offset, m.pingsSent,
                            m.pongsReceived, m.lastPongTime, m.rtt2);

  return ret;
}

}  // extern "C"
