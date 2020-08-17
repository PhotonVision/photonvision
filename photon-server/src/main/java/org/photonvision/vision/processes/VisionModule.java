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

import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.util.Units;
import io.javalin.websocket.WsContext;
import java.util.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NTDataPublisher;
import org.photonvision.common.dataflow.websocket.UIDataPublisher;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameConsumer;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/**
* This is the God Class
*
* <p>VisionModule has a pipeline manager, vision runner, and data providers. The data providers
* provide info on settings changes. VisionModuleManager holds a list of all current vision modules.
*/
public class VisionModule {

    private static final int StreamFPSCap = 30;

    private final Logger logger;
    protected final PipelineManager pipelineManager;
    protected final VisionSource visionSource;
    private final VisionRunner visionRunner;
    private final LinkedList<CVPipelineResultConsumer> resultConsumers = new LinkedList<>();
    private final LinkedList<FrameConsumer> frameConsumers = new LinkedList<>();
    private final NTDataPublisher ntConsumer;
    private final UIDataPublisher uiDataConsumer;
    protected final int moduleIndex;
    protected final QuirkyCamera cameraQuirks;

    private long lastFrameConsumeMillis;

    MJPGFrameConsumer dashboardInputStreamer;
    MJPGFrameConsumer dashboardOutputStreamer;

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
                        this.pipelineManager::getCurrentUserPipeline,
                        this::consumeResult);
        this.moduleIndex = index;

        // do this
        if (visionSource instanceof USBCameraSource) {
            cameraQuirks = ((USBCameraSource) visionSource).cameraQuirks;
        } else {
            cameraQuirks = QuirkyCamera.DefaultCamera;
        }

        DataChangeService.getInstance().addSubscriber(new VisionModuleChangeSubscriber(this));

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

        // Set vendor FOV
        if (isVendorCamera()) {
            var fov = ConfigManager.getInstance().getConfig().getHardwareConfig().vendorFOV;
            logger.info("Setting FOV of vendor camera to " + fov);
            visionSource.getSettables().setFOV(fov);
        }
    }

    void setDriverMode(boolean isDriverMode) {
        pipelineManager.setDriverMode(isDriverMode);
        saveAndBroadcastAll();
    }

    public void start() {
        visionRunner.startProcess();
    }

    public void setFovAndPitch(double fov, Rotation2d pitch) {
        var settables = visionSource.getSettables();
        logger.trace(
                () ->
                        "Setting "
                                + settables.getConfiguration().nickname
                                + ": pitch ("
                                + pitch.getDegrees()
                                + ") FOV ("
                                + fov
                                + ")");
        settables.setCameraPitch(pitch);

        // Only set FOV if we have no vendor JSON and we aren't using a PiCAM
        if (isVendorCamera()) {
            logger.info("Cannot set FOV on a vendor device! Ignoring...");
        } else {
            settables.setFOV(fov);
        }
    }

    // TODO improve robustness of this detection
    private boolean isVendorCamera() {
        return ConfigManager.getInstance().getConfig().getHardwareConfig().hasPresetFOV()
                && cameraQuirks.hasQuirk(CameraQuirk.PiCam);
    }

    public void startCalibration(UICalibrationData data) {
        var settings = pipelineManager.calibration3dPipeline.getSettings();
        settings.cameraVideoModeIndex = data.videoModeIndex;
        visionSource.getSettables().setVideoModeIndex(data.videoModeIndex);
        logger.info(
                "Starting calibration at resolution index "
                        + data.videoModeIndex
                        + " and settings "
                        + data);
        settings.gridSize = Units.inchesToMeters(data.squareSizeIn);
        settings.boardHeight = data.patternHeight;
        settings.boardWidth = data.patternWidth;
        settings.boardType = data.boardType;
        pipelineManager.setCalibrationMode(true);
    }

    public void takeCalibrationSnapshot() {
        pipelineManager.calibration3dPipeline.takeSnapshot();
    }

    public CameraCalibrationCoefficients endCalibration() {
        var ret = pipelineManager.calibration3dPipeline.tryCalibration();
        pipelineManager.setCalibrationMode(false);

        if (ret != null) {
            logger.debug("Saving calibration...");
            visionSource.getSettables().getConfiguration().addCalibration(ret);
            visionSource.getSettables().calculateFrameStaticProps();
        } else {
            logger.error("Calibration failed...");
        }
        saveAndBroadcastAll();
        return ret;
    }

    void setPipeline(int index) {
        logger.info("Setting pipeline to " + index);
        pipelineManager.setIndex(index);
        var config = pipelineManager.getPipelineSettings(index);

        if (config == null) {
            logger.error("Config for index " + index + " was null!");
            return;
        }

        visionSource.getSettables().setVideoModeInternal(config.cameraVideoModeIndex);
        visionSource.getSettables().setBrightness(config.cameraBrightness);
        visionSource.getSettables().setExposure(config.cameraExposure);

        if (!cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
            config.cameraGain = -1;
        } else {
            visionSource.getSettables().setGain(config.cameraGain);
        }

        visionSource.getSettables().getConfiguration().currentPipelineIndex =
                pipelineManager.getCurrentPipelineIndex();
    }

    public void saveModule() {
        ConfigManager.getInstance()
                .saveModule(
                        getStateAsCameraConfig(), visionSource.getSettables().getConfiguration().uniqueName);
    }

    void saveAndBroadcastAll() {
        saveModule();
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));
    }

    void saveAndBroadcastSelective(WsContext originContext, String propertyName, Object value) {
        logger.trace("Broadcasting PSC mutation - " + propertyName + ": " + value);
        saveModule();
        DataChangeService.getInstance()
                .publishEvent(
                        OutgoingUIEvent.wrappedOf("mutatePipeline", propertyName, value, originContext));
    }

    void setCameraNickname(String newName) {
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

        var calList = new ArrayList<HashMap<String, Object>>();
        for (var c : visionSource.getSettables().getConfiguration().calibrations) {
            var internalMap = new HashMap<String, Object>();

            internalMap.put("perViewErrors", c.perViewErrors);
            internalMap.put("standardDeviation", c.standardDeviation);
            internalMap.put("width", c.resolution.width);
            internalMap.put("height", c.resolution.height);
            internalMap.put("intrinsics", c.cameraIntrinsics.data);
            internalMap.put("extrinsics", c.cameraExtrinsics.data);

            calList.add(internalMap);
        }
        ret.calibrations = calList;

        ret.isFovConfigurable =
                !(HardwareManager.getInstance().getConfig().hasPresetFOV()
                        && cameraQuirks.hasQuirk(CameraQuirk.PiCam));

        return ret;
    }

    public CameraConfiguration getStateAsCameraConfig() {
        var config = visionSource.getSettables().getConfiguration();
        config.setPipelineSettings(pipelineManager.userPipelineSettings);
        config.driveModeSettings = pipelineManager.driverModePipeline.getSettings();
        config.currentPipelineIndex = Math.max(pipelineManager.getCurrentPipelineIndex(), -1);

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
        if (System.currentTimeMillis() - lastFrameConsumeMillis > 1000 / StreamFPSCap) {
            for (var frameConsumer : frameConsumers) {
                frameConsumer.accept(frame);
            }
            lastFrameConsumeMillis = System.currentTimeMillis();
        }
    }
}
