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

package org.photonvision.vision.processes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.PipelineType;
import org.photonvision.vision.target.RobotOffsetPointOperation;

@SuppressWarnings("unchecked")
public class VisionModuleChangeSubscriber extends DataChangeSubscriber {
    private final VisionModule parentModule;
    private final Logger logger;

    public VisionModuleChangeSubscriber(VisionModule parentModule) {
        this.parentModule = parentModule;
        logger =
                new Logger(
                        VisionModuleChangeSubscriber.class,
                        parentModule.visionSource.getSettables().getConfiguration().nickname,
                        LogGroup.VisionModule);
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent<?> event) {
        if (event instanceof IncomingWebSocketEvent<?> wsEvent) {

            // Camera index -1 means a "multicast event" (i.e. the event is received by all cameras)
            if (wsEvent.cameraIndex != null
                    && (wsEvent.cameraIndex == parentModule.moduleIndex || wsEvent.cameraIndex == -1)) {
                logger.trace("Got PSC event - propName: " + wsEvent.propertyName);

                var propName = wsEvent.propertyName;
                var newPropValue = wsEvent.data;
                var currentSettings = parentModule.pipelineManager.getCurrentPipeline().getSettings();

                switch (propName) {
                    case "changeActivePipeline" -> {
                        var newIndex = (Integer) newPropValue;
                        if (newIndex == parentModule.pipelineManager.getRequestedIndex()) {
                            logger.debug("Skipping pipeline change, index " + newIndex + " already active");
                            return;
                        }

                        parentModule.setPipeline(newIndex);
                        parentModule.saveAndBroadcastAll();
                    }
                    case "driverMode" -> {
                        parentModule.setDriverMode((Boolean) newPropValue);
                    }
                    case "changeCameraNickname" -> {
                        var newNickname = (String) newPropValue;
                        parentModule.setCameraNickname(newNickname);
                    }
                    case "changePipelineNickname" -> {
                        var payload = (HashMap<String, Object>) newPropValue;
                        var targetIdx = (Integer) payload.get("pipelineIndex");
                        var nickname = (String) payload.get("nickname");

                        parentModule.pipelineManager.getPipelineSettings(targetIdx).pipelineNickname = nickname;
                    }
                    case "createNewPipeline" -> {
                        var payload = (HashMap<String, Object>) newPropValue;
                        var newPipelineType = PipelineType.getPipelineType((Integer) payload.get("type"));
                        var newPipelineNickname = (String) payload.get("nickname");

                        var addedSettings = parentModule.pipelineManager.addPipeline(newPipelineType);
                        addedSettings.pipelineNickname = newPipelineNickname;
                        parentModule.saveAndBroadcastAll();
                    }
                    case "duplicatePipeline" -> {
                        var payload = (HashMap<String, Object>) newPropValue;
                        var targetIdx = (Integer) payload.get("targetIndex");
                        var newPipelineNickname = (String) payload.get("nickname");
                        var setActive = (Boolean) payload.get("setActive");

                        int newPipeIdx = parentModule.pipelineManager.duplicatePipeline(targetIdx);
                        parentModule.pipelineManager.getPipelineSettings(newPipeIdx).pipelineNickname =
                                newPipelineNickname;

                        if (setActive) {
                            parentModule.setPipeline(newPipeIdx);
                        }

                        parentModule.saveAndBroadcastAll();
                    }
                    case "resetPipeline" -> {
                        var payload = (HashMap<String, Object>) newPropValue;
                        var targetIdx = (Integer) payload.get("pipelineIndex");
                        var newPipelineTypeBase = (Integer) payload.get("type");

                        if (newPipelineTypeBase == null) {
                            // TODO reset pipeline settings to default
                        } else {
                            // TODO change pipeline type by targetIdx
                            var newPipelineType = PipelineType.getPipelineType(newPipelineTypeBase);
                        }

                        parentModule.saveAndBroadcastAll();
                    }
                    case "deletePipeline" -> {
                        var targetIdx = (Integer) newPropValue;
                        int newIndex = parentModule.pipelineManager.removePipeline(targetIdx);
                        // TODO make sure this doesn't change active pipeline unless deleting the active
                        // pipeline
                        parentModule.setPipeline(newIndex);
                        parentModule.saveAndBroadcastAll();
                    }
                    case "startCalib" -> {
                        // TODO send message to session indicating that calib started
                        //                    case "startCalibration":
                        //                        try {
                        //                            var data =
                        //                                    JacksonUtils.deserialize(
                        //                                            (Map<String, Object>) newPropValue,
                        // UICalibrationData.class);
                        //                            parentModule.startCalibration(data);
                        //                            parentModule.saveAndBroadcastAll();
                        //                        } catch (Exception e) {
                        //                            logger.error("Error deserailizing start-cal request", e);
                        //                        }
                        //                        return;
                    }
                    case "takeCalibSnapshot" -> {
                        // TODO make this respect the original context and echo back with status after snapshot
                        parentModule.takeCalibrationSnapshot();
                    }
                    case "cancelCalib" -> {
                        // TODO send message to session indicating that calib was canceled
                    }
                    case "completeCalib" -> {
                        // TODO send message to session indicating that calib was completed successfully or
                        // failed
                    }
                    case "importCalibFromData" -> {
                        var payload = (CameraCalibrationCoefficients) newPropValue;
                        parentModule.addCalibrationToConfig(payload);
                    }
                    case "saveInputSnapshot" -> {
                        parentModule.saveInputSnapshot();
                    }
                    case "saveOutputSnapshot" -> {
                        parentModule.saveOutputSnapshot();
                    }
                    case "robotOffsetPoint" -> {
                        if (currentSettings instanceof AdvancedPipelineSettings curAdvSettings) {
                            var offsetOperation = (RobotOffsetPointOperation) newPropValue;
                            var latestTarget = parentModule.lastPipelineResultBestTarget;

                            if (latestTarget != null) {
                                var newPoint = latestTarget.getTargetOffsetPoint();

                                switch (curAdvSettings.offsetRobotOffsetMode) {
                                    case Single -> {
                                        if (offsetOperation == RobotOffsetPointOperation.ROPO_CLEAR) {
                                            curAdvSettings.offsetSinglePoint = new Point();
                                        } else if (offsetOperation == RobotOffsetPointOperation.ROPO_TAKESINGLE) {
                                            curAdvSettings.offsetSinglePoint = newPoint;
                                        }
                                    }
                                    case Dual -> {
                                        if (offsetOperation == RobotOffsetPointOperation.ROPO_CLEAR) {
                                            curAdvSettings.offsetDualPointA = new Point();
                                            curAdvSettings.offsetDualPointAArea = 0;
                                            curAdvSettings.offsetDualPointB = new Point();
                                            curAdvSettings.offsetDualPointBArea = 0;
                                        } else {
                                            // Update point and area
                                            switch (offsetOperation) {
                                                case ROPO_TAKEFIRSTDUAL -> {
                                                    curAdvSettings.offsetDualPointA = newPoint;
                                                    curAdvSettings.offsetDualPointAArea = latestTarget.getArea();
                                                }
                                                case ROPO_TAKESECONDDUAL -> {
                                                    curAdvSettings.offsetDualPointB = newPoint;
                                                    curAdvSettings.offsetDualPointBArea = latestTarget.getArea();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case "changeCameraFOV" -> {
                        var newFov = (Double) newPropValue;
                        parentModule.setFov(newFov);
                        parentModule.saveModule();
                    }
                    case "changeCameraQuirks" -> {
                        // TODO make sure this serde worked
                        var newQuirks = (HashMap<CameraQuirk, Boolean>) newPropValue;
                        parentModule.changeCameraQuirks(newQuirks);
                        parentModule.saveModule();
                    }
                    case "pipelineSettingChange" -> {
                        var payload = (Pair<Integer, Map.Entry<String, Object>>) newPropValue;
                        var targetPipelineIndex = payload.getLeft();
                        var targetPipeSettings =
                                parentModule.pipelineManager.getPipelineSettings(targetPipelineIndex);

                        var settingsChange = payload.getRight();

                        try {
                            var settingField = targetPipeSettings.getClass().getField(settingsChange.getKey());
                            var settingType = settingField.getType();

                            if (settingType.isEnum()) {
                                var actual = settingType.getEnumConstants()[(int) settingsChange.getValue()];
                                settingField.set(currentSettings, actual);
                            } else if (settingType.isAssignableFrom(DoubleCouple.class)) {
                                var orig = (ArrayList<Number>) settingsChange.getValue();
                                var actual = new DoubleCouple(orig.get(0), orig.get(1));
                                settingField.set(currentSettings, actual);
                            } else if (settingType.isAssignableFrom(IntegerCouple.class)) {
                                var orig = (ArrayList<Number>) settingsChange.getValue();
                                var actual = new IntegerCouple(orig.get(0).intValue(), orig.get(1).intValue());
                                settingField.set(currentSettings, actual);
                            } else if (settingType.equals(Double.TYPE)) {
                                settingField.setDouble(
                                        currentSettings, ((Number) settingsChange.getValue()).doubleValue());
                            } else if (settingType.equals(Integer.TYPE)) {
                                settingField.setInt(currentSettings, (Integer) settingsChange.getValue());
                            } else if (settingType.equals(Boolean.TYPE)) {
                                if (settingsChange.getValue() instanceof Integer) {
                                    settingField.setBoolean(
                                            currentSettings, (Integer) settingsChange.getValue() != 0);
                                } else {
                                    settingField.setBoolean(currentSettings, (Boolean) settingsChange.getValue());
                                }
                            } else {
                                settingField.set(settingsChange.getValue(), settingsChange.getValue());
                            }
                            logger.trace(
                                    "Set prop " + settingsChange.getKey() + " to value " + settingsChange.getValue());
                            parentModule.saveAndBroadcastPipelineChanges(
                                    wsEvent.originContext,
                                    settingsChange.getKey(),
                                    settingsChange.getValue(),
                                    targetPipelineIndex);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            logger.error(
                                    "Could not set prop "
                                            + settingsChange.getKey()
                                            + " with value "
                                            + settingsChange.getValue()
                                            + " on "
                                            + targetPipeSettings,
                                    e);
                        } catch (Exception e) {
                            logger.error("Unknown exception when setting PSC prop!", e);
                        }
                    }
                }

                // Special case for camera settables
                if (propName.startsWith("camera")) {
                    var propMethodName = "set" + propName.replace("camera", "");
                    var methods = parentModule.visionSource.getSettables().getClass().getMethods();
                    for (var method : methods) {
                        if (method.getName().equalsIgnoreCase(propMethodName)) {
                            try {
                                method.invoke(parentModule.visionSource.getSettables(), newPropValue);
                            } catch (Exception e) {
                                logger.error("Failed to invoke camera settable method: " + method.getName(), e);
                            }
                        }
                    }
                }
            }
        }
    }
}
