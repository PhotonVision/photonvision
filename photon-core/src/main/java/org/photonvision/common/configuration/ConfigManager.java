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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.FileUtils;
import org.photonvision.vision.processes.VisionSource;
import org.zeroturnaround.zip.ZipUtil;

public class ConfigManager {
    private static final Logger logger = new Logger(ConfigManager.class, LogGroup.General);
    private static ConfigManager INSTANCE;

    public static final String HW_CFG_FNAME = "hardwareConfig.json";
    public static final String HW_SET_FNAME = "hardwareSettings.json";
    public static final String NET_SET_FNAME = "networkSettings.json";

    final File configDirectoryFile;

    private final ConfigProvider m_provider;

    private Thread settingsSaveThread;
    private long saveRequestTimestamp = -1;

    enum ConfigSaveStrategy {
        SQL,
        LEGACY,
        ATOMIC_ZIP;
    }

    // This logic decides which kind of ConfigManager we load as the default. If we want
    // to switch back to the legacy config manager, change this constant
    private static final ConfigSaveStrategy m_saveStrat = ConfigSaveStrategy.SQL;

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            switch (m_saveStrat) {
                case SQL:
                    INSTANCE = new ConfigManager(getRootFolder(), new SqlConfigProvider(getRootFolder()));
                    break;
                case LEGACY:
                    INSTANCE = new ConfigManager(getRootFolder(), new LegacyConfigProvider(getRootFolder()));
                    break;
                case ATOMIC_ZIP:
                    // not yet done, fall through
                default:
                    break;
            }
        }
        return INSTANCE;
    }

    private void translateLegacyIfPresent(Path folderPath) {
        if (!(m_provider instanceof SqlConfigProvider)) {
            // Cannot import into SQL if we aren't in SQL mode rn
            return;
        }
        logger.info("Translating settings zip!");

        var maybeCams = Path.of(folderPath.toAbsolutePath().toString(), "cameras").toFile();
        var maybeCamsBak = Path.of(folderPath.toAbsolutePath().toString(), "cameras_backup").toFile();

        if (maybeCams.exists() && maybeCams.isDirectory()) {
            var legacy = new LegacyConfigProvider(folderPath);
            legacy.load();
            var loadedConfig = legacy.getConfig();

            // yeet our current cameras directory, not needed anymore
            if (maybeCamsBak.exists()) FileUtils.deleteDirectory(maybeCamsBak.toPath());
            if (!maybeCams.canWrite()) {
                maybeCams.setWritable(true);
            }

            try {
                Files.move(maybeCams.toPath(), maybeCamsBak.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Exception moving cameras to cameras_bak!", e);

                // Try to just copy from cams to cams-bak instead of moving? Windows sometimes needs us to
                // do that
                try {
                    org.apache.commons.io.FileUtils.copyDirectory(maybeCams, maybeCamsBak);
                } catch (IOException e1) {
                    // So we can't move to cams_bak, and we can't copy and delete either? We just have to give
                    // up here on preserving the old folder
                    logger.error("Exception while backup-copying cameras to cameras_bak!", e);
                    e1.printStackTrace();
                }

                // So we can't save the old config, and we couldn't copy the folder
                // But we've loaded the config. So just try to delete the directory so we don't try to load
                // form it next time. That does mean we have no backup recourse, tho
                if (maybeCams.exists()) FileUtils.deleteDirectory(maybeCams.toPath());
            }

            // Save the same config out using SQL loader
            var sql = new SqlConfigProvider(getRootFolder());
            sql.setConfig(loadedConfig);
            sql.saveToDisk();
        }
    }

    public static void saveUploadedSettingsZip(File uploadPath) {
        // Unpack to /tmp/something/photonvision
        var folderPath = Path.of(System.getProperty("java.io.tmpdir"), "photonvision").toFile();
        folderPath.mkdirs();
        ZipUtil.unpack(uploadPath, folderPath);

        // Nuke the current settings directory
        FileUtils.deleteDirectory(getRootFolder());

        // If there's a cameras folder in the upload, we know we need to import from the
        // old style
        var maybeCams = Path.of(folderPath.getAbsolutePath(), "cameras").toFile();
        if (maybeCams.exists() && maybeCams.isDirectory()) {
            var legacy = new LegacyConfigProvider(folderPath.toPath());
            legacy.load();
            var loadedConfig = legacy.getConfig();

            var sql = new SqlConfigProvider(getRootFolder());
            sql.setConfig(loadedConfig);
            sql.saveToDisk();
        } else {
            // new structure -- just copy and save like we used to
            try {
                org.apache.commons.io.FileUtils.copyDirectory(folderPath, getRootFolder().toFile());
                logger.info("Copied settings successfully!");
            } catch (IOException e) {
                logger.error("Exception copying uploaded settings!", e);
            }
        }
    }

    public PhotonConfiguration getConfig() {
        return m_provider.getConfig();
    }

    private static Path getRootFolder() {
        return Path.of("photonvision_config");
    }

    ConfigManager(Path configDirectory, ConfigProvider provider) {
        this.configDirectoryFile = new File(configDirectory.toUri());
        m_provider = provider;

        settingsSaveThread = new Thread(this::saveAndWriteTask);
        settingsSaveThread.start();
    }

    public void load() {
        translateLegacyIfPresent(this.configDirectoryFile.toPath());
        m_provider.load();
    }

    public void addCameraConfigurations(List<VisionSource> sources) {
        getConfig().addCameraConfigs(sources);
        requestSave();
    }

    public void saveModule(CameraConfiguration config, String uniqueName) {
        getConfig().addCameraConfig(uniqueName, config);
        requestSave();
    }

    public File getSettingsFolderAsZip() {
        File out = Path.of(System.getProperty("java.io.tmpdir"), "photonvision-settings.zip").toFile();
        try {
            ZipUtil.pack(configDirectoryFile, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public void setNetworkSettings(NetworkConfig networkConfig) {
        getConfig().setNetworkConfig(networkConfig);
        requestSave();
    }

    public Path getLogsDir() {
        return Path.of(configDirectoryFile.toString(), "logs");
    }

    public Path getCalibDir() {
        return Path.of(configDirectoryFile.toString(), "calibImgs");
    }

    public static final String LOG_PREFIX = "photonvision-";
    public static final String LOG_EXT = ".log";
    public static final String LOG_DATE_TIME_FORMAT = "yyyy-M-d_hh-mm-ss";

    public String taToLogFname(TemporalAccessor date) {
        var dateString = DateTimeFormatter.ofPattern(LOG_DATE_TIME_FORMAT).format(date);
        return LOG_PREFIX + dateString + LOG_EXT;
    }

    public Date logFnameToDate(String fname) throws ParseException {
        // Strip away known unneded portions of the log file name
        fname = fname.replace(LOG_PREFIX, "").replace(LOG_EXT, "");
        DateFormat format = new SimpleDateFormat(LOG_DATE_TIME_FORMAT);
        return format.parse(fname);
    }

    public Path getLogPath() {
        var logFile = Path.of(this.getLogsDir().toString(), taToLogFname(LocalDateTime.now())).toFile();
        if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
        return logFile.toPath();
    }

    public Path getImageSavePath() {
        var imgFilePath = Path.of(configDirectoryFile.toString(), "imgSaves").toFile();
        if (!imgFilePath.exists()) imgFilePath.mkdirs();
        return imgFilePath.toPath();
    }

    public void saveUploadedHardwareConfig(Path uploadPath) {
        m_provider.saveUploadedHardwareConfig(uploadPath);
    }

    public void saveUploadedHardwareSettings(Path uploadPath) {
        m_provider.saveUploadedHardwareSettings(uploadPath);
    }

    public void saveUploadedNetworkConfig(Path uploadPath) {
        m_provider.saveUploadedNetworkConfig(uploadPath);
    }

    public void requestSave() {
        logger.trace("Requesting save...");
        saveRequestTimestamp = System.currentTimeMillis();
    }

    public void unloadCameraConfigs() {
        this.getConfig().getCameraConfigurations().clear();
    }

    public void saveToDisk() {
        m_provider.saveToDisk();
    }

    private void saveAndWriteTask() {
        // Only save if 1 second has past since the request was made
        while (!Thread.currentThread().isInterrupted()) {
            if (saveRequestTimestamp > 0 && (System.currentTimeMillis() - saveRequestTimestamp) > 1000L) {
                saveRequestTimestamp = -1;
                logger.debug("Saving to disk...");
                saveToDisk();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Exception waiting for settings semaphore", e);
            }
        }
    }
}
