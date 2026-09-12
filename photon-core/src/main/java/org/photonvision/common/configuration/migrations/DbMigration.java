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

package org.photonvision.common.configuration.migrations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import io.avaje.json.JsonException;

public class DbMigration {
    private static final Logger logger = new Logger(DbMigration.class, LogGroup.Config);

    public static final MigrationManager getMigration() {
        return new MigrationManager(2)
            .addStep(2, schema02)
            .addStep(3, update2026CameraConfig)
            .addStep(4, sql04);
    }

    private static final String schema02 =
        """
        CREATE TABLE IF NOT EXISTS global (
            filename TEXT PRIMARY KEY,
            contents JSON NOT NULL
        );
        CREATE TABLE IF NOT EXISTS cameras (
            unique_name TINYTEXT PRIMARY KEY,
            config_json text NOT NULL,
            drivermode_json text NOT NULL,
            pipeline_jsons mediumtext NOT NULL,
            otherpaths_json TEXT NOT NULL DEFAULT '[]'
        );
        """;

    private static final String sql04 =
        """
        ALTER TABLE cameras DROP COLUMN drivermode_json;
        ALTER TABLE cameras DROP COLUMN pipeline_jsons;
        ALTER TABLE cameras DROP COLUMN otherpaths_json;
        """;

    private static final String schema04 =
        """
        CREATE TABLE IF NOT EXISTS new_global (
            filename TEXT PRIMARY KEY,
            contents JSON NOT NULL
        );
        INSERT INTO new_global (filename, contents)
        SELECT filename, contents
        FROM global
        WHERE EXISTS (
            SELECT 1 FROM sqlite_master WHERE type='table' AND name='global'
        );
        DROP TABLE IF EXISTS global;
        ALTER TABLE new_global RENAME global;

        CREATE TABLE IF NOT EXISTS new_cameras (
            unique_name TEXT PRIMARY KEY,
            config_json JSON NOT NULL
        );
        INSERT INTO new_cameras (unique_name, config_json)
        SELECT unique_name, config_json
        FROM cameras
        WHERE EXISTS (
            SELECT 1 FROM sqlite_master WHERE type='table' AND name='cameras'
        );
        DROP TABLE IF EXISTS cameras;
        ALTER TABLE new_cameras RENAME TO cameras;
        """;


    private static MigrationFunction update2026CameraConfig = (conn) -> {
        // Fetch all camera data first, then close the result set before making modifications
        var cameraDataList = new ArrayList<Map<String, String>>();

        try {
            var query =
                    conn.prepareStatement(
                            "SELECT unique_name, config_json, drivermode_json, pipeline_jsons, otherpaths_json FROM cameras;");
            var result = query.executeQuery();

            // Collect all camera data into a list to release the result set
            while (result.next()) {
                var cameraData = new HashMap<String, String>();
                cameraData.put("unique_name", result.getString("unique_name"));
                cameraData.put("config_json", result.getString("config_json"));
                cameraData.put("drivermode_json", result.getString("drivermode_json"));
                cameraData.put("pipeline_jsons", result.getString("pipeline_jsons"));
                cameraData.put("otherpaths_json", result.getString("otherpaths_json"));
                cameraDataList.add(cameraData);
            }

            // Close the result set and query statement immediately
            result.close();
            query.close();
        } catch (SQLException e) {
        }

        // Now process the collected data
        try {
            for (var cameraData : cameraDataList) {
                String uniqueName = cameraData.get("unique_name");
                DynamicJsonEditor configJson;

                configJson = new DynamicJsonEditor(cameraData.get("config_json"));
                var matchedCameraInfo = configJson.getMap("matchedCameraInfo");

                // migrate legacy PVCameraInfo type
                if (!matchedCameraInfo.containsKey("type")) {
                    String cameraType = matchedCameraInfo.keySet().iterator().next();
                    logger.debug(
                            String.format(
                                    "Migrating legacy %s type-wrapper for camera %s",
                                    cameraType, configJson.getString("nickname")));
                    String type = String.format("PVCameraInfo.%s", cameraType);
                    var cameraInfo = configJson.getMap(String.format("matchedCameraInfo.%s", cameraType));
                    matchedCameraInfo.put("type", type);
                    matchedCameraInfo.putAll(cameraInfo);
                    matchedCameraInfo.remove(cameraType);
                }

                // Check for other_paths that haven't been migrated
                if (!configJson.hasKey("matchedCameraInfo.otherPaths")) {
                    try (var otherPathsJson = new DynamicJsonEditor(cameraData.get("otherpaths_json"))) {
                        logger.debug("Migrating legacy otherPaths");
                        matchedCameraInfo.put("otherPaths", otherPathsJson.getList(""));
                    } catch (IOException | JsonException e) {
                        logger.warn("Couldn't deserialize otherpaths_json. Skipping.\n" + e);
                    }
                }

                // Migrate pipeline_jsons
                // before 2027, pipeline_jsons contained an array of strings and each string was a
                // pipeline
                // object
                if (!configJson.hasKey("pipelineSettings")) {
                    try (var rawPipelineJsons = new DynamicJsonEditor(cameraData.get("pipeline_jsons"))) {
                        List<Map<String, Object>> pipelines = new ArrayList<Map<String, Object>>();
                        for (String pipelineString : rawPipelineJsons.getList("", String.class)) {
                            var pipelineJson = new DynamicJsonEditor(pipelineString);
                            var oldWrapper = pipelineJson.getList("");
                            String type = (String) oldWrapper.get(0);
                            @SuppressWarnings("unchecked")
                            var pipeline = (Map<String, Object>) oldWrapper.get(1);
                            logger.debug(
                                    String.format(
                                            "Migrating legacy %s, \"%s\"", type, pipeline.get("pipelineNickname")));
                            pipeline.put("type", type);
                            pipelines.add(pipeline);
                            pipelineJson.close();
                        }
                        configJson.getMap("").put("pipelineSettings", pipelines);
                    } catch (IOException | JsonException e) {
                        logger.warn("Couldn't deserialize pipeline_jsons. Skipping.\n" + e);
                    }
                }

                // Migrate drivermode_json
                if (!configJson.hasKey("driveModeSettings")) {
                    try (var legacyDriverModeJson =
                            new DynamicJsonEditor(cameraData.get("drivermode_json"))) {
                        var oldWrapper = legacyDriverModeJson.getList("");
                        String type = (String) oldWrapper.get(0);
                        logger.debug(String.format("Migrating legacy %s", type));
                        @SuppressWarnings("unchecked")
                        var pipeline = (Map<String, Object>) oldWrapper.get(1);
                        configJson.getMap("").put("driveModeSettings", pipeline);
                    } catch (IOException | JsonException e) {
                        logger.warn("Couldn't deserialize drivermode_json. Skipping.\n" + e);
                    }
                }

                // for debugging, remove before merge
                // try {
                //     logger.debug("Migrated config_json:\n" + configJson.export(true));
                // } catch (IOException | JsonException e) {
                //     logger.error("Error serializing configJson.", e);
                // }

                // update camera in database
                var sqlString =
                        "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES (?, ?, ?, ?);";
                try (var statement = conn.prepareStatement(sqlString)) {
                    statement.setString(1, uniqueName);
                    statement.setString(2, configJson.export(false));
                    statement.setString(3, "null");
                    statement.setString(4, "[]");
                    statement.executeUpdate();
                } catch (IOException e) {
                    throw new IOException("Error serializing configJson", e);
                } finally {
                    configJson.close();
                }
            }
        } catch (Exception e) {
            logger.error("Exception thrown during step", e);
        }
        return true;
    };
}
