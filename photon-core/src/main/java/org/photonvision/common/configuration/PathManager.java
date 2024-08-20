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

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PathManager {
    private static final Logger logger = new Logger(PathManager.class, LogGroup.Config);

    private static PathManager INSTANCE;

    final Path configDirectoryPath;
    final File configDirectoryFile;

    /**
     * Change the root config folder. Must happen before ConfigManager's singleton loads.
     * @param rootFolder
     */
    public static void setRootFolder(Path rootFolder) {
        INSTANCE = new PathManager(rootFolder);
    }

    public static PathManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PathManager();
        }
        return INSTANCE;
    }

    private PathManager() {
        this(Path.of("photonvision_config"));
    }

    private PathManager(Path configDirPath) {
        this.configDirectoryPath = configDirPath;
        this.configDirectoryFile = new File(getRootFolder().toUri());
        logger.info("Started with root config directory " + configDirPath.toAbsolutePath().toString());
    }

    public Path getRootFolder() {
        return configDirectoryPath;
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
