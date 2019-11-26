package com.chameleonvision.vision;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.util.LoopingRunnable;
import com.chameleonvision.vision.camera.CameraCapture;
import com.chameleonvision.vision.camera.CameraStreamer;
import com.chameleonvision.vision.pipeline.*;
import com.chameleonvision.web.ServerHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.networktables.*;
import edu.wpi.first.wpiutil.CircularBuffer;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisionProcess {

    private final CameraCapture cameraCapture;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private final CameraStreamerRunnable streamRunnable;
    private final VisionProcessRunnable visionRunnable;
    public final CameraStreamer cameraStreamer;

    private CVPipeline currentPipeline;
    private int currentPipelineIndex = 0;

    private final CVPipelineSettings driverModeSettings = new CVPipelineSettings();
    private CVPipeline driverModePipeline = new DriverVisionPipeline(driverModeSettings);

    private volatile CVPipelineResult lastPipelineResult;

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
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
                ntAuxListEntry.setString(gson.toJson(targets));

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

    public CameraCapture getCamera() {
        return cameraCapture;
    }

    public boolean getDriverMode() {
        return (currentPipeline == driverModePipeline);
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

    private static class CameraStreamerRunnable extends LoopingRunnable {

        final CameraStreamer streamer;
        private Mat streamBuffer = new Mat();

        private CameraStreamerRunnable(int cameraFPS, CameraStreamer streamer) {
            // add 2 FPS to allow for a bit of overhead
            // TODO: (low) test the effect of this
//            super(1000L/(cameraFPS + 2));
            super(10L);
            this.streamer = streamer;
        }

        @Override
        protected void process() {
//            System.out.println("running camera streamer");
//            Mat latestMat = lastPipelineResult.outputMat; //visionRunnable.result;
//            if (latestMat != null && latestMat.cols() > 0) {
//                latestMat.copyTo(streamBuffer);
//                streamer.runStream(streamBuffer);
//                streamBuffer.release();
//                if (toStreamMat != null && toStreamMat.cols() > 0) {
//                } else {
//                    System.out.println("fuuuuck");
//                }
//            }
        }
    }
}
