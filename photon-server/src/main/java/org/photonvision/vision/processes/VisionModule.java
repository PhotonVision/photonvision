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

import java.util.*;

import io.javalin.websocket.WsContext;
import org.apache.commons.lang3.tuple.Pair;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NTDataPublisher;
import org.photonvision.common.dataflow.websocket.UIDataPublisher;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.server.UIUpdateType;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameConsumer;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;
import org.photonvision.vision.pipeline.PipelineType;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/**
* This is the God Class
*
* <p>VisionModule has a pipeline manager, vision runner, and data providers. The data providers
* provide info on settings changes. VisionModuleManager holds a list of all current vision modules.
*/
public class VisionModule {

    private final Logger logger;
    private final PipelineManager pipelineManager;
    private final VisionSource visionSource;
    private final VisionRunner visionRunner;
    private final LinkedList<CVPipelineResultConsumer> resultConsumers = new LinkedList<>();
    private final LinkedList<FrameConsumer> frameConsumers = new LinkedList<>();
    private final NTDataPublisher ntConsumer;
    private final UIDataPublisher uiDataConsumer;
    private final int moduleIndex;

    private long lastSettingChangeTimestamp = 0;

    private MJPGFrameConsumer dashboardInputStreamer;
    private MJPGFrameConsumer dashboardOutputStreamer;

    public VisionModule(PipelineManager pipelineManager, VisionSource visionSource, int index) {
        logger =
                new Logger(
                        VisionModule.class,
                        visionSource.getSettables().getConfiguration().nickname,
                        LogGroup.VisionModule);
        this.pipelineManager = pipelineManager;
        this.visionSource = visionSource;
        this.visionRunner =
                new VisionRunner(
                        this.visionSource.getFrameProvider(),
                        this.pipelineManager::getCurrentPipeline,
                        this::consumeResult);
        this.moduleIndex = index;

        DataChangeService.getInstance().addSubscriber(new VisionSettingChangeSubscriber());

        dashboardOutputStreamer =
                new MJPGFrameConsumer(
                        visionSource.getSettables().getConfiguration().uniqueName + "-output");
        dashboardInputStreamer =
                new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().uniqueName + "-input");

        addResultConsumer(result -> dashboardInputStreamer.accept(result.inputFrame));
        addResultConsumer(result -> dashboardOutputStreamer.accept(result.outputFrame));

        ntConsumer =
                new NTDataPublisher(
                        visionSource.getSettables().getConfiguration().nickname,
                        pipelineManager::getCurrentPipelineIndex,
                        pipelineManager::setIndex,
                        pipelineManager::getDriverMode,
                        this::setDriverMode);
        uiDataConsumer = new UIDataPublisher(index);
        addResultConsumer(ntConsumer);
        addResultConsumer(uiDataConsumer);

        setPipeline(visionSource.getSettables().getConfiguration().currentPipelineIndex);

