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

import java.lang.foreign.MemorySegment;

/**
 * Compatibility wrapper for MemorySegment to handle API differences between Java versions.
 *
 * <p>Java 24+ uses java.lang.foreign.MemorySegment (finalized API). On non-macOS platforms, this
 * class is stubbed since foreign memory access is only needed for Swift interop.
 */
public class MemorySegmentCompat {
    private MemorySegmentCompat() {}

    /**
     * Create a MemorySegment from a native address.
     *
     * @param address The native memory address
     * @return A MemorySegment wrapping the given address
     */
    public static MemorySegment ofAddress(long address) {
        return MemorySegment.ofAddress(address);
    }
}
