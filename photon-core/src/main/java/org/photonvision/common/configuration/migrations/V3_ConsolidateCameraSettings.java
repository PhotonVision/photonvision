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
    void update(Connection conn) throws SQLException {

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
            try {

                // JsonType<List<String>> strListJsonb = Jsonb.instance().type(Types.listOf(String.class));
                String uniqueName = cameraData.get("unique_name");
                DynamicJsonEditor configJson;

                try {
                    configJson = new DynamicJsonEditor(cameraData.get("config_json"));
                } catch (IOException e) {
                    logger.error("Can't deserialize config_json", e);
                    return;
                }
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
                    // String otherPaths = String.format("{ \"otherPaths\": %s }",
                    // cameraData.get("otherpaths_json"));
                    try (var otherPathsJson = new DynamicJsonEditor(cameraData.get("otherpaths_json"))) {
                        matchedCameraInfo.put("otherPaths", otherPathsJson.getList(""));
                    } catch (IOException e) {
                        logger.warn("Couldn't deserialize otherpaths_json. Skipping.");
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
                    } catch (IOException e) {
                        logger.warn("Couldn't deserialize pipeline_jsons. Skipping.");
                    }
                }

                if (!configJson.hasKey("driveModeSettings")) {
                    try (var legacyDriverModeJson =
                            new DynamicJsonEditor(cameraData.get("drivermode_json"))) {
                        var oldWrapperIterator = legacyDriverModeJson.getList("").iterator();
                        String type = (String) oldWrapperIterator.next();
                        @SuppressWarnings("unchecked")
                        var pipeline = (Map<String, Object>) oldWrapperIterator.next();
                        configJson.getMap("").put("driveModeSettings", pipeline);
                    } catch (IOException e) {
                        logger.warn("Couldn't deserialize drivermode_json. Skipping.");
                    }
                }

                // // MIGRATION: 2026
                // var cameraInfoMatcher = cameraInfoPattern.matcher(configJson);
                // if (cameraInfoMatcher.find() && cameraInfoMatcher.group(2).equals(":")) {
                //     logger.debug("Legacy type-wrapper PVCameraInfo being migrated");
                //     configJson = remapConfigJson(configJson, cameraInfoMatcher.group(1));
                // }

                // logger.debug(configJson.toString());

                // Jsonb jsonb = Jsonb.builder().build();

                // try (JsonReader reader = jsonb.reader(cameraData.get("config_json"))) {
                //     reader.beginObject();
                //     while (reader.hasNextField()) {
                //         String fieldname = reader.nextField();

                //         JsonReader.Token tokenType = reader.currentToken();

                //         logger.debug("Field: " + fieldname + " <" + tokenType.toString() + ">");

                //         reader.skipValue();
                //     }
                //     reader.endObject();
                // }

                // // CameraConfiguration config =
                // //         Jsonb.instance().type(CameraConfiguration.class).fromJson(configJson);

                // // // MIGRATION: 2024
                // // if (config.matchedCameraInfo == null) {
                // //     logger.debug("Legacy CameraConfiguration detected - upgrading");

                // //     // manually create the matchedCameraInfo ourselves. Need to upgrade:
                // //     // baseName, path, otherPaths, cameraType, usbvid/pid -> matchedCameraInfo
                // //     config.matchedCameraInfo =
                // //             Jsonb.instance()
                // //                     .type(LegacyCameraConfigStruct.class)
                // //                     .fromJson(configJson)
                // //                     .matchedCameraInfo;

                // //     // Except that otherPaths used to be its own column. so hack that in here as well
                // //     var otherPaths =
                // //             Jsonb.instance()
                // //                     .type(String[].class)
                // //                     .fromJson(cameraData.get("otherpaths_json"));
                // //     if (config.matchedCameraInfo instanceof UsbCameraInfo usbInfo) {
                // //         usbInfo.otherPaths = otherPaths;
                // //     }
                // // }

                // // MIGRATION: 2026
                // List<String> legacyPipelineSettings =
                //         strListJsonb.fromJson(cameraData.get("pipeline_jsons"));

                // List<Object> pipelineList = new ArrayList<Object>();

                // JsonType<List<Object>> objListJsonb = Jsonb.instance().type(Types.listOf(Object.class));

                // for (var pipelineJson : legacyPipelineSettings) {
                //     logger.debug("Importing pipeline JSON into camera settings");
                //     if (pipelineJson.startsWith("[")) {
                //         logger.debug("Legacy type-wrapper CVPipelineSettings being migrated");
                //         pipelineJson = remapSettingsJson(pipelineJson);
                //     }

                //     pipelineList.add(pipelineJson);
                //     // try {
                //     //     config.pipelineSettings.add(
                //     //
                // Jsonb.instance().type(CVPipelineSettings.class).fromJson(pipelineJson));
                //     // } catch (IllegalStateException | JsonException e) {
                //     //     logger.error("Could not deserialize pipeline setting for camera " +
                // config.nickname, e);
                //     // }
                // }

                // JsonType<Map<String, Object>> objMapJsonb =
                // Jsonb.instance().type(Types.mapOf(Object.class));
                // Map<String, Object> configMap = objMapJsonb.fromJson(configJson);
                // // configMap.put("pipelineSettings", objListJsonb.toJson(pipelineList));
                // logger.debug(objMapJsonb.toJson(configMap));

                // // MIGRATION: 2026
                // logger.debug("Importing driver mode JSON into camera settings");
                // var driverModeJson = cameraData.get("drivermode_json");
                // if (driverModeJson.startsWith("[")) {
                //     logger.debug("Legacy type-wrapper CVPipelineSettings being migrated");
                //     driverModeJson = remapSettingsJson(driverModeJson);
                // }

                // configMap.put("driveModeSettings", driverModeJson);

                // logger.debug(configMap.toString());

                // loadedConfigurations.put(uniqueName, config);
                // update camera in database
                try {
                    logger.debug("Migrated config_json:\n" + configJson.export(true));
                } catch (IOException e) {
                    logger.error("Error serializing configJson.", e);
                }
                var sqlString =
                        "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES (?, ?, ?, ?);";
                var statement = conn.prepareStatement(sqlString);
                try {
                    statement.setString(1, uniqueName);
                    statement.setString(2, configJson.export(false));
                    statement.setString(3, "null");
                    statement.setString(4, "[]");
                    statement.executeUpdate();
                } catch (IOException e) {
                    logger.error("Error deserializing configJson.", e);
                } finally {
                    statement.close();
                    configJson.close();
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

    public static String remapSettingsJson(String pipelineJson) {
        final JsonType<List<Object>> objListJsonb = Jsonb.instance().type(Types.listOf(Object.class));
        final JsonType<Map<String, Object>> objMapJsonb =
                Jsonb.instance().type(Types.mapOf(Object.class));

        List<Object> pipelineMigrationIn = objListJsonb.fromJson(pipelineJson);

        @SuppressWarnings("unchecked")
        var pipelineData = (Map<String, Object>) pipelineMigrationIn.get(1);

        Map<String, Object> pipelineMigrationOut = new HashMap<>();
        pipelineMigrationOut.putAll(pipelineData);
        pipelineMigrationOut.put("type", pipelineMigrationIn.get(0));

        return objMapJsonb.toJson(pipelineMigrationOut);
    }
}

// @Json
// class CameraConfiguration {
//     public String uniqueName = "";
//     public PVCameraInfo matchedCameraInfo;
//     public String nickname = "";
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
//     // of the parameters)
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

// calibrations.stream().map(CameraCalibrationCoefficients::cloneWithoutObservations).toList();

//         return ret;
//     }
// }

// enum CameraType {
//     UsbCamera,
//     ZeroCopyPicam,
//     FileCamera // special case for File-based vision sources
// }

// @Json(typeProperty = "type")
// @Json.SubType(type = PVCameraInfo.PVUsbCameraInfo.class)
// @Json.SubType(type = PVCameraInfo.PVCSICameraInfo.class)
// @Json.SubType(type = PVCameraInfo.PVFileCameraInfo.class)
// sealed interface PVCameraInfo {

//     @Json.Property("path")
//     String path();

//     @Json.Property("name")
//     String name();

//     @Json.Property("uniquePath")
//     String uniquePath();

//     String[] otherPaths();

//     CameraType type();

//     static final class PVUsbCameraInfo extends UsbCameraInfo implements PVCameraInfo {
//         public PVUsbCameraInfo(
//                 int dev, String path, String name, String[] otherPaths, int vendorId, int
// productId) {
//             super(dev, path, name, otherPaths, vendorId, productId);
//         }

//         private PVUsbCameraInfo(UsbCameraInfo info) {
//             super(info.dev, info.path, info.name, info.otherPaths, info.vendorId,
// info.productId);
//         }

//         @Override
//         public String path() {
//             return super.path;
//         }

//         @Override
//         public String name() {
//             return super.name.replaceAll("[^\\x00-\\x7F]", "");
//         }

//         @Override
//         public String uniquePath() {
//             return Arrays.stream(super.otherPaths)
//                     .sorted() // Must sort to ensure a consistent unique path as we can get more
// than one
//                     // by-path and their order changes at random?
//                     .filter(path -> path.contains("/by-path/"))
//                     .findFirst()
//                     .orElse(path());
//         }

//         @Override
//         public String[] otherPaths() {
//             return super.otherPaths;
//         }

//         @Override
//         public CameraType type() {
//             return CameraType.UsbCamera;
//         }
//     }

//     public static final class PVCSICameraInfo implements PVCameraInfo {
//         public final String path;
//         public final String baseName;

//         public PVCSICameraInfo(String path, String baseName) {
//             this.path = path;
//             this.baseName = baseName;
//         }

//         @Override
//         public String path() {
//             return path;
//         }

//         @Override
//         public String name() {
//             return baseName.replaceAll("[^\\x00-\\x7F]", "");
//         }

//         @Override
//         public String uniquePath() {
//             return path();
//         }

//         @Override
//         public String[] otherPaths() {
//             return new String[0];
//         }

//         @Override
//         public CameraType type() {
//             return CameraType.ZeroCopyPicam;
//         }
//     }

//     public static final class PVFileCameraInfo implements PVCameraInfo {
//         public final String path;
//         public final String name;

//         public PVFileCameraInfo(String path, String name) {
//             this.path = path;
//             this.name = name;
//         }

//         @Override
//         public String path() {
//             return path;
//         }

//         @Override
//         public String name() {
//             return name;
//         }

//         @Override
//         public String uniquePath() {
//             return path();
//         }

//         @Override
//         public String[] otherPaths() {
//             return new String[0];
//         }

//         @Override
//         public CameraType type() {
//             return CameraType.FileCamera;
//         }

//     }

//     public static PVCameraInfo fromUsbCameraInfo(UsbCameraInfo info) {
//         return new PVUsbCameraInfo(info);
//     }

//     public static PVCameraInfo fromCSICameraInfo(String path, String baseName) {
//         return new PVCSICameraInfo(path, baseName);
//     }

//     public static PVCameraInfo fromFileInfo(String path, String baseName) {
//         return new PVFileCameraInfo(path, baseName);
//     }
// }

// class UsbCameraInfo {
//   @SuppressWarnings("PMD.ArrayIsStoredDirectly")
//   public UsbCameraInfo(
//       int dev, String path, String name, String[] otherPaths, int vendorId, int productId) {
//     this.dev = dev;
//     this.path = path;
//     this.name = name;
//     this.otherPaths = otherPaths;
//     this.vendorId = vendorId;
//     this.productId = productId;
//   }

//   public int dev;

//   public String path;

//   public String name;

//   public String[] otherPaths;
//   public int vendorId;
//   public int productId;
// }

// @Json
// class QuirkyCamera {
//     public final String baseName;
//     public final int usbVid;
//     public final int usbPid;
//     public final String displayName;
//     public final Map<CameraQuirk, Boolean> quirks;

//     public QuirkyCamera(
//             String baseName,
//             int usbVid,
//             int usbPid,
//             String displayName,
//             Map<CameraQuirk, Boolean> quirks) {
//         this.baseName = baseName;
//         this.usbPid = usbPid;
//         this.usbVid = usbVid;
//         this.quirks = quirks;
//         this.displayName = displayName;
//     }
// }

// enum CameraQuirk {
//     Gain,
//     LifeCamControls,
//     PsEyeControls,
//     FPSCap100,
//     AwbRedBlueGain,
//     CompletelyBroken,
//     AdjustableFocus,
//     StickyFPS,
//     ArduCamCamera,
//     ArduOV9281Controls,
//     ArduOV2311Controls,
//     ArduOV9782Controls,
//     InnoOV9281Controls,
//     ArduOV9782,
//     See3Cam_24CUG,
// }

// @Json
// class CameraCalibrationCoefficients implements Releasable {
//     /** The unrotated resolution of the calibration */
//     public final Size resolution;

//     public final JsonMatOfDouble cameraIntrinsics;

//     public final JsonMatOfDouble distCoeffs;

//     public final List<BoardObservation> observations;

//     public final double[] calobjectWarp;

//     public final Size calobjectSize;

//     public final double calobjectSpacing;

//     public final CameraLensModel lensmodel;

//     /**
//      * Contains all camera calibration data for a particular resolution of a camera. Designed for
// use
//      * with standard opencv camera calibration matrices. For details on the layout of camera
//      * intrinsics/distortion matrices, see:
//      * https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html#ga3207604e4b1a1758aa66acb6ed5aa65d
//      *
//      * @param resolution The resolution this applies to. We don't assume camera binning or try
//      *     rescaling calibration
//      * @param cameraIntrinsics Camera intrinsics parameters matrix, in the standard opencv form.
//      * @param distCoeffs Camera distortion coefficients array. Variable length depending on order
// of
//      *     distortion model
//      * @param calobjectWarp Board deformation parameters, for calibrators that can estimate that.
// See:
//      *     https://mrcal.secretsauce.net/formulation.html#board-deformation
//      * @param observations List of snapshots used to construct this calibration
//      * @param calobjectSize Dimensions of the object used to calibrate, in # of squares in
//      *     width/height
//      * @param calobjectSpacing Spacing between adjacent squares, in meters
//      */
//     public CameraCalibrationCoefficients(
//             Size resolution,
//             JsonMatOfDouble cameraIntrinsics,
//             JsonMatOfDouble distCoeffs,
//             double[] calobjectWarp,
//             List<BoardObservation> observations,
//             Size calobjectSize,
//             double calobjectSpacing,
//             CameraLensModel lensmodel) {
//         this.resolution = resolution;
//         this.cameraIntrinsics = cameraIntrinsics;
//         this.distCoeffs = distCoeffs;
//         this.calobjectWarp = calobjectWarp;
//         this.calobjectSize = calobjectSize;
//         this.calobjectSpacing = calobjectSpacing;
//         this.lensmodel = lensmodel;

//         // Legacy migration just to make sure that observations is at worst empty and never null
//         if (observations == null) {
//             observations = List.of();
//         }
//         this.observations = observations;
//     }

//     public CameraCalibrationCoefficients rotateCoefficients(ImageRotationMode rotation) {
//         if (rotation == ImageRotationMode.DEG_0) {
//             return this;
//         }
//         Mat rotatedIntrinsics = getCameraIntrinsicsMat().clone();
//         Mat rotatedDistCoeffs = getDistCoeffsMat().clone();
//         double cx = getCameraIntrinsicsMat().get(0, 2)[0];
//         double cy = getCameraIntrinsicsMat().get(1, 2)[0];
//         double fx = getCameraIntrinsicsMat().get(0, 0)[0];
//         double fy = getCameraIntrinsicsMat().get(1, 1)[0];

//         // only adjust p1 and p2 the rest are radial distortion coefficients

//         double p1 = getDistCoeffsMat().get(0, 2)[0];
//         double p2 = getDistCoeffsMat().get(0, 3)[0];

//         Size rotatedImageSize = null;

//         // A bunch of horrifying opaque rotation black magic. See image-rotation.md for more
// details.
//         switch (rotation) {
//             case DEG_0:
//                 break;
//             case DEG_270_CCW:
//                 // FX
//                 rotatedIntrinsics.put(0, 0, fy);
//                 // FY
//                 rotatedIntrinsics.put(1, 1, fx);

//                 // CX
//                 rotatedIntrinsics.put(0, 2, resolution.height - cy);
//                 // CY
//                 rotatedIntrinsics.put(1, 2, cx);

//                 // P1
//                 rotatedDistCoeffs.put(0, 2, p2);
//                 // P2
//                 rotatedDistCoeffs.put(0, 3, -p1);

//                 // The rotated image size is the same as the unrotated image size, but the width
// and height
//                 // are swapped
//                 rotatedImageSize = new Size(resolution.height, resolution.width);
//                 break;
//             case DEG_180_CCW:
//                 // CX
//                 rotatedIntrinsics.put(0, 2, resolution.width - cx);
//                 // CY
//                 rotatedIntrinsics.put(1, 2, resolution.height - cy);

//                 // P1
//                 rotatedDistCoeffs.put(0, 2, -p1);
//                 // P2
//                 rotatedDistCoeffs.put(0, 3, -p2);

//                 // The rotated image size is the same as the unrotated image size
//                 rotatedImageSize = resolution;
//                 break;
//             case DEG_90_CCW:
//                 // FX
//                 rotatedIntrinsics.put(0, 0, fy);
//                 // FY
//                 rotatedIntrinsics.put(1, 1, fx);

//                 // CX
//                 rotatedIntrinsics.put(0, 2, cy);
//                 // CY
//                 rotatedIntrinsics.put(1, 2, resolution.width - cx);

//                 // P1
//                 rotatedDistCoeffs.put(0, 2, -p2);
//                 // P2
//                 rotatedDistCoeffs.put(0, 3, p1);

//                 // The rotated image size is the same as the unrotated image size, but the width
// and height
//                 // are swapped
//                 rotatedImageSize = new Size(resolution.height, resolution.width);
//                 break;
//         }

//         JsonMatOfDouble newIntrinsics = JsonMatOfDouble.fromMat(rotatedIntrinsics);

//         JsonMatOfDouble newDistCoeffs = JsonMatOfDouble.fromMat(rotatedDistCoeffs);

//         rotatedIntrinsics.release();
//         rotatedDistCoeffs.release();

//         return new CameraCalibrationCoefficients(
//                 rotatedImageSize,
//                 newIntrinsics,
//                 newDistCoeffs,
//                 calobjectWarp,
//                 observations,
//                 calobjectSize,
//                 calobjectSpacing,
//                 lensmodel);
//     }

//     public Mat getCameraIntrinsicsMat() {
//         return cameraIntrinsics.getAsMatOfDouble();
//     }

//     public MatOfDouble getDistCoeffsMat() {
//         return distCoeffs.getAsMatOfDouble();
//     }

//     public double[] getIntrinsicsArr() {
//         return cameraIntrinsics.data;
//     }

//     public double[] getDistCoeffsArr() {
//         return distCoeffs.data;
//     }

//     public List<BoardObservation> getObservations() {
//         return observations;
//     }

//     @Override
//     public void release() {
//         cameraIntrinsics.release();
//         distCoeffs.release();
//     }

//     @Override
//     public String toString() {
//         return "CameraCalibrationCoefficients [resolution="
//                 + resolution
//                 + ", cameraIntrinsics="
//                 + cameraIntrinsics
//                 + ", distCoeffs="
//                 + distCoeffs
//                 + ", observationslen="
//                 + observations.size()
//                 + ", calobjectWarp="
//                 + Arrays.toString(calobjectWarp)
//                 + "]";
//     }

//     public UICameraCalibrationCoefficients cloneWithoutObservations() {
//         return new UICameraCalibrationCoefficients(
//                 resolution,
//                 cameraIntrinsics,
//                 distCoeffs,
//                 calobjectWarp,
//                 observations,
//                 calobjectSize,
//                 calobjectSpacing,
//                 lensmodel);
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
