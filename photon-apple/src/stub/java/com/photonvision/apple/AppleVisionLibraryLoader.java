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
 * Stub implementation of AppleVisionLibraryLoader for non-macOS platforms.
 * All initialization is a no-op since Apple Vision is not available.
 */
public class AppleVisionLibraryLoader {
    private static boolean initialized = false;

    /**
     * No-op initialization on non-macOS platforms.
     * Does not throw - silently succeeds.
     */
    public static synchronized void initialize() {
        // No-op on non-macOS
        initialized = true;
    }

    /**
     * Always returns true after first call to initialize().
     * This prevents error logs from attempting to initialize multiple times.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
