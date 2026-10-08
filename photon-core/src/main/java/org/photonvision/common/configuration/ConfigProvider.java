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

import static org.photonvision.common.configuration.migrations.DbMigration.Columns;
import static org.photonvision.common.configuration.migrations.DbMigration.Tables;

import io.avaje.json.JsonException;
import io.avaje.jsonb.Jsonb;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opencv.core.Size;
import org.photonvision.common.configuration.migrations.*;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.FileUtils;
import org.photonvision.vision.processes.VisionSource;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Saves settings in a SQLite database file (called photon.sqlite).
 *
 * <p>Within this database we have a cameras database, which has one row per camera, and holds:
 * unique_name, config_json, drivermode_json, pipeline_jsons.
 *
 * <p>Global has one row per global config file (like hardware settings and network settings)
 */
public class ConfigProvider {
    static ConfigProvider INSTANCE;

    private static final Logger logger = new Logger(ConfigProvider.class, LogGroup.Config);

    protected PhotonConfiguration config;
    final File configDirectoryFile;

    private Thread settingsSaveThread;
    private long saveRequestTimestamp = -1;
    private boolean flushOnShutdown = true;
    private boolean allowWriteTask = true;

    static class GlobalKeys {
        static final String NETWORK_CONFIG = "networkConfig";
        static final String HARDWARE_CONFIG = "hardwareConfig";
        static final String HARDWARE_SETTINGS = "hardwareSettings";
        static final String FIELD_CONFIG_FILE = "fieldLayout";
        static final String NEURAL_NETWORK_PROPERTIES = "neuralNetworkProperties";
    }

    private static final String dbNameDefault = "photon.sqlite";

    private final String dbPath;
    private final String url;
    private int dbVersion;
    private MigrationManager migrations;

    private final Object m_mutex = new Object();

    public ConfigProvider(Path rootPath, String dbName) {
        File rootFolder = rootPath.toFile();
        configDirectoryFile = rootFolder;
        // Make sure root dir exists
        if (!rootFolder.exists()) {
            if (rootFolder.mkdirs()) {
                logger.debug("Root config folder did not exist. Created!");
            } else {
                logger.error("Failed to create root config folder!");
            }
        }
        dbPath = Path.of(rootFolder.toString(), dbName).toAbsolutePath().toString();
        url = "jdbc:sqlite:" + dbPath;
        logger.debug("Using database " + dbPath);
        migrations = DbMigration.getMigration();
        initDatabase();
    }

    public ConfigProvider(Path rootPath) {
        this(rootPath, dbNameDefault);
    }

