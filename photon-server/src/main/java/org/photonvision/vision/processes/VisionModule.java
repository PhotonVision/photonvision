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
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

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
    private final LinkedList<CVPipelineResultConsumer> fpsLimitedResultConsumers = new LinkedList<>();
    private final NTDataPublisher ntConsumer;
    private final UIDataPublisher uiDataConsumer;
    protected final int moduleIndex;
    protected final QuirkyCamera cameraQuirks;

    private long lastFrameConsumeMillis;
    protected TrackedTarget lastPipelineResultBestTarget;

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

        createStreams();
        fpsLimitedResultConsumers.add(result -> dashboardInputStreamer.accept(result.inputFrame));
        fpsLimitedResultConsumers.add(result -> dashboardOutputStreamer.accept(result.outputFrame));

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
        addResultConsumer(
                (result) ->
                        lastPipelineResultBestTarget = result.hasTargets() ? result.targets.get(0) : null);

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

            HardwareManager.getInstance()
                    .visionLED
                    .setPipelineModeSupplier(() -> pipelineManager.getCurrentPipelineSettings().ledMode);
            setVisionLEDs(pipelineManager.getCurrentPipelineSettings().ledMode);
        }

        saveAndBroadcastAll();
    }

    private void destroyStreams() {
        dashboardInputStreamer.close();
        dashboardOutputStreamer.close();
    }

    private void createStreams() {
        var camStreamIdx = visionSource.getSettables().getConfiguration().streamIndex;
        // If idx = 0, we want (1181, 1182)
        var inputStreamPort = 1181 + (camStreamIdx * 2);
        var outputStreamPort = 1181 + (camStreamIdx * 2) + 1;

        dashboardOutputStreamer =
                new MJPGFrameConsumer(
                        visionSource.getSettables().getConfiguration().nickname + "-output", outputStreamPort);
        dashboardInputStreamer =
                new MJPGFrameConsumer(
                        visionSource.getSettables().getConfiguration().nickname + "-input", inputStreamPort);
    }

    void setDriverMode(boolean isDriverMode) {
        pipelineManager.setDriverMode(isDriverMode);
        setVisionLEDs(!isDriverMode);
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

    private boolean isVendorCamera() {
        return visionSource.isVendorCamera();
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

        // Disable gain if not applicable
        if (!cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
            settings.cameraGain = -1;
        }

        pipelineManager.setCalibrationMode(true);
    }

    public void takeCalibrationSnapshot() {
        pipelineManager.calibration3dPipeline.takeSnapshot();
    }

    public CameraCalibrationCoefficients endCalibration() {
        var ret = pipelineManager.calibration3dPipeline.tryCalibration();
        pipelineManager.setCalibrationMode(false);

        setPipeline(pipelineManager.getCurrentPipelineIndex());

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

        setVisionLEDs(config.ledMode);

        visionSource.getSettables().getConfiguration().currentPipelineIndex =
                pipelineManager.getCurrentPipelineIndex();
    }

    private void setVisionLEDs(boolean on) {
        if (isVendorCamera()) HardwareManager.getInstance().visionLED.setState(on);
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
        fpsLimitedResultConsumers.clear();

        // Teardown and recreate streams
        destroyStreams();
        createStreams();

        fpsLimitedResultConsumers.add(result -> dashboardInputStreamer.accept(result.inputFrame));
        fpsLimitedResultConsumers.add(result -> dashboardOutputStreamer.accept(result.outputFrame));
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
                !(ConfigManager.getInstance().getConfig().getHardwareConfig().hasPresetFOV()
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
        consumeFpsLimitedResult(result);

        result.release();
    }

    private void consumePipelineResult(CVPipelineResult result) {
        for (var dataConsumer : resultConsumers) {
            dataConsumer.accept(result);
        }
    }

    private void consumeFpsLimitedResult(CVPipelineResult result) {
        if (System.currentTimeMillis() - lastFrameConsumeMillis > 1000 / StreamFPSCap) {
            for (var c : fpsLimitedResultConsumers) {
                c.accept(result);
            }
            lastFrameConsumeMillis = System.currentTimeMillis();
        }
    }

    public void setTargetModel(TargetModel targetModel) {
        var settings = pipelineManager.getCurrentUserPipeline().getSettings();
        if (settings instanceof ReflectivePipelineSettings) {
            ((ReflectivePipelineSettings) settings).targetModel = targetModel;
            saveAndBroadcastAll();
        } else {
            logger.error("Cannot set target model of non-reflective pipe! Ignoring...");
        }
    }
}
