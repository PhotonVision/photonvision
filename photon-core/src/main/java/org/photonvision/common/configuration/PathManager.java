package org.photonvision.common.configuration;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class PathManager {
    private static PathManager INSTANCE;

    final File configDirectoryFile;

    public static PathManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PathManager();
        }
        return INSTANCE;
    }

    private PathManager() {
        this.configDirectoryFile = new File(getRootFolder().toUri());
    }
    
    public Path getRootFolder() {
        return Path.of("photonvision_config");
    }
    
    public Path getLogsDir() {
        return Path.of(configDirectoryFile.toString(), "logs");
    }    

    public static final String LOG_PREFIX = "photonvision-";
    public static final String LOG_EXT = ".log";
    public static final String LOG_DATE_TIME_FORMAT = "yyyy-M-d_hh-mm-ss";

    public String taToLogFname(TemporalAccessor date) {
        var dateString = DateTimeFormatter.ofPattern(LOG_DATE_TIME_FORMAT).format(date);
        return LOG_PREFIX + dateString + LOG_EXT;
    }

    public Path getLogPath() {
        var logFile = Path.of(this.getLogsDir().toString(), taToLogFname(LocalDateTime.now())).toFile();
        if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
        return logFile.toPath();
    }

    public Date logFnameToDate(String fname) throws ParseException {
        // Strip away known unneeded portions of the log file name
        fname = fname.replace(LOG_PREFIX, "").replace(LOG_EXT, "");
        DateFormat format = new SimpleDateFormat(LOG_DATE_TIME_FORMAT);
        return format.parse(fname);
    }
}
