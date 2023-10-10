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
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
    private final Logger logger = new Logger(SqlConfigProvider.class, LogGroup.Config);

    static class TableKeys {
        static final String CAM_UNIQUE_NAME = "unique_name";
        static final String CONFIG_JSON = "config_json";
        static final String DRIVERMODE_JSON = "drivermode_json";
        static final String PIPELINE_JSONS = "pipeline_jsons";

        static final String NETWORK_CONFIG = "networkConfig";
        static final String HARDWARE_CONFIG = "hardwareConfig";
        static final String HARDWARE_SETTINGS = "hardwareSettings";
    }

    private static final String dbName = "photon.sqlite";
    private final String dbPath;

    private final Object m_mutex = new Object();
    private final File rootFolder;

    public SqlConfigProvider(Path rootFolder) {
        this.rootFolder = rootFolder.toFile();
        dbPath = Path.of(rootFolder.toString(), dbName).toAbsolutePath().toString();
        logger.debug("Using database " + dbPath);
        initDatabase();
    }

    public PhotonConfiguration getConfig() {
        if (config == null) {
            logger.warn("CONFIG IS NULL!");
        }
        return config;
    }

    private Connection createConn() {
        String url = "jdbc:sqlite:" + dbPath;

        try {
            var conn = DriverManager.getConnection(url);
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            logger.error("Error creating connection", e);
            return null;
        }
    }

    private void tryCommit(Connection conn) {
        try {
            conn.commit();
        } catch (SQLException e) {
            logger.error("Err committing changes: ", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Err rolling back changes: ", e);
            }
        }
    }

    private void initDatabase() {
        // Make sure root dir exists

        if (!rootFolder.exists()) {
            if (rootFolder.mkdirs()) {
                logger.debug("Root config folder did not exist. Created!");
            } else {
                logger.error("Failed to create root config folder!");
            }
        }

        Connection conn = null;
        Statement createGlobalTableStatement = null, createCameraTableStatement = null;
        try {
            conn = createConn();
            if (conn == null) {
                logger.error("No connection, cannot init db");
                return;
            }

            // Create global settings table. Just a dumb table with list of jsons and their
            // name
            try {
                createGlobalTableStatement = conn.createStatement();
                String sql =
                        "CREATE TABLE IF NOT EXISTS global (\n"
                                + " filename TINYTEXT PRIMARY KEY,\n"
                                + " contents mediumtext NOT NULL\n"
                                + ");";
                createGlobalTableStatement.execute(sql);
            } catch (SQLException e) {
                logger.error("Err creating global table", e);
            }

            // Create cameras table, key is the camera unique name
            try {
                createCameraTableStatement = conn.createStatement();
                var sql =
                        "CREATE TABLE IF NOT EXISTS cameras (\n"
                                + " unique_name TINYTEXT PRIMARY KEY,\n"
                                + " config_json text NOT NULL,\n"
                                + " drivermode_json text NOT NULL,\n"
                                + " pipeline_jsons mediumtext NOT NULL\n"
                                + ");";
                createCameraTableStatement.execute(sql);
            } catch (SQLException e) {
                logger.error("Err creating cameras table", e);
            }

            this.tryCommit(conn);
        } finally {
            try {
                if (createGlobalTableStatement != null) createGlobalTableStatement.close();
                if (createCameraTableStatement != null) createCameraTableStatement.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
                // TODO, does the file still save if the SQL connection isn't closed correctly? If so,
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

            try {
                hardwareConfig =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, TableKeys.HARDWARE_CONFIG), HardwareConfig.class);
            } catch (IOException e) {
                logger.error("Could not deserialize hardware config! Loading defaults");
                hardwareConfig = new HardwareConfig();
            }

            try {
                hardwareSettings =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, TableKeys.HARDWARE_SETTINGS), HardwareSettings.class);
            } catch (IOException e) {
                logger.error("Could not deserialize hardware settings! Loading defaults");
                hardwareSettings = new HardwareSettings();
            }

            try {
                networkConfig =
                        JacksonUtils.deserialize(
                                getOneConfigFile(conn, TableKeys.NETWORK_CONFIG), NetworkConfig.class);
            } catch (IOException e) {
                logger.error("Could not deserialize network config! Loading defaults");
                networkConfig = new NetworkConfig();
            }

            var cams = loadCameraConfigs(conn);

            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("SQL Err closing connection while loading: ", e);
            }

            this.config = new PhotonConfiguration(hardwareConfig, hardwareSettings, networkConfig, cams);
        }
    }

    private String getOneConfigFile(Connection conn, String filename) {
        // Query every single row of the global settings db
        PreparedStatement query = null;
        try {
            query =
                    conn.prepareStatement("SELECT contents FROM global where filename=\"" + filename + "\"");

            var result = query.executeQuery();

            while (result.next()) {
                var contents = result.getString("contents");
                return contents;
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
            // Replace this camera's row with the new settings
            var sqlString =
                    "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES "
                            + "(?,?,?,?);";

            for (var c : config.getCameraConfigurations().entrySet()) {
                PreparedStatement statement = conn.prepareStatement(sqlString);

                var config = c.getValue();
                statement.setString(1, c.getKey());
                statement.setString(2, JacksonUtils.serializeToString(config));
                statement.setString(3, JacksonUtils.serializeToString(config.driveModeSettings));

                // Serializing a list of abstract classes sucks. Instead, make it into a array
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

    private void saveGlobal(Connection conn) {
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        PreparedStatement statement3 = null;
        try {
            // Replace this camera's row with the new settings
            var sqlString = "REPLACE INTO global (filename, contents) VALUES " + "(?,?);";

            statement1 = conn.prepareStatement(sqlString);
            addFile(
                    statement1,
                    TableKeys.HARDWARE_SETTINGS,
                    JacksonUtils.serializeToString(config.getHardwareSettings()));
            statement1.executeUpdate();

            statement2 = conn.prepareStatement(sqlString);
            addFile(
                    statement2,
                    TableKeys.NETWORK_CONFIG,
                    JacksonUtils.serializeToString(config.getNetworkConfig()));
            statement2.executeUpdate();
            statement2.close();

            statement3 = conn.prepareStatement(sqlString);
            addFile(
                    statement3,
                    TableKeys.HARDWARE_CONFIG,
                    JacksonUtils.serializeToString(config.getHardwareConfig()));
            statement3.executeUpdate();
            statement3.close();

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
            var sqlString = "REPLACE INTO global (filename, contents) VALUES " + "(?,?);";

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
        return saveOneFile(TableKeys.HARDWARE_CONFIG, uploadPath);
    }

    @Override
    public boolean saveUploadedHardwareSettings(Path uploadPath) {
        return saveOneFile(TableKeys.HARDWARE_SETTINGS, uploadPath);
    }

    @Override
    public boolean saveUploadedNetworkConfig(Path uploadPath) {
        return saveOneFile(TableKeys.NETWORK_CONFIG, uploadPath);
    }

    private HashMap<String, CameraConfiguration> loadCameraConfigs(Connection conn) {
        HashMap<String, CameraConfiguration> loadedConfigurations = new HashMap<>();

        // Querry every single row of the cameras db
        PreparedStatement query = null;
        try {
            query =
                    conn.prepareStatement(
                            String.format(
                                    "SELECT %s, %s, %s, %s FROM cameras",
                                    TableKeys.CAM_UNIQUE_NAME,
                                    TableKeys.CONFIG_JSON,
                                    TableKeys.DRIVERMODE_JSON,
                                    TableKeys.PIPELINE_JSONS));

            var result = query.executeQuery();

            // Iterate over every row/"camera" in the table
            while (result.next()) {
                List<String> dummyList = new ArrayList<>();

                var uniqueName = result.getString(TableKeys.CAM_UNIQUE_NAME);
                var config =
                        JacksonUtils.deserialize(
                                result.getString(TableKeys.CONFIG_JSON), CameraConfiguration.class);
                var driverMode =
                        JacksonUtils.deserialize(
                                result.getString(TableKeys.DRIVERMODE_JSON), DriverModePipelineSettings.class);
                List<?> pipelineSettings =
                        JacksonUtils.deserialize(
                                result.getString(TableKeys.PIPELINE_JSONS), dummyList.getClass());

                List<CVPipelineSettings> loadedSettings = new ArrayList<>();
                for (var str : pipelineSettings) {
                    if (str instanceof String) {
                        loadedSettings.add(JacksonUtils.deserialize((String) str, CVPipelineSettings.class));
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
