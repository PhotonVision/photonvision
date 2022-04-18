package org.photonvision.vision.apriltag;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.util.RuntimeLoader;
import org.opencv.core.Mat;
import org.photonvision.common.util.NativeLibHelper;

public class AprilTagJNI {
  static final String NATIVE_LIBRARY_NAME = "apriltag";
  static boolean s_libraryLoaded = false;
  static RuntimeLoader<AprilTagJNI> s_loader = null;

  public static class Helper {
    private static AtomicBoolean extractOnStaticLoad = new AtomicBoolean(true);

    public static boolean getExtractOnStaticLoad() {
      return extractOnStaticLoad.get();
    }

    public static void setExtractOnStaticLoad(boolean load) {
      extractOnStaticLoad.set(load);
    }
  }

  static {
    if (Helper.getExtractOnStaticLoad()) {
      try {
        s_loader =
          new RuntimeLoader<>(
            NATIVE_LIBRARY_NAME,
            NativeLibHelper.getInstance().NativeLibPath.toString(),
            AprilTagJNI.class);
        s_loader.loadLibrary();
      } catch (IOException ex) {
        ex.printStackTrace();
        System.exit(1);
      }
      s_libraryLoaded = true;
    }
  }

  public static synchronized void forceLoad() throws IOException {
    if (s_libraryLoaded) {
      return;
    }

    s_loader = new RuntimeLoader<>(
      NATIVE_LIBRARY_NAME,
      NativeLibHelper.getInstance().NativeLibPath.toString(),
      AprilTagJNI.class
    );

    s_loader.loadLibrary();
    s_libraryLoaded = true;
  }

  // Returns a pointer to a apriltag_detector_t
  public static native long AprilTag_Create(String fam,
                                            double decimate, double blur, int threads, boolean debug,
                                            boolean refine_edges);

  // Destroy and free a previously created detector.
  public static native long AprilTag_Destroy(long detector);

  private static native Object[] AprilTag_Detect(long detector, long imgAddr, int rows, int cols);

  // Detect targets given a GRAY frame. Returns a pointer toa zarray
  public static DetectionResult[] AprilTag_Detect(long detector, Mat img) {
    return (DetectionResult[]) AprilTag_Detect(detector, img.dataAddr(), img.rows(), img.cols());
  }

  public static void main(String[] args) {
    System.loadLibrary("apriltag");

    long detector = AprilTag_Create("tag36h11", 2, 2, 1, false, true);

    // var buff = ByteBuffer.allocateDirect(1280 * 720);

    // // try {
    // //     CameraServerCvJNI.forceLoad();
    // // } catch (IOException e) {
    // //     // TODO Auto-generated catch block
    // //     e.printStackTrace();
    // // }
    // // PicamJNI.forceLoad();
    // // TestUtils.loadLibraries();
    // var img = Imgcodecs.imread("~/Downloads/TagFams.jpg");

    var ret = AprilTag_Detect(detector, 0, 720, 1280);
    // System.out.println(detector);
    // System.out.println(ret);
    System.out.println(List.of(ret));
  }
}