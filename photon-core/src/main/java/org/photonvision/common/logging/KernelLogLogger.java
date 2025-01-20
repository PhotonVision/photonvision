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

package org.photonvision.common.logging;

import org.photonvision.common.hardware.Platform;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.jni.QueuedFileLogger;

/**
 * Listens for and reproduces Linux kernel logs, from /var/log/kern.log, into the Photon logger
 * ecosystem
 */
public class KernelLogLogger {
    private static KernelLogLogger INSTANCE;

    public static KernelLogLogger getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KernelLogLogger();
        }
        return INSTANCE;
    }

    QueuedFileLogger listener = null;
    Logger logger = new Logger(KernelLogLogger.class, LogGroup.General);

    public KernelLogLogger() {
        if (Platform.isLinux()) {
            listener = new QueuedFileLogger("/var/log/kern.log");
        } else {
            System.out.println("NOT for klogs");
        }

        // arbitrary frequency to grab logs. The underlying native buffer will grow unbounded without
        // this, lol
        TimedTaskManager.getInstance().addTask("outputPrintk", this::outputNewPrintks, 1000);
    }

    public void outputNewPrintks() {
        if (listener == null) {
            return;
        }

        for (var msg : listener.getNewlines()) {
            // We currently set all logs to debug regardless of their actual level
            logger.log(msg, LogLevel.DEBUG);
        }
    }
}
