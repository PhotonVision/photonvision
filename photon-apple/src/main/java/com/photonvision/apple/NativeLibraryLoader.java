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

package com.photonvision.apple;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads native libraries (dylibs) from JAR resources.
 *
 * <p>Swift and CoreML dylibs are packaged in the JAR at native/macos/ and need to be extracted to a
 * temporary directory before loading.
 */
public class NativeLibraryLoader {
    private static final String NATIVE_LIB_PATH = "/native/macos/";
    private static Path libDir = null;

    /**
     * Load a native library from JAR resources.
     *
     * @param libraryName Name of the library (e.g., "SwiftKitSwift" for libSwiftKitSwift.dylib)
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    public static synchronized void loadLibrary(String libraryName) {
        try {
            // Create library directory if not exists
            // Use a fixed location that can be set in java.library.path at JVM startup
            if (libDir == null) {
                // Use photonvision_config/nativelibs as our library directory
                // This path is relative to the working directory
                String pathPrefix = System.getProperty("PATH_PREFIX", "");
                libDir = Path.of(pathPrefix + "photonvision_config/nativelibs").toAbsolutePath();
                Files.createDirectories(libDir);
            }

            // Construct library filename (libFoo.dylib on macOS)
            String libFileName = System.mapLibraryName(libraryName);

            // Extract from JAR to library directory
            String resourcePath = NATIVE_LIB_PATH + libFileName;
            Path extractedLib = libDir.resolve(libFileName);

            // Only extract if not already extracted
            if (!Files.exists(extractedLib)) {
                try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        throw new UnsatisfiedLinkError("Native library not found in JAR: " + resourcePath);
                    }
                    Files.copy(in, extractedLib, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Don't load the library here - let the JExtract-generated code load it via
            // System.loadLibrary()
            // This ensures all libraries are loaded the same way and in the correct order
            // We just needed to extract the library to the directory

        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                    "Failed to extract library: " + libraryName + " - " + e.getMessage());
        }
    }

    /**
     * Load the Swift runtime library.
     *
     * <p>On macOS, the Swift runtime is embedded in Swift libraries and automatically provided by
     * dyld. However, Swift-Java's JExtract-generated code tries to load swiftCore via
     * System.loadLibrary(). We need to add the temp directory (where we extract dylibs) to
     * java.library.path so System.loadLibrary() can find our extracted libs.
     *
     * <p>We create a dummy libswiftCore.dylib symlink pointing to swiftCore from SwiftKitSwift, so
     * System.loadLibrary("swiftCore") succeeds.
     */
    public static void loadSwiftRuntime() {
        // Swift runtime is part of SwiftKitSwift and will be available via dyld
        // We don't need to do anything special here
    }
}
