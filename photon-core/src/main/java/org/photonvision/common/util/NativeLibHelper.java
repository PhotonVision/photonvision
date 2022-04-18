package org.photonvision.common.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NativeLibHelper {
  private static NativeLibHelper INSTANCE;

  public static NativeLibHelper getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new NativeLibHelper();
    }

    return INSTANCE;
  }

  public final Path NativeLibPath;

  private NativeLibHelper(){
    String home = System.getProperty("user.home");
    NativeLibPath = Paths.get(home, ".pvlibs", "nativecache");
  }


}
