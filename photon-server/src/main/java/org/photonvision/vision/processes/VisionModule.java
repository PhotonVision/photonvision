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

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NTDataConsumer;
import org.photonvision.common.datatransfer.DataConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.server.SocketHandler;
import org.photonvision.server.UIUpdateType;
import org.photonvision.vision.camera.USBCameraSource;
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
    private final LinkedList<DataConsumer> dataConsumers = new LinkedList<>();
    private final LinkedList<FrameConsumer> frameConsumers = new LinkedList<>();
    private final NTDataConsumer ntConsumer;
    private final int moduleIndex;
    private final MJPGFrameConsumer uiStreamer;
    private long lastUpdateTimestamp = -1;

    private MJPGFrameConsumer dashboardStreamer;

    public VisionModule(PipelineManager pipelineManager, VisionSource visionSource, int index) {
        logger =
                new Logger(
                        VisionModule.class,
                        ((USBCameraSource) visionSource).configuration.nickname,
                        LogGroup.VisionProcess);
        this.pipelineManager = pipelineManager;
        this.visionSource = visionSource;
        this.visionRunner =
                new VisionRunner(
                        this.visionSource.getFrameProvider(),
                        this.pipelineManager::getCurrentPipeline,
                        this::consumeResult);
        this.moduleIndex = index;

        DataChangeService.getInstance().addSubscriber(new VisionSettingChangeSubscriber());

        dashboardStreamer =
                new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().uniqueName);
        uiStreamer = new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().nickname);
        addFrameConsumer(dashboardStreamer);
        addFrameConsumer(uiStreamer);

        ntConsumer =
                new NTDataConsumer(
                        NetworkTableInstance.getDefault().getTable("photonvision"),
                        visionSource.getSettables().getConfiguration().nickname);
        addDataConsumer(ntConsumer);
        addDataConsumer(
                data -> {
                    var now = System.currentTimeMillis();
                    if (lastUpdateTimestamp + 1000.0 / 15.0 > now) return;

                    var uiMap = new HashMap<Integer, HashMap<String, Object>>();
                    var dataMap = new HashMap<String, Object>();
                    dataMap.put("fps", 1000.0 / data.result.getLatencyMillis());
                    var targets = data.result.targets;
                    var uiTargets = new ArrayList<HashMap<String, Object>>();
                    for (var t : targets) {
                        uiTargets.add(t.toHashMap());
                    }
                    dataMap.put("targets", uiTargets);

                    uiMap.put(index, dataMap);
                    var retMap = new HashMap<String, Object>();
                    retMap.put("updatePipelineResult", uiMap);

                    try {
                        SocketHandler.getInstance().broadcastMessage(retMap, null);
                    } catch (JsonProcessingException e) {
                        logger.error(e.getMessage());
                        logger.error(Arrays.toString(e.getStackTrace()));
                    }

                    lastUpdateTimestamp = now;
                });

        setPipeline(visionSource.getSettables().getConfiguration().currentPipelineIndex);

        dashboardStreamer.setFrameDivisor(
                pipelineManager.getCurrentPipelineSettings().outputFrameDivisor);
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
                            saveAndBroadcast();
                            return;
                        case "pipelineName": // rename current pipeline
                            logger.info("Changing nick to " + newPropValue);
                            pipelineManager.getCurrentPipelineSettings().pipelineNickname = (String) newPropValue;
                            saveAndBroadcast();
                            return;
                        case "newPipelineInfo": // add new pipeline
                            var typeName = (Pair<PipelineType, String>) newPropValue;
                            var type = typeName.getLeft();
                            var name = typeName.getRight();

                            logger.info("Adding a " + type + " pipeline with name " + name);

                            var addedSettings = pipelineManager.addPipeline(type);
                            addedSettings.pipelineNickname = name;
                            saveAndBroadcast();
                            return;
                        case "changePipeline": // change active pipeline
                            var index = (Integer) newPropValue;
                            if (index == pipelineManager.getCurrentPipelineIndex()) {
                                logger.debug("Skipping pipeline change, index " + index + " already active");
                                return;
                            }
                            logger.debug("Setting pipeline index to " + index);
                            setPipeline(index);
                            saveAndBroadcast();
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
                        if (propName.equals("outputFrameDivisor")) {
                            dashboardStreamer.setFrameDivisor(
                                    pipelineManager.getCurrentPipelineSettings().outputFrameDivisor);
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
                }
                saveAndBroadcast();
            }
        }
    }

    private void setPipeline(int index) {
        logger.info("Setting pipeline to " + index);
        pipelineManager.setIndex(index);
        var config = pipelineManager.getPipelineSettings(index);
        visionSource.getSettables().setCurrentVideoMode(config.cameraVideoModeIndex);
        visionSource.getSettables().setBrightness(config.cameraBrightness);
        visionSource.getSettables().setExposure(config.cameraExposure);
        visionSource.getSettables().setGain(config.cameraGain);
        visionSource.getSettables().getConfiguration().currentPipelineIndex =
                pipelineManager.getCurrentPipelineIndex();
    }

    private void saveAndBroadcast() {
        ConfigManager.getInstance()
                .saveModule(
                        getStateAsCameraConfig(), visionSource.getSettables().getConfiguration().uniqueName);
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                UIUpdateType.BROADCAST,
                                "fullsettings",
                                ConfigManager.getInstance().getConfig().toHashMap()));
    }

    private void save() {
        ConfigManager.getInstance()
                .saveModule(
                        getStateAsCameraConfig(), visionSource.getSettables().getConfiguration().uniqueName);
    }

    private void setCameraNickname(String newName) {
        visionSource.getSettables().getConfiguration().nickname = newName;
        ntConsumer.setCameraName(newName);

        frameConsumers.remove(dashboardStreamer);
        dashboardStreamer =
                new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().nickname);
        frameConsumers.add(dashboardStreamer);
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
        ret.streamPort = dashboardStreamer.getCurrentStreamPort();
        // ret.uiStreamPort = uiStreamer.getCurrentStreamPort();

        return ret;
    }

    public CameraConfiguration getStateAsCameraConfig() {
        var config = visionSource.getSettables().getConfiguration();
        config.setPipelineSettings(pipelineManager.userPipelineSettings);
        config.driveModeSettings = pipelineManager.driverModePipeline.getSettings();
        config.currentPipelineIndex = pipelineManager.getCurrentPipelineIndex();

        return config;
    }

    public void addDataConsumer(DataConsumer dataConsumer) {
        dataConsumers.add(dataConsumer);
    }

    void addFrameConsumer(FrameConsumer consumer) {
        frameConsumers.add(consumer);
    }

    void consumeResult(CVPipelineResult result) {
        // TODO: put result in to Data (not this way!)
        var data = new Data();
        data.result = result;
        consumeData(data);

        var frame = result.outputFrame;
        consumeFrame(frame);

        data.release();
    }

    void consumeData(Data data) {
        for (var dataConsumer : dataConsumers) {
            dataConsumer.accept(data);
        }
    }

    void consumeFrame(Frame frame) {
        for (var frameConsumer : frameConsumers) {
            frameConsumer.accept(frame);
        }
    }
}
