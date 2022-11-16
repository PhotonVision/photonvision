/*
Copyright (c) 2022 Photon Vision. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of FIRST, WPILib, nor the names of other WPILib
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY FIRST AND OTHER WPILIB CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY NONINFRINGEMENT AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FIRST OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.photonvision.vision.apriltag;

import edu.wpi.first.util.RuntimeDetector;
import edu.wpi.first.util.RuntimeLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.util.thread.ExecutorSizedThreadPool;
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
            } else if (RuntimeDetector.isAarch64()) {
                subfolder = "aarch64";
            } else if (RuntimeDetector.isRaspbian()) {
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
            String fam, double decimate, double blur, int threads, boolean debug, boolean refine_edges,
            int min_cluster_pixels, int maxErrorBits, int extraDecisionMargin);

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
}
