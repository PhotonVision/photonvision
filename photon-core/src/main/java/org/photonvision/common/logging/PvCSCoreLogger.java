package org.photonvision.common.logging;

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
        CameraServerJNI.setLogger(this::logMsg, 0);
        this.logger = new Logger(getClass(), LogGroup.CSCore);
    }

    private void logMsg(int level, String file, int line, String msg) {
        if (level == 20) {
            logger.error(msg);
            return;
        }

        String levelmsg;
        if (level >= 50) {
            levelmsg = "CRITICAL";
        } else if (level >= 40) {
            levelmsg = "ERROR";
        } else if (level >= 30) {
            levelmsg = "WARNING";
        } else {
            return;
        }
        logger.error("CS: " + levelmsg + ": " + msg + " (" + file + ":" + line + ")\n");
    }
}
