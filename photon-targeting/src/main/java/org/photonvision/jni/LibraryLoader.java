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

import edu.wpi.first.apriltag.jni.AprilTagJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.cscore.OpenCvLoader;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.jni.WPIMathJNI;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.WPIUtilJNI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opencv.core.Core;

public class LibraryLoader {
    private static boolean hasWpiLoaded = false;
    private static boolean hasTargetingLoaded = false;
    private static boolean hasNvidiaAprilTagLoaded = false;

    public static boolean loadWpiLibraries() {
        if (hasWpiLoaded) return true;

        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        OpenCvLoader.Helper.setExtractOnStaticLoad(false);
        JNIWrapper.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        AprilTagJNI.Helper.setExtractOnStaticLoad(false);
        try {
            // Need to load wpiutil first before checking if the MSVC runtime is valid
            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class, "wpiutiljni");
            WPIUtilJNI.checkMsvcRuntime();
            CombinedRuntimeLoader.loadLibraries(
                    LibraryLoader.class,
                    "wpimathjni",
                    "ntcorejni",
                    "wpinetjni",
                    "wpiHaljni",
                    "cscorejni",
                    "apriltagjni");

            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class, Core.NATIVE_LIBRARY_NAME);
            hasWpiLoaded = true;
        } catch (IOException e) {
            e.printStackTrace();
            hasWpiLoaded = false;
        }

        return hasWpiLoaded;
    }

    public static boolean loadTargeting() {
        if (hasTargetingLoaded) return true;
        try {
            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class, "photontargetingJNI");
            hasTargetingLoaded = true;
        } catch (IOException e) {
            hasTargetingLoaded = loadLocalBuildLibrary("photontargetingJNI");
            if (!hasTargetingLoaded) {
                e.printStackTrace();
            }
        }
        return hasTargetingLoaded;
    }

    public static boolean loadNvidiaAprilTag() throws IOException {
        if (hasNvidiaAprilTagLoaded) {
            return true;
        }

        try {
            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class, "nvidiaapriltagJNI");
            hasNvidiaAprilTagLoaded = true;
            return true;
        } catch (IOException e) {
            hasNvidiaAprilTagLoaded = loadLocalBuildLibrary("nvidiaapriltagJNI");
            if (!hasNvidiaAprilTagLoaded) {
                throw e;
            }
            return true;
        }
    }

    private static boolean loadLocalBuildLibrary(String libraryName) {
        for (var libraryPath : getLocalBuildCandidates(libraryName)) {
            if (!Files.exists(libraryPath)) {
                continue;
            }

            try {
                if ("photontargetingJNI".equals(libraryName)) {
                    var configuration = libraryPath.getParent().getFileName();
                    var platform = libraryPath.getParent().getParent().getFileName();
                    var jniRoot =
                            libraryPath.getParent().getParent().getParent().getParent();
                    var supportingLibrary =
                            jniRoot.resolveSibling("photontargeting")
                                    .resolve("shared")
                                    .resolve(platform)
                                    .resolve(configuration)
                                    .resolve(System.mapLibraryName("photontargeting"));
                    if (Files.exists(supportingLibrary)) {
                        System.load(supportingLibrary.toAbsolutePath().toString());
                    }
                }

                System.load(libraryPath.toAbsolutePath().toString());
                return true;
            } catch (UnsatisfiedLinkError ignored) {
                // Fall through to the next candidate.
            }
        }

        return false;
    }

    private static Path[] getLocalBuildCandidates(String libraryName) {
        var platform = CombinedRuntimeLoader.getPlatformPath().replace("/", "");
        var fileName = System.mapLibraryName(libraryName);
        return new Path[] {
            Path.of("photon-targeting", "build", "libs", libraryName, "shared", platform, "release", fileName),
            Path.of("photon-targeting", "build", "libs", libraryName, "shared", platform, "debug", fileName),
            Path.of("build", "libs", libraryName, "shared", platform, "release", fileName),
            Path.of("build", "libs", libraryName, "shared", platform, "debug", fileName),
            Path.of("..", "photon-targeting", "build", "libs", libraryName, "shared", platform, "release", fileName),
            Path.of("..", "photon-targeting", "build", "libs", libraryName, "shared", platform, "debug", fileName),
            Path.of("..", "build", "libs", libraryName, "shared", platform, "release", fileName),
            Path.of("..", "build", "libs", libraryName, "shared", platform, "debug", fileName),
            Path.of("..", "..", "photon-targeting", "build", "libs", libraryName, "shared", platform, "release", fileName),
            Path.of("..", "..", "photon-targeting", "build", "libs", libraryName, "shared", platform, "debug", fileName),
        };
    }
}
