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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import edu.wpi.first.networktables.NetworkTableInstance;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.networktables.NTDataConsumer;
import org.photonvision.common.datatransfer.DataConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameConsumer;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/**
 * This is the God Class
 *
 * <p>VisionModule has a pipeline manager, vision runner, and data providers. The data providers
 * provide info on settings changes. VisionModuleManager holds a list of all current vision modules.
 */
public class VisionModule {
    private static final Logger logger = new Logger(VisionModule.class, LogGroup.VisionProcess);

    private final PipelineManager pipelineManager;
    private final VisionSource visionSource;
    private final VisionRunner visionRunner;
    private final LinkedList<DataConsumer> dataConsumers = new LinkedList<>();
    private final LinkedList<FrameConsumer> frameConsumers = new LinkedList<>();
    private final NTDataConsumer ntConsumer;
    private MJPGFrameConsumer streamer;
    private int moduleIndex;

    public VisionModule(PipelineManager pipelineManager, VisionSource visionSource, int index) {
        this.pipelineManager = pipelineManager;
        this.visionSource = visionSource;
        this.visionRunner =
            new VisionRunner(
                this.visionSource.getFrameProvider(),
                this.pipelineManager::getCurrentPipeline,
                this::consumeResult);

        DataChangeService.getInstance().addSubscriber(new VisionSettingChangeSubscriber());

        this.streamer = new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().nickname);
        this.ntConsumer = new NTDataConsumer(NetworkTableInstance.getDefault().getTable("photonvision"),
            visionSource.getSettables().getConfiguration().nickname);

        addFrameConsumer(streamer);
        addDataConsumer(ntConsumer);

        this.moduleIndex = index;
    }

    public void start() {
        visionRunner.startProcess();
    }

    private class VisionSettingChangeSubscriber extends DataChangeSubscriber {

        private VisionSettingChangeSubscriber() {
            super();
        }

        @Override
        public void onDataChangeEvent(DataChangeEvent event) {
            if (event instanceof IncomingWebSocketEvent) {
                var wsEvent = (IncomingWebSocketEvent<?>) event;
                if (wsEvent.cameraIndex != null && wsEvent.cameraIndex == moduleIndex) {
                    logger.debug(wsEvent.toString());

                    var propName = wsEvent.propertyName;
                    var newValue = wsEvent.data;

                    var currentSettings = pipelineManager.getCurrentPipeline().getSettings();

                    try {
                        var propField =  currentSettings.getClass().getField(propName);

                        if (propField.getType().isAssignableFrom(IntegerCouple.class)) {
                            var orig = (ArrayList<Integer>)newValue;
                            var actual = new IntegerCouple(orig.get(0), orig.get(1));
                            propField.set(currentSettings, actual);
                            return;
                        }

                        if (propField.getType().isAssignableFrom(DoubleCouple.class)) {
                            var orig = (ArrayList<Double>)newValue;
                            var actual = new DoubleCouple(orig.get(0), orig.get(1));
                            propField.set(currentSettings, actual);
                            return;
                        }

                        if (!propField.getType().isEnum()) { // if the field is not an enum, get it based on the current pipeline
                            propField.set(newValue, newValue);
                        } else {
                            // TODO: Enums!
                            var enumType = propField.getType();
                            logger.error("Could not set prop. Enums not implemented - " + enumType.getCanonicalName());
                        }

                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        logger.error("Could not set prop " + propName + " with value " + newValue + " on " + currentSettings);
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void setCameraNickname(String newName) {
        visionSource.getSettables().getConfiguration().nickname = newName;
        ntConsumer.setCameraName(newName);

        frameConsumers.remove(streamer);
        this.streamer = new MJPGFrameConsumer(visionSource.getSettables().getConfiguration().nickname);
        this.frameConsumers.add(streamer);
    }

    public PhotonConfiguration.UICameraConfiguration toUICameraConfig() {
        // TODO
        var ret = new PhotonConfiguration.UICameraConfiguration();

        ret.fov = this.visionSource.getSettables().getFOV();
//        ret.tiltDegrees = this.visionSource.getSettables() // TODO implement tilt in camera settings
        ret.nickname = visionSource.getSettables().getConfiguration().nickname;
        ret.currentPipelineSettings = SerializationUtils.objectToHashMap(pipelineManager.getCurrentPipelineSettings());
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
        ret.streamPort = streamer.getCurrentStreamPort();

        return ret;
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
