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
public class AppleVisionLibraryLoader {
    private static boolean initialized = false;

    /**
     * Initialize and load all required native libraries.
     *
     * <p>This extracts dylibs from the JAR to photonvision_config/nativelibs/ and ensures they're
     * ready for loading. The libraries are loaded in the correct order:
     *
     * <ol>
     *   <li>swiftCore (system library, auto-loaded by dyld from /usr/lib/swift)
     *   <li>SwiftJava (from swift-java, packaged in JAR)
     *   <li>SwiftRuntimeFunctions (from swift-java, packaged in JAR)
     *   <li>AppleVisionLibrary (our Swift code, packaged in JAR)
     * </ol>
     *
     * <p>Note: On macOS 12.3+, swiftCore is part of the OS and available via dyld. The
     * JExtract-generated code will call System.loadLibrary("swiftCore"), which works because
     * /usr/lib/swift is in java.library.path.
     *
     * @throws UnsatisfiedLinkError if any library fails to load
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            // 1. Swift runtime is part of macOS and will be loaded automatically
            NativeLibraryLoader.loadSwiftRuntime();

            // 2. Extract SwiftJava (from swift-java, packaged in JAR)
            NativeLibraryLoader.loadLibrary("SwiftJava");

            // 3. Extract SwiftRuntimeFunctions (from swift-java, packaged in JAR)
            NativeLibraryLoader.loadLibrary("SwiftRuntimeFunctions");

            // 4. Extract our AppleVisionLibrary (packaged in JAR)
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
