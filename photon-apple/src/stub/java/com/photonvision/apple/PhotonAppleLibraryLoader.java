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
 * Stub implementation of PhotonAppleLibraryLoader for non-macOS platforms.
 *
 * <p>This class provides a no-op implementation on platforms where the Apple Vision framework is
 * not available.
 */
public class PhotonAppleLibraryLoader {
    private static boolean initialized = false;

    /**
     * Stub initialize method - does nothing on non-macOS platforms.
     *
     * <p>This is a no-op on non-macOS platforms since the Apple Vision framework is only available
     * on macOS.
     */
    public static synchronized void initialize() {
        // No-op on non-macOS platforms
        initialized = true;
    }

    /** Check if the library has been initialized. */
    public static boolean isInitialized() {
        return initialized;
    }
}
