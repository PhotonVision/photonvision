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

package org.photonvision.common.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class NativeObjectReleaser implements Runnable {

    private static final Logger logger = new Logger(NativeObjectReleaser.class, LogGroup.General);

    private static String formatPtr(long ptr) {
        return "0x" + String.format("%016x", ptr);
    }

    private final long ptr;
    private final LongConsumer destructor;
    private final String nativeObjectName;

    /** Atomic boolean to ensure that the native object can only be released once. */
    private AtomicBoolean released = new AtomicBoolean(false);

    public NativeObjectReleaser(long ptr, LongConsumer destructor, String nativeObjectName) {
        this.nativeObjectName = nativeObjectName;
        this.ptr = ptr;
        this.destructor = destructor;
    }

    public NativeObjectReleaser(long ptr, LongConsumer destructor) {
        this(ptr, destructor, "(void *) " + formatPtr(ptr));
    }

    @Override
    public void run() {
        if (released.compareAndSet(false, true)) {
            if (ptr <= 0) {
                logger.error("Error releasing native object " + nativeObjectName + ": negative pointer");
                return;
            }

            destructor.accept(ptr);
            logger.debug("Released native object " + nativeObjectName);
        }
    }
}

