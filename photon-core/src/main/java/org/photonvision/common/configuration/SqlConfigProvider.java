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

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.cscore.UsbCameraInfo;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.CameraConfiguration.LegacyCameraConfigStruct;
import org.photonvision.common.configuration.DatabaseSchema.Columns;
import org.photonvision.common.configuration.DatabaseSchema.Tables;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;

/**
 * Saves settings in a SQLite database file (called photon.sqlite).
 *
 * <p>Within this database we have a cameras database, which has one row per camera, and holds:
 * unique_name, config_json, drivermode_json, pipeline_jsons.
 *
 * <p>Global has one row per global config file (like hardware settings and network settings)
 */
public class SqlConfigProvider extends ConfigProvider {
    private static final Logger logger = new Logger(SqlConfigProvider.class, LogGroup.Config);

    static class GlobalKeys {
        static final String NETWORK_CONFIG = "networkConfig";
        static final String HARDWARE_CONFIG = "hardwareConfig";
        static final String HARDWARE_SETTINGS = "hardwareSettings";
        static final String ATFL_CONFIG_FILE = "apriltagFieldLayout";
    }

    private static final String dbName = "photon.sqlite";
    // private final File rootFolder;
    private final String dbPath;
    private final String url;

    private final Object m_mutex = new Object();

