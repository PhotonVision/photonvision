/*
 * Copyright (C) 2020 Photon Vision.
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
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.PipelineType;
import org.photonvision.vision.pipeline.UICalibrationData;
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
        if (event instanceof IncomingWebSocketEvent) {
            var wsEvent = (IncomingWebSocketEvent<?>) event;

            if (wsEvent.cameraIndex != null && wsEvent.cameraIndex == parentModule.moduleIndex) {
                logger.trace("Got PSC event - propName: " + wsEvent.propertyName);

                var propName = wsEvent.propertyName;
                var newPropValue = wsEvent.data;
                var currentSettings = parentModule.pipelineManager.getCurrentUserPipeline().getSettings();

                // special case for non-PipelineSetting changes
                switch (propName) {
                    case "cameraNickname": // rename camera
                        var newNickname = (String) newPropValue;
                        logger.info("Changing nickname to " + newNickname);
                        parentModule.setCameraNickname(newNickname);
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "pipelineName": // rename current pipeline
                        logger.info("Changing nick to " + newPropValue);
                        parentModule.pipelineManager.getCurrentPipelineSettings().pipelineNickname =
                                (String) newPropValue;
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "newPipelineInfo": // add new pipeline
                        var typeName = (Pair<String, PipelineType>) newPropValue;
                        var type = typeName.getRight();
                        var name = typeName.getLeft();

                        logger.info("Adding a " + type + " pipeline with name " + name);

                        var addedSettings = parentModule.pipelineManager.addPipeline(type);
                        addedSettings.pipelineNickname = name;
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "deleteCurrPipeline":
                        var indexToDelete = parentModule.pipelineManager.getCurrentPipelineIndex();
                        logger.info("Deleting current pipe at index " + indexToDelete);
                        parentModule.pipelineManager.removePipeline(indexToDelete);
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "changePipeline": // change active pipeline
                        var index = (Integer) newPropValue;
                        if (index == parentModule.pipelineManager.getCurrentPipelineIndex()) {
                            logger.debug("Skipping pipeline change, index " + index + " already active");
                            return;
                        }
                        parentModule.setPipeline(index);
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "dimLED":
                        if (parentModule.cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                            var dimPercentage = (int) newPropValue;
                            HardwareManager.getInstance().setBrightnessPercentage(dimPercentage);
                        }
                        return;
                    case "blinkLED":
                        if (parentModule.cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                            var params = (Pair<Integer, Integer>) newPropValue;
                            HardwareManager.getInstance().blinkLEDs(params.getLeft(), params.getRight());
                        }
                        return;
                    case "setLED":
                        if (parentModule.cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                            var state = (boolean) newPropValue;
                            if (state) HardwareManager.getInstance().turnLEDsOn();
                            else HardwareManager.getInstance().turnLEDsOff();
                        }
                        return;
                    case "toggleLED":
                        if (parentModule.cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                            HardwareManager.getInstance().toggleLEDs();
                        }
                        return;
                    case "shutdownLEDs":
                        if (parentModule.cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
                            HardwareManager.getInstance().shutdown();
                        }
                        return;
                    case "startcalibration":
                        var data = UICalibrationData.fromMap((Map<String, Object>) newPropValue);
                        parentModule.startCalibration(data);
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "takeCalSnapshot":
                        parentModule.takeCalibrationSnapshot();
                        return;
                    case "robotOffsetPoint":
                        if (currentSettings instanceof AdvancedPipelineSettings) {
                            var curAdvSettings = (AdvancedPipelineSettings) currentSettings;
                            var offsetOperation = RobotOffsetPointOperation.fromIndex((int)newPropValue);

                            Point latestBestTargetPoint = new Point(); // todo: get from last pipeline result

                            switch (curAdvSettings.offsetRobotOffsetMode) {
                                case Single:
                                    if (offsetOperation == RobotOffsetPointOperation.ROPO_CLEAR) {
                                        curAdvSettings.offsetCalibrationPoint = new DoubleCouple();
                                    } else if (offsetOperation == RobotOffsetPointOperation.ROPO_TAKESINGLE) {
                                        curAdvSettings.offsetCalibrationPoint = new DoubleCouple(latestBestTargetPoint.x, latestBestTargetPoint.y);
                                    }
                                    break;
                                case Dual:
                                        var firstPoint = parentModule.dualOffsetPoints.getLeft();
                                        var secondPoint = parentModule.dualOffsetPoints.getRight();

                                        if (offsetOperation == RobotOffsetPointOperation.ROPO_CLEAR) {
                                            curAdvSettings.offsetDualLineM = 0;
                                            curAdvSettings.offsetDualLineB = 0;
                                        } else {
                                            // update point
                                            switch (offsetOperation) {
                                                case ROPO_TAKEFIRSTDUAL:
                                                    firstPoint.x = latestBestTargetPoint.x;
                                                    firstPoint.y = latestBestTargetPoint.y;
                                                    break;
                                                case ROPO_TAKESECONDDUAL:
                                                    secondPoint.x = latestBestTargetPoint.x;
                                                    secondPoint.y = latestBestTargetPoint.y;
                                                    break;
                                            }

                                            // update line if either point is updated
                                            if (offsetOperation == RobotOffsetPointOperation.ROPO_TAKEFIRSTDUAL || offsetOperation ==RobotOffsetPointOperation.ROPO_TAKESECONDDUAL) {
                                                var offsetLineSlope = (secondPoint.y - firstPoint.y) / (secondPoint.x - firstPoint.x);
                                                var offsetLineIntercept = firstPoint.y - (offsetLineSlope * firstPoint.x);
                                                curAdvSettings.offsetDualLineM = offsetLineSlope;
                                                curAdvSettings.offsetDualLineB = offsetLineIntercept;
                                            }
                                    }
                                    break;
                            }
                        }
                        return;
                }

                // special case for camera settables
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

                try {
                    var propField = currentSettings.getClass().getField(propName);
                    var propType = propField.getType();

                    if (propType.isEnum()) {
                        var actual = propType.getEnumConstants()[(int) newPropValue];
                        propField.set(currentSettings, actual);
                    } else if (propType.isAssignableFrom(DoubleCouple.class)) {
                        var orig = (ArrayList<Number>) newPropValue;
                        var actual = new DoubleCouple(orig.get(0), orig.get(1));
                        propField.set(currentSettings, actual);
                    } else if (propType.isAssignableFrom(IntegerCouple.class)) {
                        var orig = (ArrayList<Integer>) newPropValue;
                        var actual = new IntegerCouple(orig.get(0), orig.get(1));
                        propField.set(currentSettings, actual);
                    } else if (propType.equals(Double.TYPE)) {
                        propField.setDouble(currentSettings, ((Number) newPropValue).doubleValue());
                    } else if (propType.equals(Integer.TYPE)) {
                        propField.setInt(currentSettings, (Integer) newPropValue);
                    } else if (propType.equals(Boolean.TYPE)) {
                        if (newPropValue instanceof Integer) {
                            propField.setBoolean(currentSettings, (Integer) newPropValue != 0);
                        } else {
                            propField.setBoolean(currentSettings, (Boolean) newPropValue);
                        }
                    } else {
                        propField.set(newPropValue, newPropValue);
                    }
                    logger.trace("Set prop " + propName + " to value " + newPropValue);

                    // special case for extra tasks to perform after setting PipelineSettings
                    if (propName.equals("streamingFrameDivisor")) {
                        parentModule.dashboardInputStreamer.setFrameDivisor(
                                parentModule.pipelineManager.getCurrentPipelineSettings().streamingFrameDivisor);
                        parentModule.dashboardOutputStreamer.setFrameDivisor(
                                parentModule.pipelineManager.getCurrentPipelineSettings().streamingFrameDivisor);
                    }

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.error(
                            "Could not set prop "
                                    + propName
                                    + " with value "
                                    + newPropValue
                                    + " on "
                                    + currentSettings,
                            e);
                } catch (Exception e) {
                    logger.error("Unknown exception when setting PSC prop!", e);
                }

                parentModule.saveAndBroadcastSelective(wsEvent.originContext, propName, newPropValue);
            }
        }
    }
}
