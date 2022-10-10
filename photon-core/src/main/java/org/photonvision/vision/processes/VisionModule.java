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

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
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
import org.photonvision.common.util.java.TriConsumer;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.camera.ZeroCopyPicamSource;
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
    private static final int streamFPSCap = 30;

    private final Logger logger;
    protected final PipelineManager pipelineManager;
    protected final VisionSource visionSource;
    private final VisionRunner visionRunner;
    private final StreamRunnable streamRunnable;
    private final LinkedList<CVPipelineResultConsumer> resultConsumers = new LinkedList<>();
    private final LinkedList<CVPipelineResultConsumer> fpsLimitedResultConsumers = new LinkedList<>();
    // Raw result consumers run before any drawing has been done by the OutputStreamPipeline
    private final LinkedList<TriConsumer<Frame, Frame, List<TrackedTarget>>> rawResultConsumers =
            new LinkedList<>();
    private final NTDataPublisher ntConsumer;
    private final UIDataPublisher uiDataConsumer;
    protected final int moduleIndex;
    protected final QuirkyCamera cameraQuirks;

    private long lastFrameConsumeMillis;
    protected TrackedTarget lastPipelineResultBestTarget;

    MJPGFrameConsumer dashboardInputStreamer;
    MJPGFrameConsumer dashboardOutputStreamer;

    FileSaveFrameConsumer inputFrameSaver;
    FileSaveFrameConsumer outputFrameSaver;

    public VisionModule(PipelineManager pipelineManager, VisionSource visionSource, int index) {
        logger =
                new Logger(
                        VisionModule.class,
                        visionSource.getSettables().getConfiguration().nickname,
                        LogGroup.VisionModule);

        // Find quirks for the current camera
        if (visionSource instanceof USBCameraSource) {
            cameraQuirks = ((USBCameraSource) visionSource).cameraQuirks;
        } else if (visionSource instanceof ZeroCopyPicamSource) {
            cameraQuirks = QuirkyCamera.ZeroCopyPiCamera;
        } else {
            cameraQuirks = QuirkyCamera.DefaultCamera;
        }

        // We don't show gain if the config says it's -1. So check here to make sure it's non-negative
        // if it _is_ supported
        if (cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
            pipelineManager.userPipelineSettings.forEach(
                    it -> {
                        if (it.cameraGain == -1) it.cameraGain = 20; // Sane default
                    });
        }
        if (cameraQuirks.hasQuirk(CameraQuirk.AWBGain)) {
            pipelineManager.userPipelineSettings.forEach(
                    it -> {
                        if (it.cameraRedGain == -1) it.cameraRedGain = 16; // Sane defaults
                        if (it.cameraBlueGain == -1) it.cameraBlueGain = 16;
                    });
        }

        this.pipelineManager = pipelineManager;
        this.visionSource = visionSource;
        this.visionRunner =
                new VisionRunner(
                        this.visionSource.getFrameProvider(),
                        this.pipelineManager::getCurrentUserPipeline,
                        this::consumeResult,
                        this.cameraQuirks);
        this.streamRunnable = new StreamRunnable(new OutputStreamPipeline());
        this.moduleIndex = index;

        DataChangeService.getInstance().addSubscriber(new VisionModuleChangeSubscriber(this));

        createStreams();

        recreateFpsLimitedResultConsumers();

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
                        visionSource.getSettables().getConfiguration().uniqueName + "-input", inputStreamPort);

        inputFrameSaver =
                new FileSaveFrameConsumer(visionSource.getSettables().getConfiguration().nickname, "input");
        outputFrameSaver =
                new FileSaveFrameConsumer(
                        visionSource.getSettables().getConfiguration().nickname, "output");
    }

    private void recreateFpsLimitedResultConsumers() {
        // Important! These must come before the stream result consumers because the stream result
        // consumers release the frame
        rawResultConsumers.add((in, out, tgts) -> inputFrameSaver.accept(in));
        fpsLimitedResultConsumers.add(result -> outputFrameSaver.accept(result.outputFrame));

        fpsLimitedResultConsumers.add(
                result -> {
                    if (this.pipelineManager.getCurrentPipelineSettings().inputShouldShow)
                        dashboardInputStreamer.accept(result.inputFrame);
                    else dashboardInputStreamer.disabledTick();
                });
        fpsLimitedResultConsumers.add(
                result -> {
                    if (this.pipelineManager.getCurrentPipelineSettings().outputShouldShow)
                        dashboardOutputStreamer.accept(result.outputFrame);
                    else dashboardInputStreamer.disabledTick();
                    ;
                });
    }

    private class StreamRunnable extends Thread {
        private final OutputStreamPipeline outputStreamPipeline;

        private final Object frameLock = new Object();
        private Frame inputFrame, outputFrame;
        private AdvancedPipelineSettings settings = new AdvancedPipelineSettings();
        private List<TrackedTarget> targets = new ArrayList<>();

        private boolean shouldRun = false;

        public StreamRunnable(OutputStreamPipeline outputStreamPipeline) {
            this.outputStreamPipeline = outputStreamPipeline;
        }

        public void updateData(
                Frame inputFrame,
                Frame outputFrame,
                AdvancedPipelineSettings settings,
                List<TrackedTarget> targets) {
            synchronized (frameLock) {
                if (shouldRun && this.inputFrame != null && this.outputFrame != null) {
                    logger.trace("Fell behind; releasing last unused Mats");
                    this.inputFrame.release();
                    this.outputFrame.release();
                }

                this.inputFrame = inputFrame;
                this.outputFrame = outputFrame;
                this.settings = settings;
                this.targets = targets;

                shouldRun =
                        inputFrame != null
                                && !inputFrame.image.getMat().empty()
                                && outputFrame != null
                                && !outputFrame.image.getMat().empty();
            }
        }

        @Override
        public void run() {
            while (true) {
                final Frame inputFrame, outputFrame;
                final AdvancedPipelineSettings settings;
                final List<TrackedTarget> targets;
                final boolean shouldRun;
                synchronized (frameLock) {
                    inputFrame = this.inputFrame;
                    outputFrame = this.outputFrame;
                    this.inputFrame = null;
                    this.outputFrame = null;

                    settings = this.settings;
                    targets = this.targets;
                    shouldRun = this.shouldRun;

                    this.shouldRun = false;
                }
                if (shouldRun) {
                    consumeRawResults(inputFrame, outputFrame, targets);
                    try {
                        CVPipelineResult osr =
                                outputStreamPipeline.process(inputFrame, outputFrame, settings, targets);

                        consumeFpsLimitedResult(osr);
                    } catch (Exception e) {
                        // Never die
                        logger.error("Exception while running stream runnable!", e);
                    }
                    try {
                        inputFrame.release();
                        outputFrame.release();
                    } catch (Exception e) {
                        logger.error("Exception freeing frames", e);
                    }
                } else {
                    // busy wait! hurray!
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void start() {
        visionRunner.startProcess();
        streamRunnable.start();
    }

    public void setFovAndPitch(double fov, Rotation2d pitch) {
        var settables = visionSource.getSettables();
        logger.trace(() -> "Setting " + settables.getConfiguration().nickname + ") FOV (" + fov + ")");

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

    void changePipelineType(int newType) {
        pipelineManager.changePipelineType(newType);
        setPipeline(pipelineManager.getCurrentPipelineIndex());
        saveAndBroadcastAll();
    }

    void setDriverMode(boolean isDriverMode) {
        pipelineManager.setDriverMode(isDriverMode);
        setVisionLEDs(!isDriverMode);
        setPipeline(
                isDriverMode
                        ? PipelineManager.DRIVERMODE_INDEX
                        : pipelineManager.getCurrentPipelineIndex());
        saveAndBroadcastAll();
    }

    public void startCalibration(UICalibrationData data) {
        var settings = pipelineManager.calibration3dPipeline.getSettings();
        pipelineManager.calibration3dPipeline.deleteSavedImages();
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
        if (!cameraQuirks.hasQuirk(CameraQuirk.AWBGain)) {
            settings.cameraRedGain = -1;
            settings.cameraBlueGain = -1;
        }

        settings.cameraAutoExposure = true;

        setPipeline(PipelineManager.CAL_3D_INDEX);
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
        } else {
            logger.error("Calibration failed...");
        }
        saveAndBroadcastAll();
        return ret;
    }

    void setPipeline(int index) {
        logger.info("Setting pipeline to " + index);
        pipelineManager.setIndex(index);
        var pipelineSettings = pipelineManager.getPipelineSettings(index);

        if (pipelineSettings == null) {
            logger.error("Config for index " + index + " was null!");
            return;
        }

        visionSource.getSettables().setVideoModeInternal(pipelineSettings.cameraVideoModeIndex);
        visionSource.getSettables().setBrightness(pipelineSettings.cameraBrightness);
        visionSource.getSettables().setGain(pipelineSettings.cameraGain);

        // If manual exposure, force exposure slider to be valid
        if (!pipelineSettings.cameraAutoExposure) {
            if (pipelineSettings.cameraExposure < 0)
                pipelineSettings.cameraExposure = 10; // reasonable default
        }

        visionSource.getSettables().setExposure(pipelineSettings.cameraExposure);
        visionSource.getSettables().setAutoExposure(pipelineSettings.cameraAutoExposure);

        if (cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
            // If the gain is disabled for some reason, re-enable it
            if (pipelineSettings.cameraGain == -1) pipelineSettings.cameraGain = 20;
            visionSource.getSettables().setGain(Math.max(0, pipelineSettings.cameraGain));
        } else {
            pipelineSettings.cameraGain = -1;
        }

        if (cameraQuirks.hasQuirk(CameraQuirk.AWBGain)) {
            // If the AWB gains are disabled for some reason, re-enable it
            if (pipelineSettings.cameraRedGain == -1) pipelineSettings.cameraRedGain = 16;
            if (pipelineSettings.cameraBlueGain == -1) pipelineSettings.cameraBlueGain = 16;
            visionSource.getSettables().setRedGain(Math.max(0, pipelineSettings.cameraRedGain));
            visionSource.getSettables().setBlueGain(Math.max(0, pipelineSettings.cameraBlueGain));
        } else {
            pipelineSettings.cameraRedGain = -1;
            pipelineSettings.cameraBlueGain = -1;
        }

        setVisionLEDs(pipelineSettings.ledMode);

        visionSource.getSettables().getConfiguration().currentPipelineIndex =
                pipelineManager.getCurrentPipelineIndex();
    }

    private boolean camShouldControlLEDs() {
        // Heuristic - if the camera has a known FOV or is a piCam, assume it's in use for
        // vision processing, and should command stuff to the LED's.
        // TODO: Make LED control a property of the camera itself and controllable in the UI.
        return isVendorCamera() || cameraQuirks.hasQuirk(CameraQuirk.PiCam);
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

    public void setCameraNickname(String newName) {
        visionSource.getSettables().getConfiguration().nickname = newName;
        ntConsumer.updateCameraNickname(newName);
        inputFrameSaver.updateCameraNickname(newName);
        outputFrameSaver.updateCameraNickname(newName);

        // Rename streams
        fpsLimitedResultConsumers.clear();

        // Teardown and recreate streams
        destroyStreams();
        createStreams();

        // Rebuild streamers
        recreateFpsLimitedResultConsumers();

        // Push new data to the UI
        saveAndBroadcastAll();
    }

    public PhotonConfiguration.UICameraConfiguration toUICameraConfig() {
        var ret = new PhotonConfiguration.UICameraConfiguration();

        ret.fov = visionSource.getSettables().getFOV();
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
            internalMap.put(
                    "pixelFormat",
                    ((videoModes.get(k) instanceof ZeroCopyPicamSource.FPSRatedVideoMode)
                                    ? "kPicam"
                                    : videoModes.get(k).pixelFormat.toString())
                            .substring(1)); // Remove the k prefix

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

        // Pipelines like DriverMode and Calibrate3dPipeline have null output frames
        if (result.inputFrame != null
                && (pipelineManager.getCurrentPipelineSettings() instanceof AdvancedPipelineSettings)) {
            streamRunnable.updateData(
                    result.inputFrame,
                    result.outputFrame,
                    (AdvancedPipelineSettings) pipelineManager.getCurrentPipelineSettings(),
                    result.targets);
            // The streamRunnable manages releasing in this case
        } else {
            consumeFpsLimitedResult(result);

            result.release();
            // In this case we don't bother with a separate streaming thread and we release
        }
    }

    private void consumePipelineResult(CVPipelineResult result) {
        for (var dataConsumer : resultConsumers) {
            dataConsumer.accept(result);
        }
    }

    private void consumeFpsLimitedResult(CVPipelineResult result) {
        long dt = System.currentTimeMillis() - lastFrameConsumeMillis;
        if (dt > 1000 / streamFPSCap) {
            for (var c : fpsLimitedResultConsumers) {
                c.accept(result);
            }
            lastFrameConsumeMillis = System.currentTimeMillis();
        }
    }

    /** Consume results prior to drawing on them. */
    private void consumeRawResults(Frame inputFrame, Frame outputFrame, List<TrackedTarget> targets) {
        for (var c : rawResultConsumers) {
            c.accept(inputFrame, outputFrame, targets);
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
