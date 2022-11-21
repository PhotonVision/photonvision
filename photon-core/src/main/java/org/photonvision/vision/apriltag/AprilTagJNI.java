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

import edu.wpi.first.util.RuntimeDetector;
import edu.wpi.first.util.RuntimeLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class AprilTagJNI {
    static final boolean USE_DEBUG =
            false; // Development flag - should be false on release, but flip to True to read in a debug
    // version of the library
    static final String NATIVE_DEBUG_LIBRARY_NAME = "apriltagd";
    static final String NATIVE_RELEASE_LIBRARY_NAME = "apriltag";

    static boolean s_libraryLoaded = false;
    static RuntimeLoader<AprilTagJNI> s_loader = null;
    private static Logger logger = new Logger(AprilTagJNI.class, LogGroup.VisionModule);

    public static synchronized void forceLoad() throws IOException {
        if (s_libraryLoaded) return;

        try {
            // Ensure the lib directory has been created to receive the unpacked shared object
            File libDirectory = Path.of("lib/").toFile();
            if (!libDirectory.exists()) {
                Files.createDirectory(libDirectory.toPath()).toFile();
            }

            // Pick the proper library based on development flags
            String libBaseName = USE_DEBUG ? NATIVE_DEBUG_LIBRARY_NAME : NATIVE_RELEASE_LIBRARY_NAME;
            String libFileName = System.mapLibraryName(libBaseName);
            File libFile = Path.of("lib/" + libFileName).toFile();

            // Always extract the library fresh
            // Yes, technically, a hashing strategy should speed this up, but it's only a
            // one-time, at-startup time hit. And not very big.
            URL resourceURL;

            String subfolder;
            // TODO 64-bit Pi support
            if (RuntimeDetector.isAthena()) {
                subfolder = "athena";
            } else if (RuntimeDetector.isArm64()) {
                subfolder = "aarch64";
            } else if (AprilTagJNI.isRaspbian()) {
                subfolder = "raspbian";
            } else if (RuntimeDetector.isWindows()) {
                subfolder = "win64";
            } else if (RuntimeDetector.isLinux()) {
                subfolder = "linux64";
            } else if (RuntimeDetector.isMac()) {
                subfolder = "mac";
            } // NOT m1, afaict, lol
            else {
                logger.error("Could not determine platform! Cannot load Apriltag JNI");
                return;
            }

            resourceURL =
                    AprilTagJNI.class.getResource(
                            "/nativelibraries/apriltag/" + subfolder + "/" + libFileName);

            try (InputStream in = resourceURL.openStream()) {
                // Remove the file if it already exists
                if (libFile.exists()) Files.delete(libFile.toPath());
                // Copy in a fresh resource
                Files.copy(in, libFile.toPath());
            }

            // Actually load the library
            System.load(libFile.getAbsolutePath());

            s_libraryLoaded = true;

        } catch (UnsatisfiedLinkError e) {
            logger.error("Couldn't load apriltag shared object");
            e.printStackTrace();
        } catch (IOException ioe) {
            logger.error("IO exception copying apriltag shared object");
            ioe.printStackTrace();
        }

        if (!s_libraryLoaded) {
            logger.error("Failed to load AprilTag Native Library!");
        } else {
            logger.info("AprilTag Native Library loaded successfully");
        }
    }

    // Returns a pointer to a apriltag_detector_t
    public static native long AprilTag_Create(
            String fam, double decimate, double blur, int threads, boolean debug, boolean refine_edges);

    // Destroy and free a previously created detector.
    public static native long AprilTag_Destroy(long detector);

    private static native Object[] AprilTag_Detect(
            long detector,
            long imgAddr,
            int rows,
            int cols,
            boolean doPoseEstimation,
            double tagWidth,
            double fx,
            double fy,
            double cx,
            double cy,
            int nIters);

    // Detect targets given a GRAY frame. Returns a pointer toa zarray
    public static DetectionResult[] AprilTag_Detect(
            long detector,
            Mat img,
            boolean doPoseEstimation,
            double tagWidth,
            double fx,
            double fy,
            double cx,
            double cy,
            int nIters) {
        return (DetectionResult[])
                AprilTag_Detect(
                        detector,
                        img.dataAddr(),
                        img.rows(),
                        img.cols(),
                        doPoseEstimation,
                        tagWidth,
                        fx,
                        fy,
                        cx,
                        cy,
                        nIters);
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

    private static boolean isRaspbian() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("/etc/os-release"))) {
          String value = reader.readLine();
          if (value == null) {
            return false;
          }
          return value.contains("Raspbian");
        } catch (IOException ex) {
          return false;
        }
      }
}
