package org.photonvision.common.logging;

public enum Level {
    OFF(0, Logger.ANSI_BLACK),
    ERROR(1, Logger.ANSI_RED),
    WARN(2, Logger.ANSI_YELLOW),
    INFO(3, Logger.ANSI_GREEN),
    DEBUG(4, Logger.ANSI_WHITE),
    TRACE(5, Logger.ANSI_CYAN),
    DE_PEST(6, Logger.ANSI_WHITE);

    public final String colorCode;
    public final int code;

    Level(int code, String colorCode) {
        this.code = code;
        this.colorCode = colorCode;
    }
}
