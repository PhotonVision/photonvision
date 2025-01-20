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
