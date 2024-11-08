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
import java.util.Date;
import java.util.List;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.FileUtils;
import org.photonvision.vision.processes.VisionSource;
import org.zeroturnaround.zip.ZipUtil;

public class ConfigManager {
    private static ConfigManager INSTANCE;

    public static final String HW_CFG_FNAME = "hardwareConfig.json";
    public static final String HW_SET_FNAME = "hardwareSettings.json";
    public static final String NET_SET_FNAME = "networkSettings.json";

    final File configDirectoryFile;

    private final ConfigProvider m_provider;

    private final Thread settingsSaveThread;
    private long saveRequestTimestamp = -1;

    // special case flag to disable flushing settings to disk at shutdown. Avoids the jvm shutdown
    // hook overwriting the settings we just uploaded
    private boolean flushOnShutdown = true;
    private boolean allowWriteTask = true;

    enum ConfigSaveStrategy {
        SQL,
        LEGACY,
        ATOMIC_ZIP
    }

    // This logic decides which kind of ConfigManager we load as the default. If we want to switch
    // back to the legacy config manager, change this constant
    private static final ConfigSaveStrategy m_saveStrat = ConfigSaveStrategy.SQL;

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            Path rootFolder = PathManager.getInstance().getRootFolder();
            switch (m_saveStrat) {
                case SQL:
                    INSTANCE = new ConfigManager(rootFolder, new SqlConfigProvider(rootFolder));
                    break;
                case LEGACY:
                    INSTANCE = new ConfigManager(rootFolder, new LegacyConfigProvider(rootFolder));
                    break;
                case ATOMIC_ZIP:
                    // not yet done, fall through
                default:
                    break;
            }
        }
        return INSTANCE;
    }

    private static final Logger logger = new Logger(ConfigManager.class, LogGroup.Config);

    private void translateLegacyIfPresent(Path folderPath) {
        if (!(m_provider instanceof SqlConfigProvider)) {
            // Cannot import into SQL if we aren't in SQL mode rn
            return;
        }

        var maybeCams = Path.of(folderPath.toAbsolutePath().toString(), "cameras").toFile();
        var maybeCamsBak = Path.of(folderPath.toAbsolutePath().toString(), "cameras_backup").toFile();

        if (maybeCams.exists() && maybeCams.isDirectory()) {
            logger.info("Translating settings zip!");
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

                // Delete the directory because we were successfully able to load the config but were unable
                // to save or copy the folder.
                if (maybeCams.exists()) FileUtils.deleteDirectory(maybeCams.toPath());
            }

            // Save the same config out using SQL loader
            var sql = new SqlConfigProvider(getRootFolder());
            sql.setConfig(loadedConfig);
            sql.saveToDisk();
        }
    }

    public static boolean nukeConfigDirectory() {
        return FileUtils.deleteDirectory(getRootFolder());
    }

    public static boolean saveUploadedSettingsZip(File uploadPath) {
        // Unpack to /tmp/something/photonvision
        var folderPath = Path.of(System.getProperty("java.io.tmpdir"), "photonvision").toFile();
        folderPath.mkdirs();
        ZipUtil.unpack(uploadPath, folderPath);

        // Nuke the current settings directory
        if (!nukeConfigDirectory()) {
            return false;
        }

        // If there's a cameras folder in the upload, we know we need to import from the
        // old style
        var maybeCams = Path.of(folderPath.getAbsolutePath(), "cameras").toFile();
        if (maybeCams.exists() && maybeCams.isDirectory()) {
            var legacy = new LegacyConfigProvider(folderPath.toPath());
            legacy.load();
            var loadedConfig = legacy.getConfig();

            var sql = new SqlConfigProvider(getRootFolder());
            sql.setConfig(loadedConfig);
            return sql.saveToDisk();
        } else {
            // new structure -- just copy and save like we used to
            try {
                org.apache.commons.io.FileUtils.copyDirectory(folderPath, getRootFolder().toFile());
                logger.info("Copied settings successfully!");
                return true;
            } catch (IOException e) {
                logger.error("Exception copying uploaded settings!", e);
                return false;
            }
        }
    }

    public PhotonConfiguration getConfig() {
        return m_provider.getConfig();
    }

    private static Path getRootFolder() {
        return PathManager.getInstance().getRootFolder();
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
        // Strip away known unneeded portions of the log file name
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

    public Path getCalibrationImageSavePath(String uniqueCameraName) {
        var imgFilePath =
                Path.of(configDirectoryFile.toString(), "calibration", uniqueCameraName).toFile();
        if (!imgFilePath.exists()) imgFilePath.mkdirs();
        return imgFilePath.toPath();
    }

    public Path getCalibrationImageSavePathWithRes(Size frameSize, String uniqueCameraName) {
        var imgFilePath =
                Path.of(
                                configDirectoryFile.toString(),
                                "calibration",
                                uniqueCameraName,
                                "imgs",
                                frameSize.toString())
                        .toFile();
        if (!imgFilePath.exists()) imgFilePath.mkdirs();
        return imgFilePath.toPath();
    }

    public boolean saveUploadedHardwareConfig(Path uploadPath) {
        return m_provider.saveUploadedHardwareConfig(uploadPath);
    }

    public boolean saveUploadedHardwareSettings(Path uploadPath) {
        return m_provider.saveUploadedHardwareSettings(uploadPath);
    }

    public boolean saveUploadedNetworkConfig(Path uploadPath) {
        return m_provider.saveUploadedNetworkConfig(uploadPath);
    }

    public boolean saveUploadedAprilTagFieldLayout(Path uploadPath) {
        return m_provider.saveUploadedAprilTagFieldLayout(uploadPath);
    }

    public void requestSave() {
        logger.trace("Requesting save...");
        saveRequestTimestamp = System.currentTimeMillis();
    }

    public void unloadCameraConfigs() {
        this.getConfig().getCameraConfigurations().clear();
    }

    public void clearConfig() {
        logger.info("Clearing configuration!");
        m_provider.clearConfig();
        m_provider.saveToDisk();
    }

    public void saveToDisk() {
        m_provider.saveToDisk();
    }

    private void saveAndWriteTask() {
        // Only save if 1 second has past since the request was made
        while (!Thread.currentThread().isInterrupted()) {
            if (saveRequestTimestamp > 0
                    && (System.currentTimeMillis() - saveRequestTimestamp) > 1000L
                    && allowWriteTask) {
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

    /** Get (and create if not present) the subfolder where ML models are stored */
    public File getModelsDirectory() {
        var ret = new File(configDirectoryFile, "models");
        if (!ret.exists()) ret.mkdirs();
        return ret;
    }

    /**
     * Disable flushing settings to disk as part of our JVM exit hook. Used to prevent uploading all
     * settings from getting its new configs overwritten at program exit and before they're all
     * loaded.
     */
    public void disableFlushOnShutdown() {
        this.flushOnShutdown = false;
    }

    /** Prevent pending automatic saves */
    public void setWriteTaskEnabled(boolean enabled) {
        this.allowWriteTask = enabled;
    }

    public void onJvmExit() {
        if (flushOnShutdown) {
            logger.info("Force-flushing settings...");
            saveToDisk();
        }
    }
}
