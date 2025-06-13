package jni;

public class Gstreamer {
  static {
    if (!LibraryLoader.load(Gstreamer.class, "gstreamer"))
      System.loadLibrary("gstreamer");
  }

  public static native long initCam(String jpipe);

  public static native void readMat(long pcap, long pmat);

  public static native void getGrayScale(long praw, long pprocessed);

  public static native void releaseCam(long pcap);
}