    public static ConfigProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigProvider(PathManager.getInstance().getConfigDir());
        }
        return INSTANCE;
    }

    private static Path getRootFolder() {
        return PathManager.getInstance().getConfigDir();
    }

    public static boolean nukeConfigDirectory() {
        return FileUtils.deleteDirectory(getRootFolder());
    }

    public static boolean saveUploadedSettingsZip(File uploadPath) {
        var folderPath = Path.of(System.getProperty("java.io.tmpdir"), "photonvision").toFile();
        folderPath.mkdirs();
        ZipUtil.unpack(uploadPath, folderPath);

        if (!nukeConfigDirectory()) {
            return false;
        }

        try {
            org.apache.commons.io.FileUtils.copyDirectory(folderPath, getRootFolder().toFile());
            logger.info("Copied settings successfully!");
            return true;
        } catch (IOException e) {
            logger.error("Exception copying uploaded settings!", e);
            return false;
        }
    }

    public static Path getImageMetadataPath() {
        return Path.of("/opt/photonvision/image-metadata.json");
    }

    public void addCameraConfigurations(List<VisionSource> sources) {
        getConfig().addCameraConfigs(sources);
        requestSave();
    }

    public void addCameraConfiguration(CameraConfiguration cameraConfig) {
        getConfig().addCameraConfig(cameraConfig);
        requestSave();
    }

    public void saveModule(CameraConfiguration cameraConfig, String uniqueName) {
        getConfig().addCameraConfig(uniqueName, cameraConfig);
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

    public File getObjectDetectionExportAsZip() {
        File out =
                Path.of(System.getProperty("java.io.tmpdir"), "photonvision-object-detection-models.zip")
                        .toFile();
        File tempProperties =
                Path.of(getModelsDirectory().toString(), "photonvision-object-detection-models.json")
                        .toFile();
        try {
            Jsonb.instance()
                    .type(NeuralNetworkModelsSettings.class)
                    .toJson(getConfig().getNeuralNetworkProperties(), new FileWriter(tempProperties));
            ZipUtil.pack(getModelsDirectory(), out);
            if (tempProperties.exists()) {
                Files.delete(tempProperties.toPath());
            }
        } catch (IOException | IllegalStateException | JsonException e) {
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
        fname = fname.replace(LOG_PREFIX, "").replace(LOG_EXT, "");
        DateFormat format = new SimpleDateFormat(LOG_DATE_TIME_FORMAT);
        return format.parse(fname);
    }

    public Path getLogPath() {
        var logFile = Path.of(getLogsDir().toString(), taToLogFname(LocalDateTime.now())).toFile();
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

    /**
     * Creates a fresh temporary directory for preliminary calibration snapshot images. The Path isn't
     * tracked, so the caller is responsible for cleaning it up.
     */
    public Path createPreliminaryCalibrationImageDir(String uniqueCameraName) throws IOException {
        return Files.createTempDirectory("photonvision_preliminary_" + uniqueCameraName);
    }

    public void requestSave() {
        logger.trace("Requesting save...");
        saveRequestTimestamp = System.currentTimeMillis();
        if (settingsSaveThread == null) {
            synchronized (this) {
                if (settingsSaveThread == null) {
                    settingsSaveThread = new Thread(this::saveAndWriteTask);
                    settingsSaveThread.setDaemon(true);
                    settingsSaveThread.start();
                }
            }
        }
    }

    public void unloadCameraConfigs() {
        getConfig().getCameraConfigurations().clear();
    }

    public void clearConfig() {
        logger.info("Clearing configuration!");
        config = new PhotonConfiguration();
        saveToDisk();
    }

    private void saveAndWriteTask() {
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

    public File getModelsDirectory() {
        var ret = new File(configDirectoryFile, "models");
        if (!ret.exists()) ret.mkdirs();
        return ret;
    }

    public void disableFlushOnShutdown() {
        flushOnShutdown = false;
    }

    public void setWriteTaskEnabled(boolean enabled) {
        allowWriteTask = enabled;
    }

    public void onJvmExit() {
        if (flushOnShutdown) {
            logger.info("Force-flushing settings...");
            saveToDisk();
        }
    }

    public PhotonConfiguration getConfig() {
        if (config == null) {
            logger.warn("CONFIG IS NULL!");
        }
        return config;
    }

    private Connection createConn(boolean autoCommit) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            conn.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            logger.error("Error creating connection", e);
        }
        return conn;
    }

    private Connection createConn() {
        return createConn(false);
    }

    private void tryCommit(Connection conn) {
        try {
            conn.commit();
        } catch (SQLException e1) {
            logger.error("Error committing changes: ", e1);
            try {
                conn.rollback();
            } catch (SQLException e2) {
                logger.error("Error rolling back changes: ", e2);
            }
        }
    }

    public int getDbVersion() {
        return this.dbVersion;
    }

    public int getExpectedVersion() {
        return migrations.getVersion();
    }

    private void initDatabase() {
        try {
            dbVersion = migrations.run(url);
            logger.info("Using database version: " + dbVersion);
        } catch (MigrationException e) {
            logger.error("Migration failure! ", e);
        }
    }

    public boolean saveToDisk() {
        logger.debug("Saving to disk");
        var start_time = System.currentTimeMillis();
        var conn = createConn();
        if (conn == null) return false;

        synchronized (m_mutex) {
            if (config == null) {
                logger.error("Config null! Cannot save");
                return false;
            }

            saveCameras(conn);
            saveGlobal(conn);
            tryCommit(conn);

            try {
                conn.close();
            } catch (SQLException e) {
                // TODO, does the file still save if the SQL connection isn't closed correctly?
                // If so, return false here.
                logger.error("SQL Error closing connection while saving to disk: ", e);
            }
        }

        var end_time = System.currentTimeMillis();
        logger.info("Settings saved in " + (end_time - start_time) + " ms!");
        return true;
    }

    private <T> T loadConfigOrDefault(
            Connection conn, String key, Class<T> ref, Supplier<T> factory) {
        String configString = getOneConfigFile(conn, key);
        T configObj;
        if (!configString.isBlank()) {
            try {
                configObj = Jsonb.instance().type(ref).fromJson(configString);
                logger.info("Loaded " + ref.getSimpleName() + " from database");
                return configObj;
            } catch (IllegalStateException | JsonException e) {
                logger.error("Could not deserialize " + ref.getSimpleName() + " from database!", e);
            }
        } else {
            logger.debug("No " + ref.getSimpleName() + " in database");
        }
        // either the config entry is empty or Jsonb threw an exception
        try {
            configObj = factory.get();
            logger.info("Loaded default " + ref.getSimpleName());
            return configObj;
        } catch (Exception e) {
            logger.error("Failed to construct a default instance of " + ref.getSimpleName(), e);
        }
        return null;
    }

    private Field fieldDefault() {
        Field field;
        try {
            field = Field.loadField(Fields.DEFAULT_FIELD);
            logger.info("Loaded " + Fields.DEFAULT_FIELD.toString() + " field");
        } catch (UncheckedIOException e) {
            logger.error("Error loading WPILib field", e);
            logger.info("Creating an empty field");
            field = new Field("", "", "", null, 0, 0, "", null);
        }
        return field;
    }

    /**
     * MIGRATION: 2026
     *
     * <p>Loads the stored AprilTag field layout, migrating any legacy {@code AprilTagFieldLayout}
     * JSON to the newer {@link Field} format before deserializing. If a migration happened the
     * upgraded JSON is written back to the database so the stored data self-upgrades.
     */
    private Field loadField(Connection conn) {
        String configString = getOneConfigFile(conn, GlobalKeys.FIELD_CONFIG_FILE);
        if (configString.isBlank()) {
            // Fall back to the legacy key used by databases written before the Field migration
            configString = getOneConfigFile(conn, "apriltagFieldLayout");
        }
        if (configString.isBlank()) {
            logger.debug("No " + Field.class.getSimpleName() + " in database");
            return fieldDefault();
        }

        try {
            String migrated = FieldLayoutMigration.migrateFieldLayoutJson(configString);
            if (!migrated.equals(configString)) {
                logger.info("Migrated legacy AprilTagFieldLayout to Field format, persisting to database");
                if (!saveOneFile(GlobalKeys.FIELD_CONFIG_FILE, migrated)) {
                    logger.error("Could not persist migrated field layout to database!");
                }
            }
            return Jsonb.instance().type(Field.class).fromJson(migrated);
        } catch (RuntimeException e) {
            logger.error("Could not deserialize " + Field.class.getSimpleName() + " from database!", e);
        }

        // either the config entry was corrupt or Jsonb threw an exception
        return fieldDefault();
    }

    public void load() {
        logger.debug("Loading config...");
        var conn = createConn();
        if (conn == null) return;

        synchronized (m_mutex) {
            var hardwareConfig =
                    loadConfigOrDefault(
                            conn, GlobalKeys.HARDWARE_CONFIG, HardwareConfig.class, HardwareConfig::new);
            var hardwareSettings =
                    loadConfigOrDefault(
                            conn, GlobalKeys.HARDWARE_SETTINGS, HardwareSettings.class, HardwareSettings::new);
            var networkConfig =
                    loadConfigOrDefault(
                            conn, GlobalKeys.NETWORK_CONFIG, NetworkConfig.class, NetworkConfig::new);
            var nnProps =
                    loadConfigOrDefault(
                            conn,
                            GlobalKeys.NEURAL_NETWORK_PROPERTIES,
                            NeuralNetworkModelsSettings.class,
                            NeuralNetworkModelsSettings::new);
            var field = loadField(conn);
            var cams = loadCameraConfigs(conn);

            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("SQL Error closing connection while loading: ", e);
            }

            this.config =
                    new PhotonConfiguration(
                            hardwareConfig, hardwareSettings, networkConfig, field, nnProps, cams);
        }
    }

    private String getOneConfigFile(Connection conn, String filename) {
        // Query every single row of the global settings db
        PreparedStatement query = null;
        try {
            query =
                    conn.prepareStatement(
                            String.format(
                                    "SELECT %s FROM %s WHERE %s = \"%s\"",
                                    Columns.GLB_CONTENTS, Tables.GLOBAL, Columns.GLB_CONFIG_NAME, filename));

            var result = query.executeQuery();

            while (result.next()) {
                return result.getString(Columns.GLB_CONTENTS);
            }
        } catch (SQLException e) {
            logger.error("SQL Error getting file " + filename, e);
        } finally {
            try {
                if (query != null) query.close();
            } catch (SQLException e) {
                logger.error("SQL Error closing config file query " + filename, e);
            }
        }

        return "";
    }

    private void saveCameras(Connection conn) {
        try {
            // Delete all cameras we don't need anymore
            String deleteExtraCamsString =
                    String.format(
                            "DELETE FROM %s WHERE %s not in (%s)",
                            Tables.CAMERAS,
                            Columns.CAM_UNIQUE_NAME,
                            config.getCameraConfigurations().keySet().stream()
                                    .map(it -> "\"" + it + "\"")
                                    .collect(Collectors.joining(", ")));

            var stmt = conn.createStatement();
            stmt.executeUpdate(deleteExtraCamsString);

            // Replace this camera's row with the new settings
            var sqlString =
                    String.format(
                            "REPLACE INTO %s (%s, %s) VALUES (?, ?);",
                            Tables.CAMERAS, Columns.CAM_UNIQUE_NAME, Columns.CAM_CONTENTS);

            for (var c : config.getCameraConfigurations().entrySet()) {
                PreparedStatement statement = conn.prepareStatement(sqlString);

                var config = c.getValue();
                statement.setString(1, c.getKey());
                statement.setString(2, Jsonb.instance().type(CameraConfiguration.class).toJson(config));

                statement.executeUpdate();
            }
        } catch (SQLException | IllegalStateException | JsonException e) {
            logger.error("Error saving cameras", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Error rolling back changes: ", e);
            }
        }
    }

    private void addFile(PreparedStatement ps, String key, String value) throws SQLException {
        ps.setString(1, key);
        ps.setString(2, value);
    }

    // NOTE to Future Developers:
    // These booleans form a mechanism to prevent saveGlobal() and
    // saveOneFile() from stepping on each other's toes. Both write
    // to the database on disk, and both write to the same keys, but
    // they use different sources. Generally, if the user has done something
    // to trigger saveOneFile() to get called, it implies they want that
    // configuration, and not whatever is in RAM right now (which is what
    // saveGlobal() uses to write). Therefor, once saveOneFile() is invoked,
    // we record which entry was overwritten in the database and prevent
    // overwriting it when saveGlobal() is invoked (likely by the shutdown
    // that should almost almost almost happen right after saveOneFile() is
    // invoked).
    //
    // In the future, this may not be needed. A better architecture would involve
    // manipulating the RAM representation of configuration when new .json files
    // are uploaded in the UI, and eliminate all other usages of saveOneFile().
    // But, seeing as it's Dec 28 and kickoff is nigh, we put this here and moved
    // on.
    // Thank you for coming to my TED talk.
    private boolean skipSavingHWCfg = false;
    private boolean skipSavingHWSet = false;
    private boolean skipSavingNWCfg = false;
    private boolean skipSavingAPRTG = false;
    private boolean skipSavingNNProps = false;

    private void saveGlobal(Connection conn) {
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        PreparedStatement statement3 = null;
        try {
            // Replace this camera's row with the new settings
            var sqlString =
                    String.format(
                            "REPLACE INTO %s (%s, %s) VALUES (?,?);",
                            Tables.GLOBAL, Columns.GLB_CONFIG_NAME, Columns.GLB_CONTENTS);

            if (!skipSavingHWSet) {
                statement1 = conn.prepareStatement(sqlString);
                addFile(
                        statement1,
                        GlobalKeys.HARDWARE_SETTINGS,
                        Jsonb.instance().type(HardwareSettings.class).toJson(config.getHardwareSettings()));
                statement1.executeUpdate();
            }

            if (!skipSavingNWCfg) {
                statement2 = conn.prepareStatement(sqlString);
                addFile(
                        statement2,
                        GlobalKeys.NETWORK_CONFIG,
                        Jsonb.instance().type(NetworkConfig.class).toJson(config.getNetworkConfig()));
                statement2.executeUpdate();
                statement2.close();
            }

            if (!skipSavingHWCfg) {
                statement3 = conn.prepareStatement(sqlString);
                addFile(
                        statement3,
                        GlobalKeys.HARDWARE_CONFIG,
                        Jsonb.instance().type(HardwareConfig.class).toJson(config.getHardwareConfig()));
                statement3.executeUpdate();
                statement3.close();
            }

            if (!skipSavingNNProps) {
                statement3 = conn.prepareStatement(sqlString);
                addFile(
                        statement3,
                        GlobalKeys.NEURAL_NETWORK_PROPERTIES,
                        Jsonb.instance()
                                .type(NeuralNetworkModelsSettings.class)
                                .toJson(config.getNeuralNetworkProperties()));
                statement3.executeUpdate();
                statement3.close();
            }

        } catch (SQLException | IllegalStateException | JsonException e) {
            logger.error("Error saving global", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Error rolling back changes: ", e);
            }
        } finally {
            try {
                if (statement1 != null) statement1.close();
                if (statement2 != null) statement2.close();
                if (statement3 != null) statement3.close();
            } catch (SQLException e) {
                logger.error("SQL Error closing global settings query ", e);
            }
        }
    }

    /**
     * MIGRATION: 2026
     *
     * <p>When we migrate the field layout, we get the result as a string. Everything else has a path
     * though, so we need this overload to maintain the prior behavior. To remove this migration,
     * we'll want to delete this overload then make the other function accept a path again.
     */
    private boolean saveOneFile(String fname, Path path) {
        try {
            return saveOneFile(fname, Files.readString(path));
        } catch (IOException e) {
            logger.error("Error while reading file to save to global: ", e);
            return false;
        }
    }

    private boolean saveOneFile(String fname, String contents) {
        Connection conn = null;
        PreparedStatement statement1 = null;

        try {
            conn = createConn();
            if (conn == null) {
                return false;
            }

            // Replace this camera's row with the new settings
            var sqlString =
                    String.format(
                            "REPLACE INTO %s (%s, %s) VALUES (?,?);",
                            Tables.GLOBAL, Columns.GLB_CONFIG_NAME, Columns.GLB_CONTENTS);

            statement1 = conn.prepareStatement(sqlString);
            addFile(statement1, fname, contents);
            statement1.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            logger.error("Error while saving file to global: ", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Error rolling back changes: ", e);
            }
            return false;
        } finally {
            try {
                if (statement1 != null) statement1.close();
                conn.close();
            } catch (SQLException e) {
                logger.error("SQL Error saving file " + fname, e);
            }
        }
    }

    public boolean saveUploadedHardwareConfig(Path uploadPath) {
        skipSavingHWCfg = true;
        return saveOneFile(GlobalKeys.HARDWARE_CONFIG, uploadPath);
    }

    public boolean saveUploadedHardwareSettings(Path uploadPath) {
        skipSavingHWSet = true;
        return saveOneFile(GlobalKeys.HARDWARE_SETTINGS, uploadPath);
    }

    public boolean saveUploadedNetworkConfig(Path uploadPath) {
        skipSavingNWCfg = true;
        return saveOneFile(GlobalKeys.NETWORK_CONFIG, uploadPath);
    }

    public boolean saveUploadedFieldLayout(Path uploadPath) {
        skipSavingAPRTG = true;
        return saveOneFile(GlobalKeys.FIELD_CONFIG_FILE, uploadPath);
    }

    public boolean saveUploadedNeuralNetworkProperties(Path uploadPath) {
        skipSavingNNProps = true;
        return saveOneFile(GlobalKeys.NEURAL_NETWORK_PROPERTIES, uploadPath);
    }

    private HashMap<String, CameraConfiguration> loadCameraConfigs(Connection conn) {
        HashMap<String, CameraConfiguration> loadedConfigurations = new HashMap<>();

        // Query every single row of the cameras db
        PreparedStatement query = null;
        try {
            query =
                    conn.prepareStatement(
                            String.format(
                                    "SELECT %s, %s FROM %s",
                                    Columns.CAM_UNIQUE_NAME, Columns.CAM_CONTENTS, Tables.CAMERAS));

            var result = query.executeQuery();

            // Iterate over every row/"camera" in the table
            while (result.next()) {
                String uniqueName = "";
                try {
                    uniqueName = result.getString(Columns.CAM_UNIQUE_NAME);

                    var configJson = result.getString(Columns.CAM_CONTENTS);

                    CameraConfiguration config =
                            Jsonb.instance().type(CameraConfiguration.class).fromJson(configJson);

                    loadedConfigurations.put(uniqueName, config);
                } catch (IllegalStateException | JsonException e) {
                    logger.error(
                            "Could not deserialize camera configuration " + uniqueName + " from database!", e);
                }
            }
        } catch (SQLException e) {
            logger.error("Error querying database to load cameras: ", e);
        } finally {
            try {
                if (query != null) query.close();
            } catch (SQLException e) {
                logger.error("SQL Error closing connection while loading cameras ", e);
            }
        }
        return loadedConfigurations;
    }

    public void setConfig(PhotonConfiguration config) {
        this.config = config;
    }
}
