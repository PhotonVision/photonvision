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

#include <functional>
#include <string>
#include <vector>

#include <wpi/FileLogger.h>

#include "jni_utils.h"
#include "org_photonvision_jni_QueuedFileLogger.h"

struct QueuedFileLogger {
  // ew ew ew ew ew ew ew ew
  std::vector<char> m_data{};

  std::mutex m_mutex;

  wpi::FileLogger logger;

  explicit QueuedFileLogger(std::string_view file)
      : logger{file, std::bind(&QueuedFileLogger::callback, this,
                               std::placeholders::_1)} {
    // fmt::println("Watching {}", file);
  }

  void callback(std::string_view newline) {
    std::lock_guard lock{m_mutex};
    // fmt::println("FileLogger got: {}", newline);
    m_data.insert(m_data.end(), newline.begin(), newline.end());
  }

  std::vector<char> SwapData() {
    std::vector<char> ret;
    {
      std::lock_guard lock{m_mutex};
      ret.swap(m_data);
    }

    return ret;
  }
};

extern "C" {

/*
 * Class:     org_photonvision_jni_QueuedFileLogger
 * Method:    create
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_QueuedFileLogger_create
  (JNIEnv* env, jclass, jstring name)
{
  const char* c_name{env->GetStringUTFChars(name, 0)};
  std::string cpp_name{c_name};
  jlong ret{reinterpret_cast<jlong>(new QueuedFileLogger(cpp_name))};
  env->ReleaseStringUTFChars(name, c_name);
  return ret;
}

/*
 * Class:     org_photonvision_jni_QueuedFileLogger
 * Method:    destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_QueuedFileLogger_destroy
  (JNIEnv*, jclass, jlong handle)
{
  CHECK_PTR(handle);
  delete reinterpret_cast<QueuedFileLogger*>(handle);
}

/*
 * Class:     org_photonvision_jni_QueuedFileLogger
 * Method:    getNewLines
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_org_photonvision_jni_QueuedFileLogger_getNewLines
  (JNIEnv* env, jclass, jlong handle)
{
  CHECK_PTR_RETURN(handle, nullptr);
  QueuedFileLogger* logger = reinterpret_cast<QueuedFileLogger*>(handle);

  return env->NewStringUTF(logger->SwapData().data());
}
}  // extern "C"
