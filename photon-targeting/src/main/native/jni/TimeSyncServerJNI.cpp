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

#include <org_photonvision_jni_TimeSyncClient.h>
#include <org_photonvision_jni_TimeSyncServer.h>

#include <cstdio>

#include "jni_utils.h"
#include "net/TimeSyncServer.h"

using namespace wpi::tsp;

extern "C" {

/*
 * Class:     org_photonvision_jni_TimeSyncServer
 * Method:    create
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_org_photonvision_jni_TimeSyncServer_create
  (JNIEnv*, jclass, jint port)
{
  return reinterpret_cast<jlong>(new TimeSyncServer(port));
}

/*
 * Class:     org_photonvision_jni_TimeSyncServer
 * Method:    start
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_TimeSyncServer_start
  (JNIEnv*, jclass, jlong ptr)
{
  CHECK_PTR(ptr);
  TimeSyncServer* server = reinterpret_cast<TimeSyncServer*>(ptr);
  server->Start();
}

/*
 * Class:     org_photonvision_jni_TimeSyncServer
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_photonvision_jni_TimeSyncServer_stop
  (JNIEnv*, jclass, jlong ptr)
{
  CHECK_PTR(ptr);
  TimeSyncServer* server = reinterpret_cast<TimeSyncServer*>(ptr);
  server->Stop();
  delete server;
}

}  // extern "C"
