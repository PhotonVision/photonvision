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
