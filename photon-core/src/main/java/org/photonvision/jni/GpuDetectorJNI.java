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
