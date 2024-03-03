package org.photonvision.common.logging;

import java.io.File;
import java.nio.file.Path;

import edu.wpi.first.cscore.CameraServerJNI;

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
        logger.log("CS: " + levelmsg + " " + level + ": " + msg + " (" + file + ":" + line + ")", pvlevel);
    }
}
