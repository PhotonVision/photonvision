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

package org.photonvision.jni;

public class QueuedFileLogger {
    long m_handle = 0;

    public QueuedFileLogger(String path) {
        m_handle = QueuedFileLogger.create(path);
    }

    public String[] getNewlines() {
        String newBuffer = null;

        synchronized (this) {
            if (m_handle == 0) {
                System.err.println("QueuedFileLogger use after free");
                return new String[0];
            }

            newBuffer = QueuedFileLogger.getNewLines(m_handle);
        }

        if (newBuffer == null) {
            return new String[0];
        }

        return newBuffer.split("\n");
    }

    public void stop() {
        synchronized (this) {
            if (m_handle != 0) {
                QueuedFileLogger.destroy(m_handle);
                m_handle = 0;
            }
        }
    }

    private static native long create(String path);

    private static native void destroy(long handle);

    private static native String getNewLines(long handle);
}
