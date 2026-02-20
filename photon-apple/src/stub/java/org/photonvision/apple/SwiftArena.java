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

package org.photonvision.apple;

/**
 * Stub implementation of SwiftArena for non-macOS platforms.
 *
 * <p>This class is never instantiated on non-macOS platforms since Apple object detection is not
 * supported.
 */
public class SwiftArena implements AutoCloseable {
    private SwiftArena() {}

    public static SwiftArena ofAuto() {
        throw new UnsupportedOperationException("SwiftArena is only supported on macOS");
    }

    public static SwiftArena ofConfined() {
        throw new UnsupportedOperationException("SwiftArena is only supported on macOS");
    }

    public Object unwrap() {
        throw new UnsupportedOperationException("SwiftArena is only supported on macOS");
    }

    @Override
    public void close() {
        // No-op stub
    }
}
