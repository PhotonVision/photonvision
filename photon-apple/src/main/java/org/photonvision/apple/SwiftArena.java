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

import org.swift.swiftkit.ffm.AllocatingSwiftArena;

/**
 * Wrapper around SwiftKit's AllocatingSwiftArena to avoid direct SwiftKit imports in photon-core.
 *
 * <p>This allows photon-core to use Swift arenas without depending on SwiftKit directly. On
 * non-macOS platforms, this class is stubbed and throws UnsupportedOperationException.
 */
public class SwiftArena implements AutoCloseable {
    private final AllocatingSwiftArena arena;

    private SwiftArena(AllocatingSwiftArena arena) {
        this.arena = arena;
    }

    /**
     * Create an automatic arena that is cleaned up by the garbage collector.
     *
     * <p>Use this for long-lived arenas like detector-scoped allocations.
     */
    public static SwiftArena ofAuto() {
        return new SwiftArena(AllocatingSwiftArena.ofAuto());
    }

    /**
     * Create a confined arena that must be explicitly closed.
     *
     * <p>Use this for short-lived arenas like frame-scoped allocations in try-with-resources blocks.
     */
    public static SwiftArena ofConfined() {
        return new SwiftArena(AllocatingSwiftArena.ofConfined());
    }

    /**
     * Get the underlying AllocatingSwiftArena for use with Swift bindings.
     *
     * <p>This is used internally when passing the arena to Swift-generated code.
     */
    public AllocatingSwiftArena unwrap() {
        return arena;
    }

    /**
     * Close this arena (only for confined arenas).
     *
     * <p>For ofAuto() arenas, this is a no-op. For ofConfined() arenas, this would close the arena if
     * AllocatingSwiftArena supported it. Currently, confined arenas are also automatically cleaned
     * up.
     */
    @Override
    public void close() {
        // AllocatingSwiftArena doesn't have a close method
        // Both ofAuto() and ofConfined() are automatically cleaned up
    }
}
