package org.photonvision.common.configuration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

public class SqlConfigLoader {
    private final Logger logger = new Logger(SqlConfigLoader.class, LogGroup.Config);

    static class TableKeys {
        static final String CAM_UNIQUE_NAME = "unique_name";
        static final String CONFIG_JSON = "config_json";
        static final String DRIVERMODE_JSON = "drivermode_json";
        static final String PIPELINE_JSONS = "pipeline_jsons";
    }

    private String dbPath = "test.db";

    public SqlConfigLoader() {
        initDatabase();
    }

    private Connection createConn() {
        String url = "jdbc:sqlite:" + dbPath;

        try {
            var conn = DriverManager.getConnection(url);
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initDatabase() {
        var conn = createConn();
        if (conn == null) return;

        // Create global settings table. Just a dumb table with list of jsons and their
        // name
        try (var stmt = conn.createStatement()) {
            String sql =
                    "CREATE TABLE IF NOT EXISTS global (\n"
                            + " filename TINYTEXT PRIMARY KEY,\n"
                            + " contents mediumtext NOT NULL\n"
                            + ");";
            stmt.execute(sql);

        } catch (SQLException e) {
            logger.error("Err creating global table", e);
        }

        // Create cameras table, key is the camera unique name
        try (var stmt = conn.createStatement()) {
            var sql =
                    "CREATE TABLE IF NOT EXISTS cameras (\n"
                            + " unique_name TINYTEXT PRIMARY KEY,\n"
                            + " config_json text NOT NULL,\n"
                            + " drivermode_json text NOT NULL,\n"
                            + " pipeline_jsons mediumtext NOT NULL\n"
                            + ");";
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.error("Err creating cameras table", e);
        }
    }

    public void saveCameras(HashMap<String, CameraConfiguration> configMap) {
        var conn = createConn();
        if (conn == null) return;
        try {
            // Replace this camera's row with the new settings
            var sqlString =
                    "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES "
                            + "(?,?,?,?);";

            PreparedStatement statement = conn.prepareStatement(sqlString);

            for (var c : configMap.entrySet()) {
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

                // Add a new row
                statement.addBatch();
            }

            var rowsChanged = statement.executeUpdate();
            conn.commit();
            System.out.println(rowsChanged + " records mutated");
            statement.addBatch(dbPath);
        } catch (SQLException | IOException e) {
            logger.error("Err saving cameras: ", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("Err rolling back changes: ", e);
            }
        }
    }

    public HashMap<String, CameraConfiguration> getAllConfigs() {
        HashMap<String, CameraConfiguration> loadedConfigurations = new HashMap<>();
        var conn = createConn();
        if (conn == null) return loadedConfigurations;

        // Querry every single row of the cameras db
        try (PreparedStatement querry =
                conn.prepareStatement(
                        "SELECT unique_name, config_json, drivermode_json, pipeline_jsons FROM cameras")) {

            var result = querry.executeQuery();

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
        }
        return loadedConfigurations;
    }
}
