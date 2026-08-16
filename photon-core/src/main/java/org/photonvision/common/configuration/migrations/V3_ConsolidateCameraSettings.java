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

import io.avaje.json.JsonException;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;

public class V3_ConsolidateCameraSettings extends MigrationStep {
    private static final String sqlString = 
    """
    ALTER TABLE cameras DROP COLUMN drivermode_json;
    ALTER TABLE cameras DROP COLUMN pipeline_jsons;
    ALTER TABLE cameras DROP COLUMN otherpaths_json;
    """;

    public V3_ConsolidateCameraSettings() {
        super(sqlString);
    }

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDescription() {
        return "Update camera config JSON for 2027";
    }

    @Override
    void preUpdate(Connection conn) throws SQLException {
        super.preUpdate(conn);
        final var cameraInfoPattern = Pattern.compile("\"(PV[\\w.]*CameraInfo)\"\\s*(:?)");

        // Fetch all camera data first, then close the result set before making modifications
        var query =
                conn.prepareStatement(
                        "SELECT unique_name, config_json, drivermode_json, pipeline_jsons, otherpaths_json FROM cameras;");
        var result = query.executeQuery();
        
        // Collect all camera data into a list to release the result set
        var cameraDataList = new java.util.ArrayList<Map<String, String>>();
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

        // Now process the collected data
        for (var cameraData : cameraDataList) {
            try {
                JsonType<List<String>> strListJsonb = Jsonb.instance().type(Types.listOf(String.class));
                String uniqueName = cameraData.get("unique_name");

                var configJson = cameraData.get("config_json");

                // MIGRATION: 2026
                var cameraInfoMatcher = cameraInfoPattern.matcher(configJson);
                if (cameraInfoMatcher.find() && cameraInfoMatcher.group(2).equals(":")) {
                    logger.info("Legacy type-wrapper PVCameraInfo being migrated");
                    configJson = remapConfigJson(configJson, cameraInfoMatcher.group(1));
                }

                CameraConfiguration config =
                        Jsonb.instance().type(CameraConfiguration.class).fromJson(configJson);

                // // MIGRATION: 2024
                // if (config.matchedCameraInfo == null) {
                //     logger.info("Legacy CameraConfiguration detected - upgrading");

                //     // manually create the matchedCameraInfo ourselves. Need to upgrade:
                //     // baseName, path, otherPaths, cameraType, usbvid/pid -> matchedCameraInfo
                //     config.matchedCameraInfo =
                //             Jsonb.instance()
                //                     .type(LegacyCameraConfigStruct.class)
                //                     .fromJson(configJson)
                //                     .matchedCameraInfo;

                //     // Except that otherPaths used to be its own column. so hack that in here as well
                //     var otherPaths =
                //             Jsonb.instance()
                //                     .type(String[].class)
                //                     .fromJson(cameraData.get("otherpaths_json"));
                //     if (config.matchedCameraInfo instanceof UsbCameraInfo usbInfo) {
                //         usbInfo.otherPaths = otherPaths;
                //     }
                // }

                // MIGRATION: 2026
                List<String> legacyPipelineSettings =
                        strListJsonb.fromJson(cameraData.get("pipeline_jsons"));

                for (var pipelineJson : legacyPipelineSettings) {
                    logger.info("Importing pipeline JSON into camera settings");
                    if (pipelineJson.startsWith("[")) {
                        logger.info("Legacy type-wrapper CVPipelineSettings being migrated");
                        pipelineJson = CVPipelineSettings.remapSettingsJson(pipelineJson);
                    }

                    try {
                        config.pipelineSettings.add(
                                Jsonb.instance().type(CVPipelineSettings.class).fromJson(pipelineJson));
                    } catch (IllegalStateException | JsonException e) {
                        logger.error("Could not deserialize pipeline setting for camera " + config.nickname, e);
                    }
                }

                // MIGRATION: 2026
                if (config.driveModeSettings == null) {
                    logger.info("Importing driver mode JSON into camera settings");
                    var driverModeJson = cameraData.get("drivermode_json");
                    if (driverModeJson.startsWith("[")) {
                        logger.info("Legacy type-wrapper CVPipelineSettings being migrated");
                        driverModeJson = CVPipelineSettings.remapSettingsJson(driverModeJson);
                    }

                    config.driveModeSettings =
                            Jsonb.instance().type(DriverModePipelineSettings.class).fromJson(driverModeJson);
                }

                // loadedConfigurations.put(uniqueName, config);
                // update camera in database
                var sqlString =
                        "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES (?, ?, ?, ?);";
                var statement = conn.prepareStatement(sqlString);
                try {
                    statement.setString(1, uniqueName);
                    statement.setString(2, Jsonb.instance().type(CameraConfiguration.class).toJson(config));
                    statement.setString(3, "null");
                    statement.setString(4, "[]");
                    statement.executeUpdate();
                } finally {
                    statement.close();
                }

            } catch (IllegalStateException | JsonException e) {
                logger.error(
                        "Could not deserialize camera configuration " + cameraData.get("unique_name") + " from database!", e);
            }
        }
    }

    public static String remapConfigJson(String configJson, String cameraType) {
        final JsonType<Map<String, Object>> objMapJsonb =
                Jsonb.instance().type(Types.mapOf(Object.class));

        Map<String, Object> cameraMigration = objMapJsonb.fromJson(configJson);

        @SuppressWarnings("unchecked")
        var cameraMigrationIn = (Map<String, Object>) cameraMigration.get("matchedCameraInfo");

        @SuppressWarnings("unchecked")
        var cameraData = (Map<String, Object>) cameraMigrationIn.get(cameraType);

        Map<String, Object> cameraMigrationOut = new HashMap<>();
        cameraMigrationOut.putAll(cameraData);
        cameraMigrationOut.put("type", "PVCameraInfo." + cameraType);

        cameraMigration.put("matchedCameraInfo", cameraMigrationOut);
        return objMapJsonb.toJson(cameraMigration);
    }
}
