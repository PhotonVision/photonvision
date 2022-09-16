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

package org.photonvision.vision.apriltag;

import edu.wpi.first.util.RuntimeLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class AprilTagJNI {
    static final String NATIVE_LIBRARY_NAME = "apriltagd";
    static boolean s_libraryLoaded = false;
    static RuntimeLoader<AprilTagJNI> s_loader = null;
    private static Logger logger = new Logger(AprilTagJNI.class, LogGroup.Camera);

    public static class Helper {
        private static AtomicBoolean extractOnStaticLoad = new AtomicBoolean(true);

        public static boolean getExtractOnStaticLoad() {
            return extractOnStaticLoad.get();
        }

        public static void setExtractOnStaticLoad(boolean load) {
            extractOnStaticLoad.set(load);
        }
    }

    // static {
    //   if (Helper.getExtractOnStaticLoad()) {
    //     try {
    //       forceLoad();
    //     } catch (IOException ex) {
    //       ex.printStackTrace();
    //       System.exit(1);
    //     }
    //   }
    // }

    public static synchronized void forceLoad() throws IOException {
        // if (s_libraryLoaded) {
        //   return;
        // }

        // s_loader = new RuntimeLoader<>(
        //   NATIVE_LIBRARY_NAME,
        //   NativeLibHelper.getInstance().NativeLibPath.toString(),
        //   AprilTagJNI.class
        // );

        // s_loader.loadLibrary();
        // s_libraryLoaded = true;

        if (s_libraryLoaded) return;

        try {
            String libFileName = System.mapLibraryName("apriltag");
            File libDirectory = Path.of("lib/").toFile();
            if (!libDirectory.exists()) {
                Files.createDirectory(libDirectory.toPath()).toFile();
            }

            // We always extract the shared object (we could hash each so, but that's a lot of work)
            URL resourceURL;
            if (Platform.isRaspberryPi()) {
                resourceURL =
                        AprilTagJNI.class.getResource("/nativelibraries/apriltag/raspi/" + libFileName);
            } else {
                resourceURL = AprilTagJNI.class.getResource("/nativelibraries/apriltag/" + libFileName);
            }
            File libFile = Path.of("lib/" + libFileName).toFile();
            try (InputStream in = resourceURL.openStream()) {
                if (libFile.exists()) Files.delete(libFile.toPath());
                Files.copy(in, libFile.toPath());
            }
            System.load(libFile.getAbsolutePath());

            s_libraryLoaded = true;
            // logger.info("Successfully loaded libpicam shared object");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Couldn't load apriltag shared object");
            e.printStackTrace();
        } catch (IOException ioe) {
            logger.error("IO exception copying apriltag shared object");
            ioe.printStackTrace();
        }

        logger.warn("Tried to load apriltags, success: " + s_libraryLoaded);
    }

    // Returns a pointer to a apriltag_detector_t
    public static native long AprilTag_Create(
            String fam, double decimate, double blur, int threads, boolean debug, boolean refine_edges);

    // Destroy and free a previously created detector.
    public static native long AprilTag_Destroy(long detector);

    private static native Object[] AprilTag_Detect(long detector, long imgAddr, int rows, int cols, boolean doPoseEstimation,
        double tagWidth, double fx, double fy, double cx, double cy, int nIters);

    // Detect targets given a GRAY frame. Returns a pointer toa zarray
    public static DetectionResult[] AprilTag_Detect(long detector, Mat img, boolean doPoseEstimation,
            double tagWidth, double fx, double fy, double cx, double cy, int nIters) {
        return (DetectionResult[]) AprilTag_Detect(detector, img.dataAddr(), img.rows(), img.cols(), doPoseEstimation,
        tagWidth, fx, fy, cx, cy, nIters);
    }

    public static void main(String[] args) {
        // System.loadLibrary("apriltag");

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

        // var ret = AprilTag_Detect(detector, 0, 720, 1280);
        // System.out.println(detector);
        // System.out.println(ret);
        // System.out.println(List.of(ret));
    }
}
