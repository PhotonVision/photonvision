package jni;

public class Gstreamer {
  static {
    if (!LibraryLoader.load(Gstreamer.class, "gstreamer"))
      System.loadLibrary("gstreamer");
  }

  public native long initCam(String jpipe);

  public native void readMat(long pcap, long pmat);

  public native void releaseCam(long pcap);
}
