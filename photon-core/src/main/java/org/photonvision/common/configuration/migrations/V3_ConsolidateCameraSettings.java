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
    private static final String sqlString = "";

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
    void update(Connection conn) throws SQLException {
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
                    logger.debug("Legacy type-wrapper PVCameraInfo being migrated");
                    configJson = remapConfigJson(configJson, cameraInfoMatcher.group(1));
                }

                CameraConfiguration config =
                        Jsonb.instance().type(CameraConfiguration.class).fromJson(configJson);

                // // MIGRATION: 2024
                // if (config.matchedCameraInfo == null) {
                //     logger.debug("Legacy CameraConfiguration detected - upgrading");

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
                    logger.debug("Importing pipeline JSON into camera settings");
                    if (pipelineJson.startsWith("[")) {
                        logger.debug("Legacy type-wrapper CVPipelineSettings being migrated");
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
                    logger.debug("Importing driver mode JSON into camera settings");
                    var driverModeJson = cameraData.get("drivermode_json");
                    if (driverModeJson.startsWith("[")) {
                        logger.debug("Legacy type-wrapper CVPipelineSettings being migrated");
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
                        "Could not deserialize camera configuration "
                                + cameraData.get("unique_name")
                                + " from database!",
                        e);
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

// @Json
// class CameraConfiguration {
//     /** A unique name (ostensibly an opaque UUID) to identify this particular configuration */
//     public String uniqueName = "";

//     /**
//      * The info of the camera we last matched to. We still match by unique path (where we can),
// but
//      * this is useful to provide warnings to users
//      */
//     public PVCameraInfo matchedCameraInfo;

//     /** User-set nickname */
//     public String nickname = "";

//     /** Deactivated vision modules do not open camera hardware or lock USB ports */
//     public boolean deactivated = false;

//     public QuirkyCamera cameraQuirks;

//     public double FOV = 70;
//     public List<CameraCalibrationCoefficients> calibrations = new ArrayList<>();
//     public int currentPipelineIndex = 0;

//     public int streamIndex = 0; // 0 index means ports [1181, 1182], 1 means [1183, 1184], etc...

//     public List<CVPipelineSettings> pipelineSettings = new ArrayList<>();

//     public DriverModePipelineSettings driveModeSettings = new DriverModePipelineSettings();

//     public CameraConfiguration(PVCameraInfo cameraInfo, String uniqueName, String nickname) {
//         this.matchedCameraInfo = cameraInfo;
//         this.uniqueName = uniqueName;
//         this.nickname = nickname;
//         this.calibrations = new ArrayList<>();

//         logger.debug("Creating USB camera configuration for " + this.toShortString());
//     }

//     // JSON Constructor (can't be marked with @Json.Creator due to public fields that aren't part
// of
//     // the parameters)
//     public CameraConfiguration(
//             String uniqueName,
//             PVCameraInfo matchedCameraInfo,
//             String nickname,
//             boolean deactivated,
//             QuirkyCamera cameraQuirks,
//             double FOV,
//             int currentPipelineIndex) {
//         this.uniqueName = uniqueName;
//         this.matchedCameraInfo = matchedCameraInfo;
//         this.nickname = nickname;
//         this.deactivated = deactivated;
//         this.cameraQuirks = cameraQuirks;
//         this.FOV = FOV;
//         this.currentPipelineIndex = currentPipelineIndex;
//     }

//     // Special case constructor for use with File sources
//     public CameraConfiguration(String uniqueName, PVCameraInfo camInfo) {
//         this.uniqueName = uniqueName;
//         this.matchedCameraInfo = camInfo;
//         this.nickname = camInfo.humanReadableName();
//         this.calibrations = new ArrayList<>();
//         this.cameraQuirks = null; // we'll deal with this later. TODO: should we not just do it
// now?
//     }

//     /**
//      * Constructor for when we don't know anything about the camera yet. Generates a UUID for the
//      * unique name
//      */
//     public CameraConfiguration(PVCameraInfo camInfo) {
//         this(UUID.randomUUID().toString(), camInfo);
//     }

//     public static class LegacyCameraConfigStruct {
//         PVCameraInfo matchedCameraInfo;

//         /** Legacy constructor for compat with 2024.3.1 */
//         @Json.Creator
//         public LegacyCameraConfigStruct(
//                 String baseName,
//                 String path,
//                 String[] otherPaths,
//                 CameraType cameraType,
//                 int usbVID,
//                 int usbPID) {
//             if (cameraType == CameraType.UsbCamera) {
//                 this.matchedCameraInfo =
//                         PVCameraInfo.fromUsbCameraInfo(
//                                 new UsbCameraInfo(-1, path, baseName, otherPaths, usbVID,
// usbPID));
//             } else if (cameraType == CameraType.ZeroCopyPicam) {
//                 this.matchedCameraInfo = PVCameraInfo.fromCSICameraInfo(path, baseName);
//             } else {
//                 // wtf
//                 logger.error("Camera type is invalid");
//                 this.matchedCameraInfo = null;
//                 return;
//             }
//         }
//     }

//     public void addPipelineSettings(List<CVPipelineSettings> settings) {
//         for (var setting : settings) {
//             addPipelineSetting(setting);
//         }
//     }

//     public void addPipelineSetting(CVPipelineSettings setting) {
//         if (pipelineSettings.stream()
//                 .anyMatch(s -> s.pipelineNickname.equalsIgnoreCase(setting.pipelineNickname))) {
//             logger.error("Could not name two pipelines the same thing! Renaming");
//             setting.pipelineNickname += "_1"; // TODO verify this logic
//         }

//         if (pipelineSettings.stream().anyMatch(s -> s.pipelineIndex == setting.pipelineIndex)) {
//             var newIndex = pipelineSettings.size();
//             logger.error("Could not insert two pipelines at same index! Changing to " +
// newIndex);
//             setting.pipelineIndex = newIndex; // TODO verify this logic
//         }

//         pipelineSettings.add(setting);
//         pipelineSettings.sort(PipelineManager.PipelineSettingsIndexComparator);
//     }

//     public void setPipelineSettings(List<CVPipelineSettings> settings) {
//         pipelineSettings = settings;
//     }

//     /**
//      * Replace a calibration in our list with the same resolution with a new one, or add it if
// none
//      * exists yet. If we are replacing an existing calibration, the old one will be "released"
// and the
//      * underlying data matrices will become invalid.
//      *
//      * @param calibration The calibration to add.
//      */
//     public void addCalibration(CameraCalibrationCoefficients calibration) {
//         logger.info("adding calibration " + calibration.resolution);
//         calibrations.stream()
//                 .filter(it -> it.resolution.equals(calibration.resolution))
//                 .findAny()
//                 .ifPresent(
//                         (it) -> {
//                             it.release();
//                             calibrations.remove(it);
//                         });
//         calibrations.add(calibration);
//     }

//     /**
//      * Remove a calibration from our list. If found, the calibration will be "released". If not
// found,
//      * no-op.
//      *
//      * @param resolution The resolution to remove.
//      */
//     public void removeCalibration(Size resolution) {
//         logger.info("deleting calibration " + resolution);
//         calibrations.stream()
//                 .filter(it -> it.resolution.equals(resolution))
//                 .findAny()
//                 .ifPresent(
//                         (it) -> {
//                             it.release();
//                             calibrations.remove(it);
//                         });
//     }

//     /**
//      * cscore will auto-reconnect to the camera path we give it. v4l does not guarantee that if i
// swap
//      * cameras around, the same /dev/videoN ID will be assigned to that camera. So instead
// default to
//      * pinning to a particular USB port, or by "path" (appears to be a global identifier on
// Windows).
//      *
//      * <p>This represents our best guess at an immutable path to detect a camera at.
//      */
//     public String getDevicePath() {
//         return matchedCameraInfo.uniquePath();
//     }

//     public String toShortString() {
//         return "CameraConfiguration [uniqueName="
//                 + uniqueName
//                 + ", matchedCameraInfo="
//                 + matchedCameraInfo
//                 + ", nickname="
//                 + nickname
//                 + ", deactivated="
//                 + deactivated
//                 + ", cameraQuirks="
//                 + cameraQuirks
//                 + ", FOV="
//                 + FOV
//                 + "]";
//     }

//     @Override
//     public String toString() {
//         return "CameraConfiguration [uniqueName="
//                 + uniqueName
//                 + ", matchedCameraInfo="
//                 + matchedCameraInfo
//                 + ", nickname="
//                 + nickname
//                 + ", deactivated="
//                 + deactivated
//                 + ", cameraQuirks="
//                 + cameraQuirks
//                 + ", FOV="
//                 + FOV
//                 + ", calibrations="
//                 + calibrations
//                 + ", currentPipelineIndex="
//                 + currentPipelineIndex
//                 + ", streamIndex="
//                 + streamIndex
//                 + ", pipelineSettings="
//                 + pipelineSettings
//                 + ", driveModeSettings="
//                 + driveModeSettings
//                 + "]";
//     }

//     /**
//      * UICameraConfiguration has some stuff particular to VisionModule, but enough of it's common
// to
//      * warrant this helper
//      */
//     public UICameraConfiguration toUiConfig() {
//         var ret = new UICameraConfiguration();

//         ret.matchedCameraInfo = matchedCameraInfo;
//         ret.cameraPath = getDevicePath();
//         ret.nickname = nickname;
//         ret.uniqueName = uniqueName;
//         ret.deactivated = deactivated;
//         ret.isCSICamera = matchedCameraInfo.type() == CameraType.ZeroCopyPicam;
//         ret.pipelineNicknames = pipelineSettings.stream().map(it ->
// it.pipelineNickname).toList();
//         ret.cameraQuirks = cameraQuirks;
//         ret.calibrations =
//
// calibrations.stream().map(CameraCalibrationCoefficients::cloneWithoutObservations).toList();

//         return ret;
//     }
// }
// @Json
// class DriverModePipelineSettings extends CVPipelineSettings {
//     public DoubleCouple offsetPoint = new DoubleCouple();
//     public boolean crosshair = true;

//     public DriverModePipelineSettings() {
//         super();
//         pipelineNickname = "Driver Mode";
//         pipelineIndex = PipelineManager.DRIVERMODE_INDEX;
//         pipelineType = PipelineType.DriverMode;
//         inputShouldShow = true;
//         cameraAutoExposure = true;
//     }
// }
