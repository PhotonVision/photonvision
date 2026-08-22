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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class V3_ConsolidateCameraSettings extends MigrationStep {

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDescription() {
        return "Update camera config JSON for 2027";
    }

    @Override
    void update(Connection conn) throws SQLException, IOException {

        // Fetch all camera data first, then close the result set before making modifications
        var query =
                conn.prepareStatement(
                        "SELECT unique_name, config_json, drivermode_json, pipeline_jsons, otherpaths_json FROM cameras;");
        var result = query.executeQuery();

        // Collect all camera data into a list to release the result set
        var cameraDataList = new ArrayList<Map<String, String>>();
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
            String uniqueName = cameraData.get("unique_name");
            DynamicJsonEditor configJson;

            configJson = new DynamicJsonEditor(cameraData.get("config_json"));
            var matchedCameraInfo = configJson.getMap("matchedCameraInfo");

            // migrate legacy PVCameraInfo type
            if (!matchedCameraInfo.containsKey("type")) {
                String cameraType = matchedCameraInfo.keySet().iterator().next();
                logger.debug("Legacy " + cameraType + " type-wrapper being migrated");
                String type = String.format("PVCameraInfo.%s", cameraType);
                logger.debug(String.format("matchedCameraInfo.%s", cameraType));
                var cameraInfo = configJson.getMap(String.format("matchedCameraInfo.%s", cameraType));
                matchedCameraInfo.put("type", type);
                matchedCameraInfo.putAll(cameraInfo);
                matchedCameraInfo.remove(cameraType);
            }

            // Check for other_paths that haven't been migrated
            if (!configJson.hasKey("matchedCameraInfo.otherPaths")) {
                try (var otherPathsJson = new DynamicJsonEditor(cameraData.get("otherpaths_json"))) {
                    matchedCameraInfo.put("otherPaths", otherPathsJson.getList(""));
                } catch (IOException | JsonException e) {
                    logger.warn("Couldn't deserialize otherpaths_json. Skipping.\n" + e);
                }
            }

            // Migrate pipeline_jsons
            // before 2027, pipeline_jsons contained an array of strings and each string was a pipeline
            // object
            if (!configJson.hasKey("pipelineSettings")) {
                try (var rawPipelineJsons = new DynamicJsonEditor(cameraData.get("pipeline_jsons"))) {
                    List<Map<String, Object>> pipelines = new ArrayList<Map<String, Object>>();
                    for (String pipelineString : rawPipelineJsons.getList("", String.class)) {
                        var pipelineJson = new DynamicJsonEditor(pipelineString);
                        var oldWrapperIterator = pipelineJson.getList("").iterator();
                        String type = (String) oldWrapperIterator.next();
                        @SuppressWarnings("unchecked")
                        var pipeline = (Map<String, Object>) oldWrapperIterator.next();
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
                try (var legacyDriverModeJson = new DynamicJsonEditor(cameraData.get("drivermode_json"))) {
                    var oldWrapperIterator = legacyDriverModeJson.getList("").iterator();
                    String type = (String) oldWrapperIterator.next();
                    @SuppressWarnings("unchecked")
                    var pipeline = (Map<String, Object>) oldWrapperIterator.next();
                    configJson.getMap("").put("driveModeSettings", pipeline);
                } catch (IOException | JsonException e) {
                    logger.warn("Couldn't deserialize drivermode_json. Skipping.\n" + e);
                }
            }

            // update camera in database
            try {
                logger.debug("Migrated config_json:\n" + configJson.export(true));
            } catch (IOException | JsonException e) {
                logger.error("Error serializing configJson.", e);
            }
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
    }
}
