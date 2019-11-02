package com.chameleonvision.vision.process;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.Orientation;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.VideoException;
import edu.wpi.first.networktables.*;
import org.opencv.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisionProcess implements Runnable {

    private final String cameraName;
    public final CameraProcess cameraProcess;
    // NetworkTables
    public NetworkTableEntry ntPipelineEntry;
    public NetworkTableEntry ntDriverModeEntry;
    private int ntDriveModeListenerID;
    private int ntPipelineListenerID;
    private NetworkTableEntry ntYawEntry;
    private NetworkTableEntry ntPitchEntry;
    private NetworkTableEntry ntDistanceEntry;
    private NetworkTableEntry ntTimeStampEntry;
    private NetworkTableEntry ntValidEntry;
    // chameleon specific
    private Pipeline currentPipeline;
    private CVProcess cvProcess;
    // pipeline process items
//    private List<MatOfPoint> foundContours = new ArrayList<>();
//    private List<MatOfPoint> filteredContours = new ArrayList<>();
//    private List<MatOfPoint> deSpeckledContours = new ArrayList<>();
//    private List<RotatedRect> groupedContours = new ArrayList<>();
    private Mat cameraInputMat = new Mat();
    private Mat hsvThreshMat = new Mat();
    private Mat streamOutputMat = new Mat();
    private long timeStamp = 0;

    public VisionProcess(CameraProcess cameraProcess) {

        // USBCamera settings
        cvProcess = new StandardCVProcess(cameraProcess.getCamVals());
        this.cameraProcess = cameraProcess; // new USBCameraProcess(cameraProcess);

        this.cameraName = cameraProcess.getCamName();

        initNT(NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + cameraProcess.getNickname()));
    }

    private void driverModeListener(EntryNotification entryNotification) {
        cameraProcess.setDriverMode(entryNotification.value.getBoolean());
    }

    private void pipelineListener(EntryNotification entryNotification) {
        var ntPipelineIndex = (int) entryNotification.value.getDouble();
        if (ntPipelineIndex >= cameraProcess.getPipelines().size()) {
            ntPipelineEntry.setNumber(cameraProcess.getCurrentPipelineIndex());
        } else {
            var pipeline = cameraProcess.getCurrentPipeline();
            cameraProcess.setCurrentPipelineIndex(ntPipelineIndex);
            try {
                cameraProcess.setExposure(pipeline.exposure);
            } catch (VideoException e) {
                System.err.println(e.toString());
            }
            cameraProcess.setBrightness(pipeline.brightness);
            if (SettingsManager.generalSettings.currentCamera.equals(cameraName)) {
                SettingsManager.generalSettings.currentPipeline = ntPipelineIndex;
                HashMap<String, Object> pipeChange = new HashMap<>();
                pipeChange.put("currentPipeline", ntPipelineIndex);
                ServerHandler.broadcastMessage(pipeChange);
                ServerHandler.sendFullSettings();

            }
        }
    }

    private void updateNetworkTables(PipelineResult pipelineResult) {
        if (pipelineResult.IsValid) {
            ntValidEntry.setBoolean(true);
            ntYawEntry.setNumber(pipelineResult.Yaw);
            ntPitchEntry.setNumber(pipelineResult.Pitch);
            ntDistanceEntry.setNumber(pipelineResult.Area);
            ntTimeStampEntry.setNumber(timeStamp);
            NetworkTableInstance.getDefault().flush();
        } else {
            ntYawEntry.setNumber(0.0);
            ntPitchEntry.setNumber(0.0);
            ntDistanceEntry.setNumber(0.0);
            ntTimeStampEntry.setNumber(timeStamp);
            ntValidEntry.setBoolean(false);
        }
    }

    private PipelineResult runVisionProcess(Mat inputImage, Mat outputImage) {
        return cvProcess.runPipeline(
                currentPipeline,
                inputImage,
                outputImage,
                cameraProcess.getCamVals(),
                currentPipeline.orientation.equals(Orientation.Inverted),
                cameraProcess.getDriverMode()
        );
    }

    @Override
    public void run() {
        // processing time tracking
        long startTime;
        long fpsLastTime = 0;
        double processTimeMs;
        double fps = 0;
        double uiFps = 0;
        int maxFps = cameraProcess.getVideoMode().fps;

        new Thread(cameraProcess).start();

        long lastFrameEndNanosec = 0;

        while (!Thread.interrupted()) {
            startTime = System.nanoTime();
            if ((startTime - lastFrameEndNanosec) * 1e-6 >= 1000.0 / (maxFps + 3)) { // 3 additional fps to allow for overhead
//                foundContours.clear();
//                filteredContours.clear();
//                groupedContours.clear();
//                deSpeckledContours.clear();

                // update FPS for ui only every 0.5 seconds
                if ((startTime - fpsLastTime) * 1e-6 >= 500) {
                    if (fps >= maxFps) {
                        uiFps = maxFps;
                    } else {
                        uiFps = fps;
                    }
                    fpsLastTime = System.nanoTime();
                }

                currentPipeline = cameraProcess.getCurrentPipeline();
                // start fps counter right before grabbing input frame
                timeStamp = cameraProcess.getLatestFrame(cameraInputMat);
                if (cameraInputMat.cols() == 0 && cameraInputMat.rows() == 0) {
                    continue;
                }

                // get vision data
                var pipelineResult = runVisionProcess(cameraInputMat, streamOutputMat);
                updateNetworkTables(pipelineResult);
                if (cameraName.equals(SettingsManager.generalSettings.currentCamera)) {
                    HashMap<String, Object> WebSend = new HashMap<>();
                    HashMap<String, Object> point = new HashMap<>();
                    HashMap<String, Object> calculated = new HashMap<>();
                    List<Double> center = new ArrayList<>();
                    if (pipelineResult.IsValid) {
                        center.add(pipelineResult.RawPoint.center.x);
                        center.add(pipelineResult.RawPoint.center.y);
                        calculated.put("pitch", pipelineResult.Pitch);
                        calculated.put("yaw", pipelineResult.Yaw);
                    } else {
                        center.add(0.0);
                        center.add(0.0);
                        calculated.put("pitch", 0);
                        calculated.put("yaw", 0);
                    }
                    point.put("fps", uiFps);
                    point.put("calculated", calculated);
                    point.put("rawPoint", center);
                    WebSend.put("point", point);
                    ServerHandler.broadcastMessage(WebSend);
                }

                cameraProcess.updateFrame(streamOutputMat);

                cameraInputMat.release();
                hsvThreshMat.release();

                // calculate FPS
                lastFrameEndNanosec = System.nanoTime();
                processTimeMs = (lastFrameEndNanosec - startTime) * 1e-6;
                fps = 1000 / processTimeMs;
                //please dont enable if you are not debugging
                //				System.out.printf("%s - Process time: %-5.2fms, FPS: %-5.2f, FoundContours: %d, FilteredContours: %d, GroupedContours: %d\n", cameraName, processTimeMs, fps, FoundContours.size(), FilteredContours.size(), GroupedContours.size());
            }
        }

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

    /**
     * Rebases the writing location for the vision process - pipeline output
     *
     * @param newTable the new writing location
     */
    private void initNT(NetworkTable newTable) {
        ntPipelineEntry = newTable.getEntry("pipeline");
        ntDriverModeEntry = newTable.getEntry("driver_mode");
        ntPitchEntry = newTable.getEntry("pitch");
        ntYawEntry = newTable.getEntry("yaw");
        ntDistanceEntry = newTable.getEntry("distance");
        ntTimeStampEntry = newTable.getEntry("timestamp");
        ntValidEntry = newTable.getEntry("is_valid");
        ntDriveModeListenerID = ntDriverModeEntry.addListener(this::driverModeListener, EntryListenerFlags.kUpdate);
        ntPipelineListenerID = ntPipelineEntry.addListener(this::pipelineListener, EntryListenerFlags.kUpdate);
        ntDriverModeEntry.setBoolean(false);
        ntPipelineEntry.setNumber(cameraProcess.getCurrentPipelineIndex());
    }
}
