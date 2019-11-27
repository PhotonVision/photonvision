package com.chameleonvision.vision;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.util.LoopingRunnable;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.camera.CameraStreamer;
import com.chameleonvision.vision.pipeline.*;
import com.chameleonvision.web.ServerHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.networktables.*;
import edu.wpi.first.wpiutil.CircularBuffer;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class VisionProcess {

    private final CameraCapture cameraCapture;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private final CameraStreamerRunnable streamRunnable;
    private final VisionProcessRunnable visionRunnable;
    public final CameraStreamer cameraStreamer;

    private CVPipeline currentPipeline;
    private int currentPipelineIndex = 0;

    private CVPipeline driverModePipeline = new DriverVisionPipeline(new CVPipelineSettings());

    private volatile CVPipelineResult lastPipelineResult;

    private BlockingQueue<Mat> streamFrameQueue = new LinkedBlockingDeque<>(1);

    // network table stuff
    private final NetworkTable defaultTable;
    private NetworkTableEntry ntPipelineEntry;
    private NetworkTableEntry ntDriverModeEntry;
    private int ntDriveModeListenerID;
    private int ntPipelineListenerID;
    private NetworkTableEntry ntYawEntry;
    private NetworkTableEntry ntPitchEntry;
    private NetworkTableEntry ntAuxListEntry;
    private NetworkTableEntry ntAreaEntry;
    private NetworkTableEntry ntTimeStampEntry;
    private NetworkTableEntry ntValidEntry;
    private ObjectMapper objectMapper;

    VisionProcess(CameraCapture cameraCapture, String name) {
        this.cameraCapture = cameraCapture;

        pipelines.add(new CVPipeline2d("New Pipeline"));
        setPipeline(0, false);

        // Thread to put frames on the dashboard
        this.cameraStreamer = new CameraStreamer(cameraCapture, name);
        this.streamRunnable = new CameraStreamerRunnable(30, cameraStreamer);

        // Thread to process vision data
        this.visionRunnable = new VisionProcessRunnable();

        // network table
        defaultTable = NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + cameraCapture.getProperties().name);
    }

    public void start() {
        System.out.println("Starting NetworkTables.");
        initNT(defaultTable);
        System.out.println("Starting vision thread.");
        new Thread(visionRunnable).start();
        System.out.println("Starting stream thread.");
        new Thread(streamRunnable).start();
    }

    /**
     * Removes the old value change listeners
     * calls {@link #initNT}
     *
     * @param newTable passed to {@link #initNT}
     */
    public void resetNT(NetworkTable newTable) {
        ntDriverModeEntry.removeListener(ntDriveModeListenerID);
        ntPipelineEntry.removeListener(ntPipelineListenerID);
        initNT(newTable);
    }

    private void initNT(NetworkTable newTable) {
        ntPipelineEntry = newTable.getEntry("pipeline");
        ntDriverModeEntry = newTable.getEntry("driver_mode");
        ntPitchEntry = newTable.getEntry("pitch");
        ntYawEntry = newTable.getEntry("yaw");
        ntAreaEntry = newTable.getEntry("area");
        ntTimeStampEntry = newTable.getEntry("timestamp");
        ntValidEntry = newTable.getEntry("is_valid");
        ntAuxListEntry = newTable.getEntry("aux_targets");
        ntDriveModeListenerID = ntDriverModeEntry.addListener(this::setDriverMode, EntryListenerFlags.kUpdate);
        ntPipelineListenerID = ntPipelineEntry.addListener(this::setPipeline, EntryListenerFlags.kUpdate);
        ntDriverModeEntry.setBoolean(false);
        ntPipelineEntry.setNumber(0);
    }

    private void setDriverMode(EntryNotification driverModeEntryNotification) {
        setDriverMode(driverModeEntryNotification.value.getBoolean());
    }

    public void setDriverMode(boolean driverMode) {
        if (driverMode) {
            setPipelineInternal(driverModePipeline);
        } else {
            setPipeline(currentPipelineIndex, true);
        }
    }

    /**
     * Method called by the nt entry listener to update the next pipeline.
     * @param notification the notification
     */
    private void setPipeline(EntryNotification notification) {
        var wantedPipelineIndex = (int) notification.value.getDouble();

        if (wantedPipelineIndex >= pipelines.size()) {
            ntPipelineEntry.setNumber(currentPipelineIndex);
        } else {
            currentPipelineIndex = wantedPipelineIndex;
            setPipeline(wantedPipelineIndex, true);
        }
    }

    public void setPipeline(int pipelineIndex, boolean updateUI) {
        CVPipeline newPipeline = pipelines.get(pipelineIndex);
        if (newPipeline != null) {
            setPipelineInternal(newPipeline);
            currentPipelineIndex = pipelineIndex;

            // update the configManager
            if(ConfigManager.settings.currentCamera.equals(cameraCapture.getProperties().name)) {
                ConfigManager.settings.currentPipeline = pipelineIndex;

                if (updateUI) {
                    HashMap<String, Object> pipeChange = new HashMap<>();
                    pipeChange.put("currentPipeline", pipelineIndex);
                    ServerHandler.broadcastMessage(pipeChange);
                    ServerHandler.sendFullSettings();
                }
            }
        }
    }

    private void setPipelineInternal(CVPipeline pipeline) {
        currentPipeline = pipeline;
        currentPipeline.initPipeline(cameraCapture);
    }

    private void updateUI(CVPipelineResult data) {
        if(cameraCapture.getProperties().name.equals(ConfigManager.settings.currentCamera)) {
            HashMap<String, Object> WebSend = new HashMap<>();
            HashMap<String, Object> point = new HashMap<>();
            HashMap<String, Object> calculated = new HashMap<>();
            List<Double> center = new ArrayList<>();
            if (data.hasTarget) {
                if(data instanceof CVPipeline2d.CVPipeline2dResult) {
                    CVPipeline2d.CVPipeline2dResult result = (CVPipeline2d.CVPipeline2dResult) data;
                    CVPipeline2d.Target2d bestTarget = result.targets.get(0);
                    center.add(bestTarget.rawPoint.center.x);
                    center.add(bestTarget.rawPoint.center.y);
                    calculated.put("pitch", bestTarget.pitch);
                    calculated.put("yaw", bestTarget.yaw);
                    calculated.put("area", bestTarget.area);
                } else if (data instanceof CVPipeline3d.CVPipeline3dResult) {
                    // TODO: (2.1) 3d stuff in UI
                } else {
                    center.add(0.0);
                    center.add(0.0);
                    calculated.put("pitch", 0);
                    calculated.put("yaw", 0);
                }
            } else {
                center.add(0.0);
                center.add(0.0);
                calculated.put("pitch", 0);
                calculated.put("yaw", 0);
            }
            point.put("fps", visionRunnable.fps);
            point.put("calculated", calculated);
            point.put("rawPoint", center);
            WebSend.put("point", point);
            ServerHandler.broadcastMessage(WebSend);
        }
    }

    private void updateNetworkTableData(CVPipelineResult data) {
        ntValidEntry.setBoolean(data.hasTarget);
        if(data.hasTarget && !(data instanceof DriverVisionPipeline.DriverPipelineResult)) {
            if(data instanceof CVPipeline2d.CVPipeline2dResult) {

                //noinspection unchecked
                List<CVPipeline2d.Target2d> targets = (List<CVPipeline2d.Target2d>) data.targets;
                ntTimeStampEntry.setDouble(data.imageTimestamp);
                ntPitchEntry.setDouble(targets.get(0).pitch);
                ntYawEntry.setDouble(targets.get(0).yaw);
                ntAreaEntry.setDouble(targets.get(0).area);
                try {
                    ntAreaEntry.setString(objectMapper.writeValueAsString(targets));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else if (data instanceof CVPipeline3d.CVPipeline3dResult) {
                // TODO: (2.1) 3d stuff...
            }
        } else {
            ntPitchEntry.setDouble(0.0);
            ntYawEntry.setDouble(0.0);
            ntAreaEntry.setDouble(0.0);
            ntTimeStampEntry.setDouble(0.0);
            ntAuxListEntry.setString("");
        }
    }

    public void setVideoMode(VideoMode newMode) {
        cameraCapture.setVideoMode(newMode);
        cameraStreamer.setNewVideoMode(newMode);
    }

    public List<CVPipeline> getPipelines() {
        return pipelines;
    }

    public CVPipeline getCurrentPipeline() {
        return currentPipeline;
    }

    public int getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    public void addPipeline() {
        // TODO: (2.1) add to UI option between 2d and 3d pipeline
        pipelines.add(new CVPipeline2d());
    }

    public void addPipeline(CVPipeline pipeline) {
        pipelines.add(pipeline);
    }

    public void addPipeline(CVPipelineSettings settings) {
        if (settings instanceof CVPipeline2dSettings) {
            pipelines.add(new CVPipeline2d((CVPipeline2dSettings) settings));
        }
    }

    public CameraCapture getCamera() {
        return cameraCapture;
    }

    public boolean getDriverMode() {
        return (currentPipeline == driverModePipeline);
    }

    public void setDriverModeSettings(CVPipelineSettings settings) {

        driverModePipeline.settings = settings;
    }

    public CVPipelineSettings getDriverModeSettings() {
        return driverModePipeline.settings;
    }

    public CVPipeline getPipelineByIndex(int pipelineIndex) {
        return pipelines.get(pipelineIndex);
    }

    /**
     * VisionProcessRunnable will process images as quickly as possible
     */
    private class VisionProcessRunnable implements Runnable {

        volatile Double fps = 0.0;
        private CircularBuffer fpsAveragingBuffer = new CircularBuffer(7);
        private Mat streamBuffer = new Mat();

        @Override
        public void run() {
            var lastUpdateTimeNanos = System.nanoTime();
            while(!Thread.interrupted()) {

                // blocking call, will block until camera has a new frame.
                Pair<Mat, Long> camData = cameraCapture.getFrame();

                Mat camFrame = camData.getLeft();
                if (camFrame.cols() > 0 && camFrame.rows() > 0) {
                    CVPipelineResult result = currentPipeline.runPipeline(camFrame);

                    if (result != null) {
                        result.setTimestamp(camData.getRight());
                        lastPipelineResult = result;
                        updateNetworkTableData(lastPipelineResult);
                        updateUI(lastPipelineResult);
                    }
                }

                try {
                    streamFrameQueue.add(lastPipelineResult.outputMat);
                } catch (Exception e) {
                    System.out.println("Vision running faster than stream");
                }

                var deltaTimeNanos = lastUpdateTimeNanos - System.nanoTime();
                fpsAveragingBuffer.addFirst(1.0 / (deltaTimeNanos * 1E-09));
                lastUpdateTimeNanos = System.nanoTime();
                fps = getAverageFPS();
            }
        }

        double getAverageFPS() {
            var temp = 0.0;
            for(int i = 0; i < 7; i++) {
                temp += fpsAveragingBuffer.get(i);
            }
            temp /= 7.0;
            return temp;
        }

    }

    private class CameraStreamerRunnable extends LoopingRunnable {

        final CameraStreamer streamer;

        private CameraStreamerRunnable(int cameraFPS, CameraStreamer streamer) {
            // add 2 FPS to allow for a bit of overhead
            super(1000L/(cameraFPS + 2));
            this.streamer = streamer;
        }

        @Override
        protected void process() {
            try {
                if (!streamFrameQueue.isEmpty()) {
                    Mat latestMat = streamFrameQueue.take();
                    if (!latestMat.empty()) {
                        streamer.runStream(latestMat);
                    } else {
                        System.out.println("stream mat empty");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
