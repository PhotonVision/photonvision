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

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.cscore.VideoException;
import edu.wpi.first.math.util.Units;
import io.javalin.websocket.WsContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import org.opencv.core.Size;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeService.SubscriberHandle;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NTDataPublisher;
import org.photonvision.common.dataflow.statusLEDs.StatusLEDConsumer;
import org.photonvision.common.dataflow.websocket.UICameraConfiguration;
import org.photonvision.common.dataflow.websocket.UIDataPublisher;
import org.photonvision.common.dataflow.websocket.UIPhotonConfiguration;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.csi.LibcameraGpuSource;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.consumer.FileSaveFrameConsumer;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.OutputStreamPipeline;
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
    private final Logger logger;
    protected final PipelineManager pipelineManager;
    protected final VisionSource visionSource;
    private final VisionRunner visionRunner;
    private final StreamRunnable streamRunnable;
    private final VisionModuleChangeSubscriber changeSubscriber;
    private final SubscriberHandle changeSubscriberHandle;
    private final LinkedList<CVPipelineResultConsumer> resultConsumers = new LinkedList<>();
    // Raw result consumers run before any drawing has been done by the
    // OutputStreamPipeline
    private final LinkedList<BiConsumer<Frame, List<TrackedTarget>>> streamResultConsumers =
            new LinkedList<>();
    private final NTDataPublisher ntConsumer;
    private final UIDataPublisher uiDataConsumer;
    private final StatusLEDConsumer statusLEDsConsumer;
    protected final QuirkyCamera cameraQuirks;

    protected TrackedTarget lastPipelineResultBestTarget;

    private int inputStreamPort = -1;
    private int outputStreamPort = -1;

    FileSaveFrameConsumer inputFrameSaver;
    FileSaveFrameConsumer outputFrameSaver;

    MJPGFrameConsumer inputVideoStreamer;
    MJPGFrameConsumer outputVideoStreamer;

    public VisionModule(PipelineManager pipelineManager, VisionSource visionSource) {
        logger =
                new Logger(
                        VisionModule.class,
                        visionSource.getSettables().getConfiguration().nickname,
                        LogGroup.VisionModule);

        cameraQuirks = visionSource.getCameraConfiguration().cameraQuirks;

        if (visionSource.getCameraConfiguration().cameraQuirks == null)
            visionSource.getCameraConfiguration().cameraQuirks = QuirkyCamera.DefaultCamera;

        // We don't show gain if the config says it's -1. So check here to make sure
        // it's non-negative if it _is_ supported
        if (cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
            pipelineManager.userPipelineSettings.forEach(
                    it -> {
                        if (it.cameraGain == -1) it.cameraGain = 75; // Sane default
                    });
        }
        if (cameraQuirks.hasQuirk(CameraQuirk.AwbRedBlueGain)) {
            pipelineManager.userPipelineSettings.forEach(
                    it -> {
                        if (it.cameraRedGain == -1) it.cameraRedGain = 11; // Sane defaults
                        if (it.cameraBlueGain == -1) it.cameraBlueGain = 20;
                    });
        }

        this.pipelineManager = pipelineManager;
        this.visionSource = visionSource;
        changeSubscriber = new VisionModuleChangeSubscriber(this);
        this.visionRunner =
                new VisionRunner(
                        this.visionSource.getFrameProvider(),
                        this.pipelineManager::getCurrentPipeline,
                        this::consumeResult,
                        this.cameraQuirks,
                        getChangeSubscriber());
        this.streamRunnable = new StreamRunnable(new OutputStreamPipeline());
        changeSubscriberHandle = DataChangeService.getInstance().addSubscriber(changeSubscriber);

        createStreams();

        recreateStreamResultConsumers();

        ntConsumer =
                new NTDataPublisher(
                        visionSource.getSettables().getConfiguration().nickname,
                        pipelineManager::getRequestedIndex,
                        this::setPipeline,
                        pipelineManager::getDriverMode,
                        this::setDriverMode);
        uiDataConsumer = new UIDataPublisher(visionSource.getSettables().getConfiguration().uniqueName);
        statusLEDsConsumer =
                new StatusLEDConsumer(visionSource.getSettables().getConfiguration().uniqueName);
        addResultConsumer(ntConsumer);
        addResultConsumer(uiDataConsumer);
        addResultConsumer(statusLEDsConsumer);
        addResultConsumer(
                (result) ->
                        lastPipelineResultBestTarget = result.hasTargets() ? result.targets.get(0) : null);

        // Sync VisionModule state with the first pipeline index
        setPipeline(visionSource.getSettables().getConfiguration().currentPipelineIndex);

        // Set vendor FOV
        if (isVendorCamera()) {
            var fov = ConfigManager.getInstance().getConfig().getHardwareConfig().vendorFOV;
            logger.info("Setting FOV of vendor camera to " + fov);
            visionSource.getSettables().setFOV(fov);
        }

        // Configure LED's if supported by the underlying hardware
        if (HardwareManager.getInstance().visionLED != null && this.camShouldControlLEDs()) {
            HardwareManager.getInstance()
                    .visionLED
                    .setPipelineModeSupplier(() -> pipelineManager.getCurrentPipelineSettings().ledMode);
            setVisionLEDs(pipelineManager.getCurrentPipelineSettings().ledMode);
        }

        saveAndBroadcastAll();
    }

    private void createStreams() {
        var camStreamIdx = visionSource.getSettables().getConfiguration().streamIndex;
        // If idx = 0, we want (1181, 1182)
        this.inputStreamPort = 1181 + (camStreamIdx * 2);
        this.outputStreamPort = 1181 + (camStreamIdx * 2) + 1;

        inputFrameSaver =
                new FileSaveFrameConsumer(
                        visionSource.getSettables().getConfiguration().nickname,
                        visionSource.getSettables().getConfiguration().uniqueName,
                        "input");
        outputFrameSaver =
                new FileSaveFrameConsumer(
                        visionSource.getSettables().getConfiguration().nickname,
                        visionSource.getSettables().getConfiguration().uniqueName,
                        "output");

        String camHostname = CameraServerJNI.getHostname();
        inputVideoStreamer =
                new MJPGFrameConsumer(
                        camHostname + "_Port_" + inputStreamPort + "_Input_MJPEG_Server", inputStreamPort);
        outputVideoStreamer =
                new MJPGFrameConsumer(
                        camHostname + "_Port_" + outputStreamPort + "_Output_MJPEG_Server", outputStreamPort);
    }

    private void recreateStreamResultConsumers() {
        streamResultConsumers.add(
                (frame, tgts) -> {
                    if (frame != null) inputFrameSaver.accept(frame.colorImage);
                });
        streamResultConsumers.add(
                (frame, tgts) -> {
                    if (frame != null) outputFrameSaver.accept(frame.processedImage);
                });
        streamResultConsumers.add(
                (frame, tgts) -> {
                    if (frame != null) inputVideoStreamer.accept(frame.colorImage);
                });
        streamResultConsumers.add(
                (frame, tgts) -> {
                    if (frame != null) outputVideoStreamer.accept(frame.processedImage);
                });
    }

    private class StreamRunnable extends Thread {
        private final OutputStreamPipeline outputStreamPipeline;

        private final Object frameLock = new Object();
        private Frame latestFrame;
        private AdvancedPipelineSettings settings = new AdvancedPipelineSettings();
        private List<TrackedTarget> targets = new ArrayList<>();

        private boolean shouldRun = false;

        public StreamRunnable(OutputStreamPipeline outputStreamPipeline) {
            this.outputStreamPipeline = outputStreamPipeline;
        }

        public void updateData(
                Frame inputOutputFrame, AdvancedPipelineSettings settings, List<TrackedTarget> targets) {
            synchronized (frameLock) {
                if (shouldRun && this.latestFrame != null) {
                    logger.trace("Fell behind; releasing last unused Mats");
                    this.latestFrame.release();
                }

                this.latestFrame = inputOutputFrame;
                this.settings = settings;
                this.targets = targets;

                shouldRun = inputOutputFrame != null;
                // && inputOutputFrame.colorImage != null
                // && !inputOutputFrame.colorImage.getMat().empty()
                // && inputOutputFrame.processedImage != null
                // && !inputOutputFrame.processedImage.getMat().empty();
            }
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                final Frame m_frame;
                final AdvancedPipelineSettings settings;
                final List<TrackedTarget> targets;
                final boolean shouldRun;
                synchronized (frameLock) {
                    m_frame = this.latestFrame;
                    this.latestFrame = null;

                    settings = this.settings;
                    targets = this.targets;
                    shouldRun = this.shouldRun;

                    this.shouldRun = false;
                }
                if (shouldRun) {
                    try {
                        CVPipelineResult osr = outputStreamPipeline.process(m_frame, settings, targets);
                        consumeResults(m_frame, targets);

                    } catch (Exception e) {
                        // Never die
                        logger.error("Exception while running stream runnable!", e);
                    }
                    try {
                        m_frame.release();
                    } catch (Exception e) {
                        logger.error("Exception freeing frames", e);
                    }
                } else {
                    // busy wait! hurray!
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        logger.warn("StreamRunnable was interrupted - exiting");
                        return;
                    }
                }
            }
        }
    }

    public void start() {
        visionSource.cameraConfiguration.deactivated = false;
        visionRunner.startProcess();
        streamRunnable.start();
    }

    public void stop() {
        visionSource.cameraConfiguration.deactivated = true;
        visionRunner.stopProcess();

        try {
            streamRunnable.interrupt();
            streamRunnable.join();
        } catch (InterruptedException e) {
            logger.error("Exception killing process thread", e);
        }

        visionSource.release();

        inputVideoStreamer.close();
        outputVideoStreamer.close();
        inputFrameSaver.close();
        outputFrameSaver.close();

        changeSubscriberHandle.stop();
        setVisionLEDs(false);
    }

    public void setFov(double fov) {
        var settables = visionSource.getSettables();
        logger.trace(() -> "Setting " + settables.getConfiguration().nickname + ") FOV (" + fov + ")");

        // Only set FOV if we have no vendor JSON, and we aren't using a PiCAM
        if (isVendorCamera()) {
            logger.info("Cannot set FOV on a vendor device! Ignoring...");
        } else {
            settables.setFOV(fov);
        }
    }

    private boolean isVendorCamera() {
        return visionSource.isVendorCamera();
    }

    public VisionModuleChangeSubscriber getChangeSubscriber() {
        return changeSubscriber;
    }

    void changePipelineType(int newType) {
        pipelineManager.changePipelineType(newType);
        setPipeline(pipelineManager.getRequestedIndex());
        saveAndBroadcastAll();
    }

    void setDriverMode(boolean isDriverMode) {
        pipelineManager.setDriverMode(isDriverMode);
        setVisionLEDs(!isDriverMode);
        setPipeline(
                isDriverMode ? PipelineManager.DRIVERMODE_INDEX : pipelineManager.getRequestedIndex());
        saveAndBroadcastAll();
    }

    public void startCalibration(UICalibrationData data) {
        var settings = pipelineManager.calibration3dPipeline.getSettings();

        var videoMode = visionSource.getSettables().getAllVideoModes().get(data.videoModeIndex);
        var resolution = new Size(videoMode.width, videoMode.height);

        settings.cameraVideoModeIndex = data.videoModeIndex;
        visionSource.getSettables().setVideoModeIndex(data.videoModeIndex);
        logger.info(
                "Starting calibration at resolution index "
                        + data.videoModeIndex
                        + " and settings "
                        + data);
        settings.gridSize = Units.inchesToMeters(data.squareSizeIn);
        settings.markerSize = Units.inchesToMeters(data.markerSizeIn);
        settings.boardHeight = data.patternHeight;
        settings.boardWidth = data.patternWidth;
        settings.boardType = data.boardType;
        settings.resolution = resolution;
        settings.useOldPattern = data.useOldPattern;
        settings.tagFamily = data.tagFamily;

        // Disable gain if not applicable
        if (!cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
            settings.cameraGain = -1;
        }
        if (!cameraQuirks.hasQuirk(CameraQuirk.AwbRedBlueGain)) {
            settings.cameraRedGain = -1;
            settings.cameraBlueGain = -1;
        }

        settings.cameraAutoExposure = true;

        setPipeline(PipelineManager.CAL_3D_INDEX);
    }

    public void saveInputSnapshot() {
        inputFrameSaver.overrideTakeSnapshot();
    }

    public void saveOutputSnapshot() {
        outputFrameSaver.overrideTakeSnapshot();
    }

    public void takeCalibrationSnapshot() {
        pipelineManager.calibration3dPipeline.takeSnapshot();
    }

    public CameraCalibrationCoefficients endCalibration() {
        var ret =
                pipelineManager.calibration3dPipeline.tryCalibration(
                        ConfigManager.getInstance()
                                .getCalibrationImageSavePathWithRes(
                                        pipelineManager.calibration3dPipeline.getSettings().resolution,
                                        visionSource.getCameraConfiguration().uniqueName));
        pipelineManager.setCalibrationMode(false);

        setPipeline(pipelineManager.getRequestedIndex());

        if (ret != null) {
            logger.debug("Saving calibration...");
            visionSource.getSettables().addCalibration(ret);
        } else {
            logger.error("Calibration failed...");
        }
        saveAndBroadcastAll();
        return ret;
    }

    boolean setPipeline(int index) {
        logger.info("Setting pipeline to " + index);
        logger.info("Pipeline name: " + pipelineManager.getPipelineNickname(index));
        pipelineManager.setIndex(index);

        VisionSourceSettables settables = visionSource.getSettables();

        var pipelineSettings = pipelineManager.getPipelineSettings(index);

        if (pipelineSettings == null) {
            logger.error("Config for index " + index + " was null! Not changing pipelines");
            return false;
        }

        visionRunner.runSynchronously(
                () -> {
                    settables.setVideoModeInternal(pipelineSettings.cameraVideoModeIndex);
                    settables.setBrightness(pipelineSettings.cameraBrightness);

                    // If manual exposure, force exposure slider to be valid
                    if (!pipelineSettings.cameraAutoExposure) {
                        if (pipelineSettings.cameraExposureRaw < 0)
                            pipelineSettings.cameraExposureRaw = 10; // reasonable default
                    }

                    settables.setExposureRaw(pipelineSettings.cameraExposureRaw);
                    try {
                        settables.setAutoExposure(pipelineSettings.cameraAutoExposure);
                    } catch (VideoException e) {
                        logger.error("Unable to set camera auto exposure!");
                        logger.error(e.toString());
                    }
                    if (cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
                        // If the gain is disabled for some reason, re-enable it
                        if (pipelineSettings.cameraGain == -1) pipelineSettings.cameraGain = 75;
                        settables.setGain(Math.max(0, pipelineSettings.cameraGain));
                    } else {
                        pipelineSettings.cameraGain = -1;
                    }

                    if (cameraQuirks.hasQuirk(CameraQuirk.AwbRedBlueGain)) {
                        // If the AWB gains are disabled for some reason, re-enable it
                        if (pipelineSettings.cameraRedGain == -1) pipelineSettings.cameraRedGain = 11;
                        if (pipelineSettings.cameraBlueGain == -1) pipelineSettings.cameraBlueGain = 20;
                        settables.setRedGain(Math.max(0, pipelineSettings.cameraRedGain));
                        settables.setBlueGain(Math.max(0, pipelineSettings.cameraBlueGain));
                    } else {
                        pipelineSettings.cameraRedGain = -1;
                        pipelineSettings.cameraBlueGain = -1;

                        // All other cameras (than picams) should support AWB temp
                        settables.setWhiteBalanceTemp(pipelineSettings.cameraWhiteBalanceTemp);
                        settables.setAutoWhiteBalance(pipelineSettings.cameraAutoWhiteBalance);
                    }

                    setVisionLEDs(pipelineSettings.ledMode);

                    settables.getConfiguration().currentPipelineIndex = pipelineManager.getRequestedIndex();
                });

        return true;
    }

    private boolean camShouldControlLEDs() {
        // Heuristic - if the camera has a known FOV or is a piCam, assume it's in use
        // for
        // vision processing, and should command stuff to the LED's.
        // TODO: Make LED control a property of the camera itself and controllable in
        // the UI.
        return isVendorCamera();
    }

    private void setVisionLEDs(boolean on) {
        if (camShouldControlLEDs() && HardwareManager.getInstance().visionLED != null)
            HardwareManager.getInstance().visionLED.setState(on);
    }

    public void saveModule() {
        ConfigManager.getInstance()
                .saveModule(
                        getStateAsCameraConfig(), visionSource.getSettables().getConfiguration().uniqueName);
    }

    public void saveAndBroadcastAll() {
        saveModule();
        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings",
                                UIPhotonConfiguration.programStateToUi(ConfigManager.getInstance().getConfig())));
    }

    void saveAndBroadcastSelective(WsContext originContext, String propertyName, Object value) {
        logger.trace("Broadcasting PSC mutation - " + propertyName + ": " + value);
        saveModule();

        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> subMap = new HashMap<>();
        subMap.put(propertyName, value);
        map.put("mutatePipelineSettings", subMap);

        DataChangeService.getInstance()
                .publishEvent(new OutgoingUIEvent<>("mutatePipeline", map, originContext));
    }

    public void setCameraNickname(String newName) {
        visionSource.getSettables().getConfiguration().nickname = newName;
        ntConsumer.updateCameraNickname(newName);
        inputFrameSaver.updateCameraNickname(newName);
        outputFrameSaver.updateCameraNickname(newName);

        // Push new data to the UI
        saveAndBroadcastAll();
    }

    public UICameraConfiguration toUICameraConfig() {
        var ret = new UICameraConfiguration();

        var config = visionSource.getCameraConfiguration();
        ret.matchedCameraInfo = config.matchedCameraInfo;
        ret.cameraPath = config.getDevicePath();
        ret.fov = visionSource.getSettables().getFOV();
        ret.isCSICamera = config.matchedCameraInfo.type() == CameraType.ZeroCopyPicam;
        ret.nickname = visionSource.getSettables().getConfiguration().nickname;
        ret.uniqueName = visionSource.getSettables().getConfiguration().uniqueName;
        ret.currentPipelineSettings =
                SerializationUtils.objectToHashMap(pipelineManager.getCurrentPipelineSettings());
        ret.currentPipelineIndex = pipelineManager.getRequestedIndex();
        ret.pipelineNicknames = pipelineManager.getPipelineNicknames();
        ret.cameraQuirks = visionSource.getSettables().getConfiguration().cameraQuirks;
        ret.minExposureRaw = visionSource.getSettables().getMinExposureRaw();
        ret.maxExposureRaw = visionSource.getSettables().getMaxExposureRaw();
        ret.minWhiteBalanceTemp = visionSource.getSettables().getMinWhiteBalanceTemp();
        ret.maxWhiteBalanceTemp = visionSource.getSettables().getMaxWhiteBalanceTemp();

        ret.deactivated = config.deactivated;

        // TODO refactor into helper method
        var temp = new HashMap<Integer, HashMap<String, Object>>();
        var videoModes = visionSource.getSettables().getAllVideoModes();

        for (var k : videoModes.keySet()) {
            var internalMap = new HashMap<String, Object>();

            internalMap.put("width", videoModes.get(k).width);
            internalMap.put("height", videoModes.get(k).height);
            internalMap.put("fps", videoModes.get(k).fps);
            internalMap.put(
                    "pixelFormat",
                    ((videoModes.get(k) instanceof LibcameraGpuSource.FPSRatedVideoMode)
                                    ? "kPicam"
                                    : videoModes.get(k).pixelFormat.toString())
                            .substring(1)); // Remove the k prefix

            temp.put(k, internalMap);
        }

        if (videoModes.size() == 0) {
            logger.error("no video modes, guhhhhh");
        }

        ret.videoFormatList = temp;
        ret.outputStreamPort = this.outputStreamPort;
        ret.inputStreamPort = this.inputStreamPort;

        ret.calibrations =
                visionSource.getSettables().getConfiguration().calibrations.stream()
                        .map(CameraCalibrationCoefficients::cloneWithoutObservations)
                        .toList();

        ret.isFovConfigurable =
                !(ConfigManager.getInstance().getConfig().getHardwareConfig().hasPresetFOV());

        ret.isConnected = visionSource.getFrameProvider().isConnected();
        ret.hasConnected = visionSource.getFrameProvider().hasConnected();

        return ret;
    }

    public CameraConfiguration getStateAsCameraConfig() {
        var config = visionSource.getSettables().getConfiguration();
        config.setPipelineSettings(pipelineManager.userPipelineSettings);
        config.driveModeSettings = pipelineManager.driverModePipeline.getSettings();
        config.currentPipelineIndex = Math.max(pipelineManager.getRequestedIndex(), -1);

        return config;
    }

    public void addResultConsumer(CVPipelineResultConsumer dataConsumer) {
        resultConsumers.add(dataConsumer);
    }

    private void consumeResult(CVPipelineResult result) {
        consumePipelineResult(result);

        // Pipelines like DriverMode and Calibrate3dPipeline have null output frames
        if (result.inputAndOutputFrame != null
                && (pipelineManager.getCurrentPipelineSettings()
                        instanceof AdvancedPipelineSettings settings)) {
            streamRunnable.updateData(result.inputAndOutputFrame, settings, result.targets);
            // The streamRunnable manages releasing in this case
        } else {
            consumeResults(result.inputAndOutputFrame, result.targets);

            result.release();
            // In this case we don't bother with a separate streaming thread and we release
        }
    }

    private void consumePipelineResult(CVPipelineResult result) {
        for (var dataConsumer : resultConsumers) {
            dataConsumer.accept(result);
        }
    }

    /** Consume stream/target results, no rate limiting applied */
    private void consumeResults(Frame frame, List<TrackedTarget> targets) {
        for (var c : streamResultConsumers) {
            c.accept(frame, targets);
        }
    }

    public void setTargetModel(TargetModel targetModel) {
        var pipelineSettings = pipelineManager.getCurrentPipeline().getSettings();
        if (pipelineSettings instanceof ReflectivePipelineSettings settings) {
            settings.targetModel = targetModel;
            saveAndBroadcastAll();
        } else {
            logger.error("Cannot set target model of non-reflective pipe! Ignoring...");
        }
    }

    public void addCalibrationToConfig(CameraCalibrationCoefficients newCalibration) {
        if (newCalibration != null) {
            logger.info("Got new calibration for " + newCalibration.unrotatedImageSize);
            visionSource.getSettables().addCalibration(newCalibration);
        } else {
            logger.error("Got null calibration?");
        }

        saveAndBroadcastAll();
    }

    /**
     * Add/remove quirks from the camera we're controlling
     *
     * @param quirksToChange map of true/false for quirks we should change
     */
    public void changeCameraQuirks(HashMap<CameraQuirk, Boolean> quirksToChange) {
        visionSource.getCameraConfiguration().cameraQuirks.updateQuirks(quirksToChange);
        visionSource.remakeSettables();
        saveAndBroadcastAll();
    }

    public String uniqueName() {
        return this.visionSource.cameraConfiguration.uniqueName;
    }

    public CameraConfiguration getCameraConfiguration() {
        return this.visionSource.cameraConfiguration;
    }
}
