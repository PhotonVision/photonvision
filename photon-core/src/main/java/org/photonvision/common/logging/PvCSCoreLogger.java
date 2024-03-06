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

import edu.wpi.first.cscore.CameraServerJNI;
import java.nio.file.Path;

/** Redirect cscore logs to our logger */
public class PvCSCoreLogger {
    private static PvCSCoreLogger INSTANCE;

    public static PvCSCoreLogger getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PvCSCoreLogger();
        }
        return INSTANCE;
    }

    private Logger logger;

    private PvCSCoreLogger() {
        CameraServerJNI.setLogger(this::logMsg, 7);
        this.logger = new Logger(getClass(), LogGroup.CSCore);
    }

    private void logMsg(int level, String file, int line, String msg) {
        if (level == 20) {
            logger.info(msg);
            return;
        }

        file = Path.of(file).getFileName().toString();

        String levelmsg;
        LogLevel pvlevel;
        if (level >= 50) {
            levelmsg = "CRITICAL";
            pvlevel = LogLevel.ERROR;
        } else if (level >= 40) {
            levelmsg = "ERROR";
            pvlevel = LogLevel.ERROR;
        } else if (level >= 30) {
            levelmsg = "WARNING";
            pvlevel = LogLevel.WARN;
        } else if (level >= 20) {
            levelmsg = "INFO";
            pvlevel = LogLevel.INFO;
        } else {
            levelmsg = "DEBUG";
            pvlevel = LogLevel.DEBUG;
        }
        logger.log(
                "CS: " + levelmsg + " " + level + ": " + msg + " (" + file + ":" + line + ")", pvlevel);
    }
}
