package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import com.chameleonvision.classabstraction.camera.CameraStreamer;
import com.chameleonvision.classabstraction.config.ConfigManager;
import com.chameleonvision.classabstraction.pipeline.*;
import com.chameleonvision.classabstraction.util.LoopingRunnable;
import com.chameleonvision.web.ServerHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisionProcess {

    private final CameraProcess cameraProcess;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private final CameraFrameRunnable cameraRunnable;
    private final CameraStreamerRunnable streamRunnable;
    private final VisionProcessRunnable visionRunnable;
    private final CameraStreamer cameraStreamer;
    private CVPipeline currentPipeline;
    private int currentPipelineIndex = 0;

    private final CVPipelineSettings driverVisionSettings = new CVPipelineSettings();

    // shitty stuff
    private volatile Mat lastCameraFrame = new Mat();
    private volatile boolean hasUnprocessedFrame = true;
    private volatile CVPipelineResult lastPipelineResult;

    // network table stuff
    public NetworkTableEntry ntPipelineEntry;
    public NetworkTableEntry ntDriverModeEntry;
    private int ntDriveModeListenerID;
    private int ntPipelineListenerID;
    private NetworkTableEntry ntYawEntry;
    private NetworkTableEntry ntPitchEntry;
    private NetworkTableEntry ntAuxListEntry;
    private NetworkTableEntry ntDistanceEntry;
    private NetworkTableEntry ntTimeStampEntry;
    private NetworkTableEntry ntValidEntry;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public VisionProcess(CameraProcess cameraProcess, String name) {
        this.cameraProcess = cameraProcess;

        pipelines.add(new DriverVisionPipeline(() -> driverVisionSettings));
        setPipeline(0);

        // Thread to grab frames from the camera
        // TODO: fix video modes!!!
        // TODO: FIX FPS!!!!!!!
        this.cameraRunnable = new CameraFrameRunnable(cameraProcess.getProperties().videoModes.get(0).fps);

        lastPipelineResult = new DriverVisionPipeline.DriverPipelineResult(
                null, cameraRunnable.getFrame(new Mat()), 0
        );

        // Thread to put frames on the dashboard
        this.cameraStreamer = new CameraStreamer(cameraProcess, name);
        this.streamRunnable = new CameraStreamerRunnable(30, cameraStreamer);

        // Thread to process vision data
        this.visionRunnable = new VisionProcessRunnable();
    }

    public void start() {
        System.out.println("Starting camera thread.");
        new Thread(cameraRunnable).start();
        while (cameraRunnable.cameraFrame == null) {
            try {
                if (lastCameraFrame.cols() > 0) break;
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
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
        ntDistanceEntry = newTable.getEntry("distance");
        ntTimeStampEntry = newTable.getEntry("timestamp");
        ntValidEntry = newTable.getEntry("is_valid");
        ntDriveModeListenerID = ntDriverModeEntry.addListener(this::setDriverMode, EntryListenerFlags.kUpdate);
        ntPipelineListenerID = ntPipelineEntry.addListener(this::setPipeline, EntryListenerFlags.kUpdate);
        ntDriverModeEntry.setBoolean(false);
        ntPipelineEntry.setNumber(0);
    }

    private void setDriverMode(EntryNotification ignored) {
        setPipeline(0);
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
            setPipeline(wantedPipelineIndex);
        }
    }

    public void setPipeline(int pipelineIndex) {

        CVPipeline newPipeline = pipelines.get(pipelineIndex);
        if (newPipeline != null) {
            setPipelineInternal(newPipeline);
        }

        // update the configManager
        if(ConfigManager.settings.currentCamera.equals(cameraProcess.getProperties().name)) {
            ConfigManager.settings.currentPipeline = pipelineIndex;
            HashMap<String, Object> pipeChange = new HashMap<>();
            pipeChange.put("currentPipeline", pipelineIndex);
            ServerHandler.broadcastMessage(pipeChange);
            ServerHandler.sendFullSettings();
        }

    }

    private void setPipelineInternal(CVPipeline pipeline) {
        pipelines.add(pipeline);
        currentPipeline = pipeline;
        currentPipeline.initPipeline(cameraProcess);
    }

    private void updateUI(CVPipelineResult data) {
        if(cameraProcess.getProperties().name.equals(ConfigManager.settings.currentCamera)) {
            HashMap<String, Object> WebSend = new HashMap<>();
            HashMap<String, Object> point = new HashMap<>();
            HashMap<String, Object> calculated = new HashMap<>();
            List<Double> center = new ArrayList<>();
            if (data.hasTarget) {

                if(data instanceof CVPipeline2d.CVPipeline2dResult) {
                    CVPipeline2d.CVPipeline2dResult result = (CVPipeline2d.CVPipeline2dResult) data;
                    CVPipeline2d.Target bestTarget = result.targets.get(0);
                    center.add(bestTarget.rawPoint.center.x);
                    center.add(bestTarget.rawPoint.center.y);
                    calculated.put("pitch", bestTarget.pitch);
                    calculated.put("yaw", bestTarget.yaw);
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
                ntTimeStampEntry.setDouble(data.processTime);

                //noinspection unchecked
                List<CVPipeline2d.Target> targets = (List<CVPipeline2d.Target>) data.targets;
                ntPitchEntry.setDouble(targets.get(0).pitch);
                ntYawEntry.setDouble(targets.get(0).yaw);
                ntDistanceEntry.setDouble(targets.get(0).area);
                ntAuxListEntry.setString(gson.toJson(targets));

            } else if(data instanceof CVPipeline3d.CVPipeline3dResult) {
                // TODO implement
            }
        } else {
            ntPitchEntry.setDouble(0.0);
            ntYawEntry.setDouble(0.0);
            ntDistanceEntry.setDouble(0.0);
            ntTimeStampEntry.setDouble(0.0);
        }
    }

    public void setVideoMode(VideoMode newMode) {
        cameraProcess.setVideoMode(newMode);
        cameraStreamer.setNewVideoMode(newMode);
    }

    public CVPipeline getCurrentPipeline() {
        return currentPipeline;
    }

    /**
     * CameraFrameRunnable grabs images from the cameraProcess
     * at a specified loopTime
     */
    protected class CameraFrameRunnable extends LoopingRunnable {
        private Mat cameraFrame;
        private long timestampMicros;

        private final Object frameLock = new Object();

        /**
         * CameraFrameRunnable grabs images from the cameraProcess
         * at a specified framerate
         * @param cameraFPS FPS of camera
         */
        CameraFrameRunnable(int cameraFPS) {
            // add 2 FPS to allow for a bit of overhead
            // TODO: test the affect of this
            super(1000L/(cameraFPS + 2));
        }

        @Override
        public void process() {
            System.out.println("running camera grabber process");

            // Grab camera frames
            var camData = cameraProcess.getFrame();
            if (camData.getLeft().cols() > 0) {
//                    System.out.println("grabbing frame");
//                    synchronized (frameLock) {
//                        cameraFrame = camData.getLeft();
//                    }
                timestampMicros = camData.getRight();
                camData.getLeft().copyTo(lastCameraFrame);
                hasUnprocessedFrame = true;

            }
        }

        public Mat getFrame(Mat dst) {
            if (cameraFrame != null) {
                dst = cameraFrame;
            } else {
                System.out.println("no frame");
            }
            return dst;
        }

    }

    /**
     * VisionProcessRunnable will process images as quickly as possible
     */
    private class VisionProcessRunnable implements Runnable {

        public Double fps = 0.0; // TODO update or average or something
        private CVPipelineResult result;
        private Mat streamBuffer = new Mat();

        @Override
        public void run() {
            while(!Thread.interrupted()) {
                System.out.println("running vision process");

                while(!hasUnprocessedFrame) {
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                lastCameraFrame.copyTo(streamBuffer); // = //cameraRunnable.getFrame(streamBuffer);
                hasUnprocessedFrame = false;

                if (streamBuffer.cols() > 0 && streamBuffer.rows() > 0) {
                    result = currentPipeline.runPipeline(streamBuffer);
                    lastPipelineResult = result;

                    updateNetworkTableData(lastPipelineResult);
                    updateUI(lastPipelineResult);

                } else {
//                    System.err.println("Bad streambuffer mat");
                }
                // TODO do something with the result
            }
        }
    }

    private class CameraStreamerRunnable extends LoopingRunnable {

        private final CameraStreamer streamer;
        private Mat streamBuffer = new Mat();

        private CameraStreamerRunnable(int cameraFPS, CameraStreamer streamer) {
            // add 2 FPS to allow for a bit of overhead
            // TODO: test the affect of this
            super(1000L/(cameraFPS + 2));
            this.streamer = streamer;
        }

        @Override
        protected void process() {
            System.out.println("running camera streamer");
            Mat latestMat = lastPipelineResult.outputMat; //visionRunnable.result;
            if (latestMat != null && latestMat.cols() > 0) {
                latestMat.copyTo(streamBuffer);
                streamer.runStream(streamBuffer);
//                if (toStreamMat != null && toStreamMat.cols() > 0) {
//                } else {
//                    System.out.println("fuuuuck");
//                }
            }
        }
    }
}
