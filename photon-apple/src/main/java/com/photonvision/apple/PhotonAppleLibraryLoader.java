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

/**
 * Initializes the Apple Vision library by loading required native libraries.
 *
 * <p>IMPORTANT: This MUST be called before using any AppleObjectDetector or Swift-generated
 * classes. The static initializer in the JExtract-generated code will try to load libraries, so we
 * extract them to java.library.path first.
 */
public class PhotonAppleLibraryLoader {
    private static boolean initialized = false;

    /**
     * Initialize and load all required native libraries.
     *
     * <p>This extracts dylibs from the JAR to photonvision_config/nativelibs/ and loads them
     * explicitly using System.load() with full paths. The libraries are loaded in the correct order:
     *
     * <ol>
     *   <li>swiftCore - System library (dyld loads automatically from shared cache, no explicit
     *       loading needed)
     *   <li>SwiftJava - From swift-java, packaged in JAR, extracted and loaded
     *   <li>SwiftRuntimeFunctions - From swift-java, packaged in JAR, extracted and loaded
     *   <li>AppleVisionLibrary - Our Swift code, packaged in JAR, extracted and loaded
     * </ol>
     *
     * <p>IMPORTANT: This must be called before any JExtract-generated Swift classes are loaded,
     * because their static initializers will try to load these libraries. Loading them here first
     * ensures the static initializers succeed.
     *
     * <p>On macOS 12.3+, swiftCore and other system Swift frameworks are part of the dyld shared
     * cache and don't need explicit loading - they're automatically resolved when our Swift libraries
     * are loaded.
     *
     * @throws UnsatisfiedLinkError if any library fails to load
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            // 1. Extract libraries from JAR to photonvision_config/nativelibs/
            NativeLibraryLoader.extractLibrary("SwiftJava");
            NativeLibraryLoader.extractLibrary("SwiftRuntimeFunctions");
            NativeLibraryLoader.extractLibrary("AppleVisionLibrary");

            // 2. Load Swift runtime (attempts to preload swiftCore from dyld cache using FFM)
            NativeLibraryLoader.loadSwiftRuntime();

            // 3. Preload all libraries in the correct order using System.load() with full paths
            // This ensures they're loaded BEFORE the ObjectDetector static initializer runs
            // Once loaded, the ObjectDetector's System.loadLibrary() calls will succeed
            // because the library is already in memory
            NativeLibraryLoader.loadLibrary("SwiftJava");
            NativeLibraryLoader.loadLibrary("SwiftRuntimeFunctions");
            NativeLibraryLoader.loadLibrary("AppleVisionLibrary");

            initialized = true;
        } catch (UnsatisfiedLinkError e) {
            throw new UnsatisfiedLinkError(
                    "Failed to initialize Apple Vision libraries. "
                            + "Make sure you're running on macOS with Swift runtime available. "
                            + "Original error: "
                            + e.getMessage());
        }
    }

    /** Check if the library has been initialized. */
    public static boolean isInitialized() {
        return initialized;
    }
}
