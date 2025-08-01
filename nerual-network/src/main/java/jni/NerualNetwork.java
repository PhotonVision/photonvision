package jni;

public class NerualNetwork {
  static {
    // match.so eg libnerual_network.so
    if (!LibraryLoader.load(NerualNetwork.class, "nerual-network"))
      System.loadLibrary("nerual-network");
  }

  public static native long initModel(String jpath);

  public static native float[] runModel(long jmodel, long jmat);

  public static native void releaseModel(long jmodel);
}
