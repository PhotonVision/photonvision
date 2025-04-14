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
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.JacksonUtils;
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
    private List<VisionModuleChange<?>> settingChanges = new ArrayList<>();
    private final ReentrantLock changeListLock = new ReentrantLock();

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
        if (event instanceof IncomingWebSocketEvent wsEvent) {
            // Camera index -1 means a "multicast event" (i.e. the event is received by all cameras)
            if (wsEvent.cameraUniqueName != null
                    && wsEvent.cameraUniqueName.equals(parentModule.uniqueName())) {
                logger.trace("Got PSC event - propName: " + wsEvent.propertyName);
                changeListLock.lock();
                try {
                    getSettingChanges()
                            .add(
                                    new VisionModuleChange(
                                            wsEvent.propertyName,
                                            wsEvent.data,
                                            parentModule.pipelineManager.getCurrentPipeline().getSettings(),
                                            wsEvent.originContext));
                } finally {
                    changeListLock.unlock();
                }
            }
        }
    }

    public List<VisionModuleChange<?>> getSettingChanges() {
        return settingChanges;
    }

    public void processSettingChanges() {
        // special case for non-PipelineSetting changes
        changeListLock.lock();
        try {
            for (var change : settingChanges) {
                var propName = change.getPropName();
                var newPropValue = change.getNewPropValue();
                var currentSettings = change.getCurrentSettings();
                var originContext = change.getOriginContext();
                boolean handled = true;
                switch (propName) {
                    case "pipelineName" -> {
                        newPipelineNickname((String) newPropValue);
                        continue;
                    }
                    case "newPipelineInfo" -> newPipelineInfo((Pair<String, PipelineType>) newPropValue);
                    case "deleteCurrPipeline" -> deleteCurrPipeline();
                    case "changePipeline" -> changePipeline((Integer) newPropValue);
                    case "startCalibration" -> startCalibration((Map<String, Object>) newPropValue);
                    case "saveInputSnapshot" -> parentModule.saveInputSnapshot();
                    case "saveOutputSnapshot" -> parentModule.saveOutputSnapshot();
                    case "takeCalSnapshot" -> parentModule.takeCalibrationSnapshot();
                    case "duplicatePipeline" -> duplicatePipeline((Integer) newPropValue);
                    case "calibrationUploaded" -> {
                        if (newPropValue instanceof CameraCalibrationCoefficients newCal) {
                            parentModule.addCalibrationToConfig(newCal);
                        } else {
                            logger.warn("Received invalid calibration data");
                        }
                    }
                    case "robotOffsetPoint" -> {
                        if (currentSettings instanceof AdvancedPipelineSettings curAdvSettings) {
                            robotOffsetPoint(curAdvSettings, (Integer) newPropValue);
                        }
                    }
                    case "changePipelineType" -> {
                        parentModule.changePipelineType((Integer) newPropValue);
                        parentModule.saveAndBroadcastAll();
                    }
                    case "isDriverMode" -> parentModule.setDriverMode((Boolean) newPropValue);
                    default -> handled = false;
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

                if (!handled) {
                    try {
                        setProperty(currentSettings, propName, newPropValue);
                        logger.trace("Set prop " + propName + " to value " + newPropValue);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        logger.error(
                                "Could not set prop "
                                        + propName
                                        + " with value "
                                        + newPropValue
                                        + " on "
                                        + currentSettings
                                        + " | "
                                        + e.getClass().getSimpleName(),
                                e);
                    } catch (Exception e) {
                        logger.error("Unknown exception when setting PSC prop!", e);
                    }
                }

                parentModule.saveAndBroadcastSelective(originContext, propName, newPropValue);
            }
            getSettingChanges().clear();
        } finally {
            changeListLock.unlock();
        }
    }

    public void newPipelineNickname(String newNickname) {
        logger.info("Changing pipeline nickname to " + newNickname);
        parentModule.pipelineManager.getCurrentPipelineSettings().pipelineNickname = newNickname;
        parentModule.saveAndBroadcastAll();
    }

    public void newPipelineInfo(Pair<String, PipelineType> typeName) {
        var type = typeName.getRight();
        var name = typeName.getLeft();

        logger.info("Adding a " + type + " pipeline with name " + name);

        var addedSettings = parentModule.pipelineManager.addPipeline(type);
        addedSettings.pipelineNickname = name;
        parentModule.saveAndBroadcastAll();
    }

    public void deleteCurrPipeline() {
        var indexToDelete = parentModule.pipelineManager.getRequestedIndex();
        logger.info("Deleting current pipe at index " + indexToDelete);
        int newIndex = parentModule.pipelineManager.removePipeline(indexToDelete);
        parentModule.setPipeline(newIndex);
        parentModule.saveAndBroadcastAll();
    }

    public void changePipeline(int index) {
        if (index == parentModule.pipelineManager.getRequestedIndex()) {
            logger.debug("Skipping pipeline change, index " + index + " already active");
            return;
        }
        parentModule.setPipeline(index);
        parentModule.saveAndBroadcastAll();
    }

    public void startCalibration(Map<String, Object> data) {
        try {
            var deserialized = JacksonUtils.deserialize(data, UICalibrationData.class);
            parentModule.startCalibration(deserialized);
            parentModule.saveAndBroadcastAll();
        } catch (Exception e) {
            logger.error("Error deserializing start-calibration request", e);
        }
    }

    public void duplicatePipeline(int index) {
        var newIndex = parentModule.pipelineManager.duplicatePipeline(index);
        parentModule.setPipeline(newIndex);
        parentModule.saveAndBroadcastAll();
    }

    public void robotOffsetPoint(AdvancedPipelineSettings curAdvSettings, int offsetIndex) {
        RobotOffsetPointOperation offsetOperation = RobotOffsetPointOperation.fromIndex(offsetIndex);

        var latestTarget = parentModule.lastPipelineResultBestTarget;
        if (latestTarget == null) {
            return;
        }

        var newPoint = latestTarget.getTargetOffsetPoint();
        switch (curAdvSettings.offsetRobotOffsetMode) {
            case Single -> {
                switch (offsetOperation) {
                    case CLEAR -> curAdvSettings.offsetSinglePoint = new Point();
                    case TAKE_SINGLE -> curAdvSettings.offsetSinglePoint = newPoint;
                    case TAKE_FIRST_DUAL, TAKE_SECOND_DUAL -> {
                        logger.warn("Dual point operation in single point mode");
                    }
                }
            }
            case Dual -> {
                switch (offsetOperation) {
                    case CLEAR -> {
                        curAdvSettings.offsetDualPointA = new Point();
                        curAdvSettings.offsetDualPointAArea = 0;
                        curAdvSettings.offsetDualPointB = new Point();
                        curAdvSettings.offsetDualPointBArea = 0;
                    }
                    case TAKE_FIRST_DUAL -> {
                        // update point and area
                        curAdvSettings.offsetDualPointA = newPoint;
                        curAdvSettings.offsetDualPointAArea = latestTarget.getArea();
                    }
                    case TAKE_SECOND_DUAL -> {
                        // update point and area
                        curAdvSettings.offsetDualPointB = newPoint;
                        curAdvSettings.offsetDualPointBArea = latestTarget.getArea();
                    }
                    case TAKE_SINGLE -> {
                        logger.warn("Single point operation in dual point mode");
                    }
                }
            }
            case None -> {
                logger.warn("Robot offset point operation requested, but no offset mode set");
            }
        }
    }

    /**
     * Sets the value of a property in the given object using reflection. This method should not be
     * used generally and is only known to be correct in the context of `onDataChangeEvent`.
     *
     * @param currentSettings The object whose property needs to be set.
     * @param propName The name of the property to be set.
     * @param newPropValue The new value to be assigned to the property.
     * @throws IllegalAccessException If the field cannot be accessed.
     * @throws NoSuchFieldException If the field does not exist.
     * @throws Exception If an some other unknown exception occurs while setting the property.
     */
    protected static void setProperty(Object currentSettings, String propName, Object newPropValue)
            throws IllegalAccessException, NoSuchFieldException, Exception {
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
            if (newPropValue instanceof Integer intValue) {
                propField.setBoolean(currentSettings, intValue != 0);
            } else {
                propField.setBoolean(currentSettings, (Boolean) newPropValue);
            }
        } else {
            propField.set(currentSettings, newPropValue);
        }
    }
}
