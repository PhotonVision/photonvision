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
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads native libraries (dylibs) from JAR resources.
 *
 * <p>Swift and CoreML dylibs are packaged in the JAR at native/macos/ and need to be extracted to a
 * directory in java.library.path before loading.
 */
public class NativeLibraryLoader {
    private static final String NATIVE_LIB_PATH = "/native/macos/";
    private static Path libDir = null;
    private static boolean addedToLibraryPath = false;

    /**
     * Add the library extraction directory to java.library.path.
     *
     * <p>This uses reflection to modify the system property and ClassLoader's usr_paths field, making
     * extracted libraries discoverable by System.loadLibrary() calls.
     */
    public static synchronized void addToJavaLibraryPath() {
        if (addedToLibraryPath) {
            return;
        }

        try {
            // Create library directory if not exists
            if (libDir == null) {
                String pathPrefix = System.getProperty("PATH_PREFIX", "");
                libDir = Path.of(pathPrefix + "photonvision_config/nativelibs").toAbsolutePath();
                Files.createDirectories(libDir);
            }

            // Add to java.library.path system property
            // Include both our extracted libs directory and /usr/lib/swift for system Swift libraries
            String currentPath = System.getProperty("java.library.path", "");
            String pathSeparator = System.getProperty("path.separator");
            String newPath = libDir.toString() + pathSeparator + "/usr/lib/swift";
            if (!currentPath.isEmpty()) {
                newPath = newPath + pathSeparator + currentPath;
            }
            System.setProperty("java.library.path", newPath);

            // Reset ClassLoader's cache of java.library.path using reflection
            // This is necessary because ClassLoader caches the path at startup
            try {
                java.lang.reflect.Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
                sysPathsField.setAccessible(true);
                sysPathsField.set(null, null);
            } catch (Exception e) {
                // If reflection fails, it's not critical - the System.setProperty() above should work
                // for libraries loaded after this point
            }

            addedToLibraryPath = true;
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("Failed to create library directory: " + e.getMessage());
        }
    }

    /**
     * Extract a native library from JAR resources to the library directory.
     *
     * <p>This extracts the library without loading it. After extraction, the library can be loaded
     * using System.loadLibrary() if the directory is in java.library.path.
     *
     * @param libraryName Name of the library (e.g., "SwiftJava" for libSwiftJava.dylib)
     * @throws UnsatisfiedLinkError if the library cannot be extracted
     */
    public static synchronized void extractLibrary(String libraryName) {
        try {
            // Create library directory if not exists
            if (libDir == null) {
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
        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                    "Failed to extract library: " + libraryName + " - " + e.getMessage());
        }
    }

    /**
     * Load a library using System.load() with the full path.
     *
     * @param libraryName Name of the library (e.g., "SwiftJava")
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    public static synchronized void loadLibrary(String libraryName) {
        try {
            if (libDir == null) {
                String pathPrefix = System.getProperty("PATH_PREFIX", "");
                libDir = Path.of(pathPrefix + "photonvision_config/nativelibs").toAbsolutePath();
            }

            String libFileName = System.mapLibraryName(libraryName);
            Path libPath = libDir.resolve(libFileName);

            if (!Files.exists(libPath)) {
                throw new UnsatisfiedLinkError("Library not found: " + libPath);
            }

            System.load(libPath.toString());
        } catch (Exception e) {
            throw new UnsatisfiedLinkError("Failed to load library: " + libraryName + " - " + e.getMessage());
        }
    }

    /**
     * Load the Swift runtime library.
     *
     * <p>On macOS 12.3+, swiftCore is in the dyld shared cache. We use FFM's SymbolLookup to load it
     * via dlopen, which can find libraries in the shared cache.
     */
    public static void loadSwiftRuntime() {
        try {
            // Use FFM SymbolLookup.libraryLookup() to load swiftCore via dlopen()
            // This will find it in the dyld shared cache even though no file exists
            SymbolLookup.libraryLookup("swiftCore", java.lang.foreign.Arena.global());
        } catch (IllegalArgumentException e) {
            // If SymbolLookup fails, try System.loadLibrary as fallback
            try {
                System.loadLibrary("swiftCore");
            } catch (UnsatisfiedLinkError e2) {
                // If both fail, dyld will automatically load swiftCore from the
                // shared cache when we load our Swift libraries that depend on it
            }
        }
    }
}
