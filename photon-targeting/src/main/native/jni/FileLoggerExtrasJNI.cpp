/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
    // wpi::println("Watching {}", file);
  }

  void callback(std::string_view newline) {
    std::lock_guard lock{m_mutex};
    // wpi::println("FileLogger got: {}", newline);
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
