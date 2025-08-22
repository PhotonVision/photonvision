/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright
 * ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.photonvision.jni;

import edu.wpi.first.apriltag.AprilTagDetection;

public class GpuDetectorJNI {
    static boolean libraryLoaded = false;

    static {
        if (!libraryLoaded) System.loadLibrary("971apriltag");
        libraryLoaded = true;
    }

    public static native long createGpuDetector(int width, int height);

    public static native void destroyGpuDetector(long handle);

    public static native void setparams(
            long handle,
            double fx,
            double cx,
            double fy,
            double cy,
            double k1,
            double k2,
            double p1,
            double p2,
            double k3);

    public static native AprilTagDetection[] processimage(long handle, long p);
}