        dashboardInputStreamer.setFrameDivisor(
                pipelineManager.getCurrentPipelineSettings().streamingFrameDivisor);
        dashboardOutputStreamer.setFrameDivisor(
                pipelineManager.getCurrentPipelineSettings().streamingFrameDivisor);
    }

    private void setDriverMode(boolean isDriverMode) {
        pipelineManager.setDriverMode(isDriverMode);
        saveAndBroadcastAll();
    }

    public void start() {
        visionRunner.startProcess();
    }

    private class VisionSettingChangeSubscriber extends DataChangeSubscriber {

        private VisionSettingChangeSubscriber() {
            super();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void onDataChangeEvent(DataChangeEvent event) {
            if (event instanceof IncomingWebSocketEvent) {
                var wsEvent = (IncomingWebSocketEvent<?>) event;

                // TODO: remove?
                if (wsEvent.propertyName.equals("save")) {
                    logger.debug("UI-based saving deprecated, ignoring");
                    // saveAndBroadcast();
                    return;
                }

                if (wsEvent.cameraIndex != null && wsEvent.cameraIndex == moduleIndex) {
                    logger.debug("Got PSC event - propName: " + wsEvent.propertyName);

                    var propName = wsEvent.propertyName;
                    var newPropValue = wsEvent.data;
                    var currentSettings = pipelineManager.getCurrentPipeline().getSettings();

                    // special case for non-PipelineSetting changes
                    switch (propName) {
                        case "cameraNickname": // rename camera
                            var newNickname = (String) newPropValue;
                            logger.info("Changing nickname to " + newNickname);
                            setCameraNickname(newNickname);
                            saveAndBroadcastAll();
                            return;
                        case "pipelineName": // rename current pipeline
                            logger.info("Changing nick to " + newPropValue);
                            pipelineManager.getCurrentPipelineSettings().pipelineNickname = (String) newPropValue;
                            saveAndBroadcastAll();
                            return;
                        case "newPipelineInfo": // add new pipeline
                            var typeName = (Pair<String, PipelineType>) newPropValue;
                            var type = typeName.getRight();
                            var name = typeName.getLeft();

                            logger.info("Adding a " + type + " pipeline with name " + name);

                            var addedSettings = pipelineManager.addPipeline(type);
                            addedSettings.pipelineNickname = name;
                            saveAndBroadcastAll();
                            return;
                        case "deleteCurrPipeline":
                            var indexToDelete = pipelineManager.getCurrentPipelineIndex();
                            logger.info("Deleting current pipe at index " + indexToDelete);
                            pipelineManager.removePipeline(indexToDelete);
                            saveAndBroadcastAll();
                            return;
                        case "changePipeline": // change active pipeline
                            var index = (Integer) newPropValue;
                            if (index == pipelineManager.getCurrentPipelineIndex()) {
                                logger.debug("Skipping pipeline change, index " + index + " already active");
                                return;
                            }
                            logger.debug("Setting pipeline index to " + index);
                            setPipeline(index);
                            saveAndBroadcastAll();
                            return;
                    }

                    // special case for camera settables
                    if (propName.startsWith("camera")) {
                        var propMethodName = "set" + propName.replace("camera", "");
                        var methods = visionSource.getSettables().getClass().getMethods();
                        for (var method : methods) {
                            if (method.getName().equalsIgnoreCase(propMethodName)) {
                                try {
                                    method.invoke(visionSource.getSettables(), newPropValue);
                                } catch (Exception e) {
                                    logger.error("Failed to invoke camera settable method: " + method.getName());
                                    e.printStackTrace();
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
                            propField.setDouble(currentSettings, (Double) newPropValue);
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
                            dashboardInputStreamer.setFrameDivisor(
                                    pipelineManager.getCurrentPipelineSettings().streamingFrameDivisor);
                            dashboardOutputStreamer.setFrameDivisor(
                                    pipelineManager.getCurrentPipelineSettings().streamingFrameDivisor);
                        }

                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        logger.error(
                                "Could not set prop "
                                        + propName
                                        + " with value "
                                        + newPropValue
                                        + " on "
                                        + currentSettings);
                        e.printStackTrace();
                    } catch (Exception e) {
                        logger.error("Unknown exception when setting PSC prop!");
                        e.printStackTrace();
                    }

                    saveModule();

                    VisionModule.this.lastSettingChangeTimestamp = System.currentTimeMillis();
                }
            }
        }
    }

    private void setPipeline(int index) {
        logger.info("Setting pipeline to " + index);
        pipelineManager.setIndex(index);
        var config = pipelineManager.getPipelineSettings(index);

        if (config == null) {
            logger.error("Config for index " + index + " was null!");
            return;
        }

        visionSource.getSettables().setCurrentVideoMode(config.cameraVideoModeIndex);
        visionSource.getSettables().setBrightness(config.cameraBrightness);
        visionSource.getSettables().setExposure(config.cameraExposure);
        visionSource.getSettables().setGain(config.cameraGain);
        visionSource.getSettables().getConfiguration().currentPipelineIndex =
                pipelineManager.getCurrentPipelineIndex();
    }

    private void saveModule() {
        ConfigManager.getInstance()
                .saveModule(
                        getStateAsCameraConfig(), visionSource.getSettables().getConfiguration().uniqueName);
    }

    private void saveAndBroadcastAll() {
        saveModule();
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                UIUpdateType.BROADCAST,
                                "fullsettings",
                                ConfigManager.getInstance().getConfig().toHashMap(),
                                null));
    }

    private void saveAndBroadcastSelective(WsContext originContext, String propertyName, Object value) {
        logger.trace("Broadcasting PSC mutation - " + propertyName + ": " + value);
        saveModule();
        DataChangeService.getInstance()
                .publishEvent(
                        OutgoingUIEvent.wrappedOf(
                                UIUpdateType.BROADCAST, "mutatePipeline", propertyName, value, originContext));
    }

    private void setCameraNickname(String newName) {
        visionSource.getSettables().getConfiguration().nickname = newName;
        ntConsumer.updateCameraNickname(newName);

        // rename streams
        frameConsumers.remove(dashboardOutputStreamer);
        frameConsumers.remove(dashboardInputStreamer);
        dashboardOutputStreamer =
                new MJPGFrameConsumer(
                        visionSource.getSettables().getConfiguration().uniqueName + "-output");
        dashboardInputStreamer =
                new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().uniqueName + "-input");
        frameConsumers.add(dashboardOutputStreamer);
        frameConsumers.add(dashboardInputStreamer);
    }

    public PhotonConfiguration.UICameraConfiguration toUICameraConfig() {
        var ret = new PhotonConfiguration.UICameraConfiguration();

        ret.fov = visionSource.getSettables().getFOV();
        //        ret.tiltDegrees = this.visionSource.getSettables() // TODO implement tilt in camera
        // settings
        ret.nickname = visionSource.getSettables().getConfiguration().nickname;
        ret.currentPipelineSettings =
                SerializationUtils.objectToHashMap(pipelineManager.getCurrentPipelineSettings());
        ret.currentPipelineIndex = pipelineManager.getCurrentPipelineIndex();
        ret.pipelineNicknames = pipelineManager.getPipelineNicknames();

        // TODO refactor into helper method
        var temp = new HashMap<Integer, HashMap<String, Object>>();
        var videoModes = visionSource.getSettables().getAllVideoModes();
        for (var k : videoModes.keySet()) {
            var internalMap = new HashMap<String, Object>();

            internalMap.put("width", videoModes.get(k).width);
            internalMap.put("height", videoModes.get(k).height);
            internalMap.put("fps", videoModes.get(k).fps);
            internalMap.put("pixelFormat", videoModes.get(k).pixelFormat.toString());

            temp.put(k, internalMap);
        }
        ret.videoFormatList = temp;
        ret.outputStreamPort = dashboardOutputStreamer.getCurrentStreamPort();
        ret.inputStreamPort = dashboardInputStreamer.getCurrentStreamPort();
        //         ret.uiStreamPort = uiStreamer.getCurrentStreamPort();

        return ret;
    }

    public CameraConfiguration getStateAsCameraConfig() {
        var config = visionSource.getSettables().getConfiguration();
        config.setPipelineSettings(pipelineManager.userPipelineSettings);
        config.driveModeSettings = pipelineManager.driverModePipeline.getSettings();
        config.currentPipelineIndex = pipelineManager.getCurrentPipelineIndex();

        return config;
    }

    public void addResultConsumer(CVPipelineResultConsumer dataConsumer) {
        resultConsumers.add(dataConsumer);
    }

    private void consumeResult(CVPipelineResult result) {
        consumePipelineResult(result);

        var frame = result.outputFrame;
        consumeFrame(frame);

        result.release();
    }

    private void consumePipelineResult(CVPipelineResult result) {
        for (var dataConsumer : resultConsumers) {
            dataConsumer.accept(result);
        }
    }

    private void consumeFrame(Frame frame) {
        for (var frameConsumer : frameConsumers) {
            frameConsumer.accept(frame);
        }
    }
}
