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

            // Camera index -1 means a "multicast event" (i.e. the event is received by all cameras)
            if (wsEvent.cameraIndex != null
                    && (wsEvent.cameraIndex == parentModule.moduleIndex || wsEvent.cameraIndex == -1)) {
                logger.trace("Got PSC event - propName: " + wsEvent.propertyName);

                var propName = wsEvent.propertyName;
                var newPropValue = wsEvent.data;
                var currentSettings = parentModule.pipelineManager.getCurrentPipeline().getSettings();

                // special case for non-PipelineSetting changes
                switch (propName) {
                        //                    case "cameraNickname": // rename camera
                        //                        var newNickname = (String) newPropValue;
                        //                        logger.info("Changing nickname to " + newNickname);
                        //                        parentModule.setCameraNickname(newNickname);
                        //                        return;
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
                        int newIndex = parentModule.pipelineManager.removePipeline(indexToDelete);
                        parentModule.setPipeline(newIndex);
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
                    case "startCalibration":
                        var data = UICalibrationData.fromMap((Map<String, Object>) newPropValue);
                        parentModule.startCalibration(data);
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "takeCalSnapshot":
                        parentModule.takeCalibrationSnapshot();
                        return;
                    case "duplicatePipeline":
                        int idx = parentModule.pipelineManager.duplicatePipeline((Integer) newPropValue);
                        parentModule.setPipeline(idx);
                        parentModule.saveAndBroadcastAll();
                        return;
                    case "calibrationUploaded":
                        if (newPropValue instanceof CameraCalibrationCoefficients)
                            parentModule.addCalibrationToConfig((CameraCalibrationCoefficients) newPropValue);
                        return;
                    case "robotOffsetPoint":
                        if (currentSettings instanceof AdvancedPipelineSettings) {
                            var curAdvSettings = (AdvancedPipelineSettings) currentSettings;
                            var offsetOperation = RobotOffsetPointOperation.fromIndex((int) newPropValue);
                            var latestTarget = parentModule.lastPipelineResultBestTarget;

                            if (latestTarget != null) {
                                var newPoint = latestTarget.getTargetOffsetPoint();

                                switch (curAdvSettings.offsetRobotOffsetMode) {
                                    case Single:
                                        if (offsetOperation == RobotOffsetPointOperation.ROPO_CLEAR) {
                                            curAdvSettings.offsetSinglePoint = new Point();
                                        } else if (offsetOperation == RobotOffsetPointOperation.ROPO_TAKESINGLE) {
                                            curAdvSettings.offsetSinglePoint = newPoint;
                                        }
                                        break;
                                    case Dual:
                                        if (offsetOperation == RobotOffsetPointOperation.ROPO_CLEAR) {
                                            curAdvSettings.offsetDualPointA = new Point();
                                            curAdvSettings.offsetDualPointAArea = 0;
                                            curAdvSettings.offsetDualPointB = new Point();
                                            curAdvSettings.offsetDualPointBArea = 0;
                                        } else {
                                            // update point and area
                                            switch (offsetOperation) {
                                                case ROPO_TAKEFIRSTDUAL:
                                                    curAdvSettings.offsetDualPointA = newPoint;
                                                    curAdvSettings.offsetDualPointAArea = latestTarget.getArea();
                                                    break;
                                                case ROPO_TAKESECONDDUAL:
                                                    curAdvSettings.offsetDualPointB = newPoint;
                                                    curAdvSettings.offsetDualPointBArea = latestTarget.getArea();
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        return;
                    case "changePipelineType":
                        parentModule.changePipelineType((Integer) newPropValue);
                        parentModule.saveAndBroadcastAll();
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
                        var orig = (ArrayList<Number>) newPropValue;
                        var actual = new IntegerCouple(orig.get(0).intValue(), orig.get(1).intValue());
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
