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
 * classes. The static initializer will fail if libraries are not in java.library.path, so we
 * extract and load them first.
 */
public class AppleVisionLibraryLoader {
    private static boolean initialized = false;

    /**
     * Initialize and load all required native libraries.
     *
     * <p>This extracts dylibs from the JAR and loads them in the correct order: 1. swiftCore (system
     * library, auto-loaded by dyld) 2. SwiftKitSwift (from swift-java) 3. AppleVisionLibrary (our
     * Swift code)
     *
     * <p>Note: On macOS 12.3+, swiftCore is part of the OS and doesn't need explicit loading. The
     * JExtract-generated code will try to load it via System.loadLibrary(), which may fail, but the
     * dyld will provide it automatically when SwiftKitSwift is loaded.
     *
     * @throws UnsatisfiedLinkError if any library fails to load
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            // 1. Swift runtime is part of macOS and doesn't need explicit loading
            NativeLibraryLoader.loadSwiftRuntime();

            // 2. Load SwiftKitSwift (from swift-java, packaged in JAR)
            // This also sets up java.library.path and creates a swiftCore symlink
            NativeLibraryLoader.loadLibrary("SwiftKitSwift");

            // 3. Load our AppleVisionLibrary (packaged in JAR)
            NativeLibraryLoader.loadLibrary("AppleVisionLibrary");

            initialized = true;
        } catch (UnsatisfiedLinkError e) {
            throw new UnsatisfiedLinkError(
                    "Failed to initialize Apple Vision libraries. "
                            + "Make sure you're running on macOS with Swift runtime available: "
                            + e.getMessage());
        }
    }

    /** Check if the library has been initialized. */
    public static boolean isInitialized() {
        return initialized;
    }
}