    public SqlConfigProvider(Path rootPath) {
        File rootFolder = rootPath.toFile();
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
        initDatabase();
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
            logger.error("Err committing changes: ", e1);
            try {
                conn.rollback();
            } catch (SQLException e2) {
                logger.error("Err rolling back changes: ", e2);
            }
        }
    }

    private int getIntPragma(String pragma) {
        int retval = 0;
        try (Connection conn = createConn(true);
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA " + pragma + ";");
            retval = rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error querying " + pragma, e);
        }
        return retval;
    }

    private int getSchemaVersion() {
        return getIntPragma("schema_version");
    }

    public int getUserVersion() {
        return getIntPragma("user_version");
    }

    private void setUserVersion(Connection conn, int value) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + value + ";");
        } catch (SQLException e) {
            logger.error("Error setting user_version to ", e);
        }
    }

    private void doMigration(int index) throws SQLException {
        logger.debug("Running migration step " + index);
        try (Connection conn = createConn();
                Statement stmt = conn.createStatement()) {
            for (String sql : DatabaseSchema.migrations[index].split(";")) {
                stmt.addBatch(sql);
            }
            stmt.executeBatch();
            setUserVersion(conn, index + 1);
            tryCommit(conn);
        } catch (SQLException e) {
            logger.error("Error with migration step " + index, e);
            throw e;
        }
    }

    private void initDatabase() {
        int userVersion = getUserVersion();
        int expectedVersion = DatabaseSchema.migrations.length;

        if (userVersion < expectedVersion) {
            // older database, run migrations

            // first, check to see if this is one of the ones from 2024 beta that need
            // special handling
            if (userVersion == 0 && getSchemaVersion() > 0) {
                String sql =
                        "SELECT COUNT(*) AS CNTREC FROM pragma_table_info('cameras') WHERE name='otherpaths_json';";
                try (Connection conn = createConn(true);
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sql); ) {
                    if (rs.getInt("CNTREC") == 0) {
                        // need to add otherpaths_json
                        userVersion = 1;
                    } else {
                        // already there, no need to add the column
                        userVersion = 2;
                    }
                    setUserVersion(conn, userVersion);
                } catch (SQLException e) {
                    logger.error(
                            "Could not determine the version of the database. Try deleting "
                                    + dbName
                                    + "and restart photonvision.",
                            e);
                }
            }

            logger.debug("Older database version. Migrating ... ");
            try {
                for (int index = userVersion; index < expectedVersion; index++) {
                    doMigration(index);
                }
                logger.debug("Database migration complete");
            } catch (SQLException e) {
                logger.error("Error with database migration", e);
            }
        }

        // Warn if the database still isn't at the correct version
        userVersion = getUserVersion();
        if (userVersion > expectedVersion) {
            // database must be from a newer version, so warn
            logger.warn(
                    "This database is from a newer version of PhotonVision. Check that you are running the right version of PhotonVision.");
        } else if (userVersion < expectedVersion) {
            // migration didn't work, so warn
            logger.warn(
                    "This database migration failed. Expected version: "
                            + expectedVersion
                            + ", got version: "
                            + userVersion);
        } else {
            // migration worked
            logger.info("Using correct database version: " + userVersion);
        }
    }

    @Override
    public boolean saveToDisk() {
        logger.debug("Saving to disk");
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
                // If so,
                // return false here.
                logger.error("SQL Err closing connection while saving to disk: ", e);
            }
        }

        logger.info("Settings saved!");
        return true;
    }

    @Override
    public void load() {
        logger.debug("Loading config...");
        var conn = createConn();
        if (conn == null) return;

        synchronized (m_mutex) {
            HardwareConfig hardwareConfig;
            HardwareSettings hardwareSettings;
            NetworkConfig networkConfig;
            AprilTagFieldLayout atfl;

            try {
                hardwareConfig =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, GlobalKeys.HARDWARE_CONFIG), HardwareConfig.class);
            } catch (IOException e) {
                logger.error("Could not deserialize hardware config! Loading defaults", e);
                hardwareConfig = new HardwareConfig();
            }

            try {
                hardwareSettings =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, GlobalKeys.HARDWARE_SETTINGS), HardwareSettings.class);
            } catch (IOException e) {
                logger.error("Could not deserialize hardware settings! Loading defaults", e);
                hardwareSettings = new HardwareSettings();
            }

            try {
                networkConfig =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, GlobalKeys.NETWORK_CONFIG), NetworkConfig.class);
            } catch (IOException e) {
                logger.error("Could not deserialize network config! Loading defaults", e);
                networkConfig = new NetworkConfig();
            }

            try {
                atfl =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, GlobalKeys.ATFL_CONFIG_FILE), AprilTagFieldLayout.class);
            } catch (IOException e) {
                logger.error("Could not deserialize apriltag layout! Loading defaults", e);
                try {
                    atfl = AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);
                } catch (UncheckedIOException e2) {
                    logger.error("Error loading WPILib field", e);
                    atfl = null;
                }
                if (atfl == null) {
                    // what do we even do here lmao -- wpilib should always work
                    logger.error("Field layout is *still* null??????");
                    atfl = new AprilTagFieldLayout(List.of(), 1, 1);
                }
            }

            var cams = loadCameraConfigs(conn);

            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("SQL Err closing connection while loading: ", e);
            }

            this.config =
                    new PhotonConfiguration(hardwareConfig, hardwareSettings, networkConfig, atfl, cams);
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
                                    Columns.GLB_CONTENTS, Tables.GLOBAL, Columns.GLB_FILENAME, filename));

            var result = query.executeQuery();

            while (result.next()) {
                return result.getString(Columns.GLB_CONTENTS);
            }
        } catch (SQLException e) {
            logger.error("SQL Err getting file " + filename, e);
        } finally {
            try {
                if (query != null) query.close();
            } catch (SQLException e) {
                logger.error("SQL Err closing config file query " + filename, e);
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
                            "REPLACE INTO %s (%s, %s, %s, %s) VALUES (?,?,?,?);",
                            Tables.CAMERAS,
                            Columns.CAM_UNIQUE_NAME,
                            Columns.CAM_CONFIG_JSON,
                            Columns.CAM_DRIVERMODE_JSON,
                            Columns.CAM_PIPELINE_JSONS);

            for (var c : config.getCameraConfigurations().entrySet()) {
                PreparedStatement statement = conn.prepareStatement(sqlString);

                var config = c.getValue();
                statement.setString(1, c.getKey());
                statement.setString(2, JacksonUtils.serializeToString(config));
                statement.setString(3, JacksonUtils.serializeToString(config.driveModeSettings));

                // Serializing a list of abstract classes sucks. Instead, make it into an array
                // of strings, which we can later unpack back into individual settings
                List<String> settings =
                        config.pipelineSettings.stream()
                                .map(
                                        it -> {
                                            try {
                                                return JacksonUtils.serializeToString(it);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                return null;
                                            }
                                        })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                statement.setString(4, JacksonUtils.serializeToString(settings));

                statement.executeUpdate();
            }

        } catch (SQLException | IOException e) {
            logger.error("Err saving cameras", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Err rolling back changes: ", e);
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

    private void saveGlobal(Connection conn) {
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        PreparedStatement statement3 = null;
        try {
            // Replace this camera's row with the new settings
            var sqlString =
                    String.format(
                            "REPLACE INTO %s (%s, %s) VALUES (?,?);",
                            Tables.GLOBAL, Columns.GLB_FILENAME, Columns.GLB_CONTENTS);

            if (!skipSavingHWSet) {
                statement1 = conn.prepareStatement(sqlString);
                addFile(
                        statement1,
                        GlobalKeys.HARDWARE_SETTINGS,
                        JacksonUtils.serializeToString(config.getHardwareSettings()));
                statement1.executeUpdate();
            }

            if (!skipSavingNWCfg) {
                statement2 = conn.prepareStatement(sqlString);
                addFile(
                        statement2,
                        GlobalKeys.NETWORK_CONFIG,
                        JacksonUtils.serializeToString(config.getNetworkConfig()));
                statement2.executeUpdate();
                statement2.close();
            }

            if (!skipSavingHWCfg) {
                statement3 = conn.prepareStatement(sqlString);
                addFile(
                        statement3,
                        GlobalKeys.HARDWARE_CONFIG,
                        JacksonUtils.serializeToString(config.getHardwareConfig()));
                statement3.executeUpdate();
                statement3.close();
            }

        } catch (SQLException | IOException e) {
            logger.error("Err saving global", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Err rolling back changes: ", e);
            }
        } finally {
            try {
                if (statement1 != null) statement1.close();
                if (statement2 != null) statement2.close();
                if (statement3 != null) statement3.close();
            } catch (SQLException e) {
                logger.error("SQL Err closing global settings query ", e);
            }
        }
    }

    private boolean saveOneFile(String fname, Path path) {
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
                            Tables.GLOBAL, Columns.GLB_FILENAME, Columns.GLB_CONTENTS);

            statement1 = conn.prepareStatement(sqlString);
            addFile(statement1, fname, Files.readString(path));
            statement1.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException | IOException e) {
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

    @Override
    public boolean saveUploadedHardwareConfig(Path uploadPath) {
        skipSavingHWCfg = true;
        return saveOneFile(GlobalKeys.HARDWARE_CONFIG, uploadPath);
    }

    @Override
    public boolean saveUploadedHardwareSettings(Path uploadPath) {
        skipSavingHWSet = true;
        return saveOneFile(GlobalKeys.HARDWARE_SETTINGS, uploadPath);
    }

    @Override
    public boolean saveUploadedNetworkConfig(Path uploadPath) {
        skipSavingNWCfg = true;
        return saveOneFile(GlobalKeys.NETWORK_CONFIG, uploadPath);
    }

    @Override
    public boolean saveUploadedAprilTagFieldLayout(Path uploadPath) {
        skipSavingAPRTG = true;
        return saveOneFile(GlobalKeys.ATFL_CONFIG_FILE, uploadPath);
    }

    private HashMap<String, CameraConfiguration> loadCameraConfigs(Connection conn) {
        HashMap<String, CameraConfiguration> loadedConfigurations = new HashMap<>();

        // Query every single row of the cameras db
        PreparedStatement query = null;
        try {
            query =
                    conn.prepareStatement(
                            String.format(
                                    "SELECT %s, %s, %s, %s, %s FROM %s",
                                    Columns.CAM_UNIQUE_NAME,
                                    Columns.CAM_CONFIG_JSON,
                                    Columns.CAM_DRIVERMODE_JSON,
                                    Columns.CAM_OTHERPATHS_JSON,
                                    Columns.CAM_PIPELINE_JSONS,
                                    Tables.CAMERAS));

            var result = query.executeQuery();

            // Iterate over every row/"camera" in the table
            while (result.next()) {
                List<String> dummyList = new ArrayList<>();

                var uniqueName = result.getString(Columns.CAM_UNIQUE_NAME);

                // A horrifying hack to keep backward compat with otherpaths
                // We -really- need to delete this -stupid- otherpaths column. I hate it.
                var configStr = result.getString(Columns.CAM_CONFIG_JSON);
                CameraConfiguration config = JacksonUtils.deserialize(configStr, CameraConfiguration.class);

                if (config.matchedCameraInfo == null) {
                    logger.info("Legacy CameraConfiguration detected - upgrading");

                    // manually create the matchedCameraInfo ourselves. Need to upgrade:
                    // baseName, path, otherPaths, cameraType, usbvid/pid -> matchedCameraInfo
                    config.matchedCameraInfo =
                            JacksonUtils.deserialize(configStr, LegacyCameraConfigStruct.class).matchedCameraInfo;

                    // Except that otherPaths used to be its own column. so hack that in here as well
                    var otherPaths =
                            JacksonUtils.deserialize(
                                    result.getString(Columns.CAM_OTHERPATHS_JSON), String[].class);
                    if (config.matchedCameraInfo instanceof UsbCameraInfo usbInfo) {
                        usbInfo.otherPaths = otherPaths;
                    }
                }

                var driverMode =
                        JacksonUtils.deserialize(
                                result.getString(Columns.CAM_DRIVERMODE_JSON), DriverModePipelineSettings.class);
                List<?> pipelineSettings =
                        JacksonUtils.deserialize(
                                result.getString(Columns.CAM_PIPELINE_JSONS), dummyList.getClass());

                List<CVPipelineSettings> loadedSettings = new ArrayList<>();
                for (var setting : pipelineSettings) {
                    if (setting instanceof String str) {
                        loadedSettings.add(JacksonUtils.deserialize(str, CVPipelineSettings.class));
                    }
                }

                config.pipelineSettings = loadedSettings;
                config.driveModeSettings = driverMode;
                loadedConfigurations.put(uniqueName, config);
            }
        } catch (SQLException | IOException e) {
            logger.error("Err loading cameras: ", e);
        } finally {
            try {
                if (query != null) query.close();
            } catch (SQLException e) {
                logger.error("SQL Err closing connection while loading cameras ", e);
            }
        }
        return loadedConfigurations;
    }

    public void setConfig(PhotonConfiguration config) {
        this.config = config;
    }
}
