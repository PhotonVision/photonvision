package com.chameleonvision.vision.process;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.CalibrationMode;
import com.chameleonvision.vision.Orientation;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.VideoException;
import edu.wpi.first.networktables.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VisionProcess implements Runnable {

    private final Camera camera;
    private final String cameraName;
    private final CameraProcess cameraProcess;
    // NetworkTables
    public NetworkTableEntry ntPipelineEntry;
    public NetworkTableEntry ntDriverModeEntry;
    private NetworkTableEntry ntYawEntry;
    private NetworkTableEntry ntPitchEntry;
    private NetworkTableEntry ntDistanceEntry;
    private NetworkTableEntry ntTimeStampEntry;
    private NetworkTableEntry ntValidEntry;
    // chameleon specific
    private Pipeline currentPipeline;
    private CVProcess cvProcess;
    // pipeline process items
    private List<MatOfPoint> foundContours = new ArrayList<>();
    private List<MatOfPoint> filteredContours = new ArrayList<>();
    private List<MatOfPoint> deSpeckledContours = new ArrayList<>();
    private List<RotatedRect> groupedContours = new ArrayList<>();
    private Mat cameraInputMat = new Mat();
    private Mat hsvThreshMat = new Mat();
    private Mat streamOutputMat = new Mat();
    private Scalar contourRectColor = new Scalar(255, 0, 0);
    private Scalar BoxRectColor = new Scalar(0, 0, 233);
    private long timeStamp = 0;

    public VisionProcess(Camera processCam) {
        camera = processCam;
        this.cameraName = camera.name;

        // NetworkTables
        NetworkTable ntTable = NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + cameraName);
        ntPipelineEntry = ntTable.getEntry("pipeline");
        ntDriverModeEntry = ntTable.getEntry("driver_mode");
        ntPitchEntry = ntTable.getEntry("pitch");
        ntYawEntry = ntTable.getEntry("yaw");
        ntDistanceEntry = ntTable.getEntry("distance");
        ntTimeStampEntry = ntTable.getEntry("timestamp");
        ntValidEntry = ntTable.getEntry("is_valid");
        ntDriverModeEntry.addListener(this::driverModeListener, EntryListenerFlags.kUpdate);
        ntPipelineEntry.addListener(this::pipelineListener, EntryListenerFlags.kUpdate);
        ntDriverModeEntry.setBoolean(false);
        ntPipelineEntry.setNumber(camera.getCurrentPipelineIndex());

        // camera settings
        cvProcess = new CVProcess(camera.getCamVals());
        cameraProcess = new CameraProcess(camera);
    }

    private void driverModeListener(EntryNotification entryNotification) {
        if (entryNotification.value.getBoolean()) {
            camera.setExposure(25);
            camera.setBrightness(15);
        } else {
            Pipeline pipeline = camera.getCurrentPipeline();
            camera.setExposure(pipeline.exposure);
            camera.setBrightness(pipeline.brightness);
        }
    }

    private void pipelineListener(EntryNotification entryNotification) {
        var ntPipelineIndex = (int) entryNotification.value.getDouble();
        if (ntPipelineIndex >= camera.getPipelines().size()){
            ntPipelineEntry.setNumber(camera.getCurrentPipelineIndex());
        } else{
            var pipeline = camera.getCurrentPipeline();
            camera.setCurrentPipelineIndex(ntPipelineIndex);
            try {
                camera.setExposure(pipeline.exposure);
            } catch (VideoException e) {
                System.err.println(e.toString());
            }
            camera.setBrightness(pipeline.brightness);
            if (SettingsManager.GeneralSettings.currentCamera.equals(cameraName)) {
                SettingsManager.GeneralSettings.currentPipeline = ntPipelineIndex;
                HashMap<String, Object> pipeChange = new HashMap<>();
                pipeChange.put("currentPipeline", ntPipelineIndex);
                ServerHandler.broadcastMessage(pipeChange);
                ServerHandler.sendFullSettings();

            }
        }
    }

    private void drawContour(Mat inputMat, RotatedRect contourRect) {
        if (contourRect == null) return;
        List<MatOfPoint> drawnContour = new ArrayList<>();
        Point[] vertices = new Point[4];
        contourRect.points(vertices);
        MatOfPoint contour = new MatOfPoint(vertices);
        drawnContour.add(contour);
        Rect box = Imgproc.boundingRect(contour);
        Imgproc.drawContours(inputMat, drawnContour, 0, contourRectColor, 3);
        Imgproc.circle(inputMat, contourRect.center, 3, contourRectColor);
        Imgproc.rectangle(inputMat, new Point(box.x, box.y), new Point((box.x + box.width), (box.y + box.height)), BoxRectColor, 2);
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
        var pipelineResult = new PipelineResult();

        if (currentPipeline == null) {
            return pipelineResult;
        }
        if (currentPipeline.orientation.equals(Orientation.Inverted)) {
            Core.flip(inputImage, inputImage, -1);
        }
        if (ntDriverModeEntry.getBoolean(false)) {
            inputImage.copyTo(outputImage);
            return pipelineResult;
        }
        Scalar hsvLower = new Scalar(currentPipeline.hue.get(0).intValue(), currentPipeline.saturation.get(0).intValue(), currentPipeline.value.get(0).intValue());
        Scalar hsvUpper = new Scalar(currentPipeline.hue.get(1).intValue(), currentPipeline.saturation.get(1).intValue(), currentPipeline.value.get(1).intValue());

        cvProcess.hsvThreshold(inputImage, hsvThreshMat, hsvLower, hsvUpper, currentPipeline.erode, currentPipeline.dilate);

        if (currentPipeline.isBinary) {
            Imgproc.cvtColor(hsvThreshMat, outputImage, Imgproc.COLOR_GRAY2BGR, 3);
        } else {
            inputImage.copyTo(outputImage);
        }
        foundContours = cvProcess.findContours(hsvThreshMat);
        if (foundContours.size() > 0) {
            filteredContours = cvProcess.filterContours(foundContours, currentPipeline.area, currentPipeline.ratio, currentPipeline.extent);
            if (filteredContours.size() > 0) {
                deSpeckledContours = cvProcess.rejectSpeckles(filteredContours, currentPipeline.speckle.doubleValue());
                if (deSpeckledContours.size() > 0){
                    groupedContours = cvProcess.groupTargets(deSpeckledContours, currentPipeline.targetIntersection, currentPipeline.targetGroup);
                    if (groupedContours.size() > 0) {
                        var finalRect = cvProcess.sortTargetsToOne(groupedContours, currentPipeline.sortMode);
                        pipelineResult.RawPoint = finalRect;
                        pipelineResult.IsValid = true;
                            switch (currentPipeline.calibrationMode){
                                case None:
                                    ///use the center of the camera to find the pitch and yaw difference
                                    pipelineResult.CalibratedX = camera.getCamVals().CenterX;
                                    pipelineResult.CalibratedY = camera.getCamVals().CenterY;
                                    break;
                                case Single:
                                    // use the static point as a calibration method instead of the center
                                    pipelineResult.CalibratedX = currentPipeline.point.get(0).doubleValue();
                                    pipelineResult.CalibratedY = currentPipeline.point.get(1).doubleValue();
                                    break;
                                case Dual:
                                    // use the calculated line to find the difference in length between the point and the line
                                    pipelineResult.CalibratedX = (finalRect.center.y - currentPipeline.b) / currentPipeline.m;
                                    pipelineResult.CalibratedY = (finalRect.center.x * currentPipeline.m) + currentPipeline.b;
                                    break;
                        }
                        pipelineResult.Pitch = camera.getCamVals().CalculatePitch(finalRect.center.y, pipelineResult.CalibratedY);
                        pipelineResult.Yaw = camera.getCamVals().CalculateYaw(finalRect.center.x, pipelineResult.CalibratedX);
                        pipelineResult.Area = finalRect.size.area();
                        drawContour(outputImage, finalRect);
                    }
                }
            }
        }

        return pipelineResult;
    }

    @Override
    public void run() {
        // processing time tracking
        long startTime;
        long fpsLastTime = 0;
        double processTimeMs;
        double fps = 0;
        double uiFps = 0;
        int maxFps = camera.getVideoMode().fps;

        new Thread(cameraProcess).start();

        long lastFrameEndNanosec = 0;

        while (!Thread.interrupted()) {
            startTime = System.nanoTime();
            if ((startTime - lastFrameEndNanosec) * 1e-6 >= 1000.0 / maxFps + 3) { // 3 additional fps to allow for overhead
                foundContours.clear();
                filteredContours.clear();
                groupedContours.clear();

                // update FPS for ui only every 0.5 seconds
                if ((startTime - fpsLastTime) * 1e-6 >= 500) {
                    if (fps >= maxFps) {
                        uiFps = maxFps;
                    } else {
                        uiFps = fps;
                    }
                    fpsLastTime = System.nanoTime();
                }

                currentPipeline = camera.getCurrentPipeline();
                // start fps counter right before grabbing input frame
                timeStamp = cameraProcess.getLatestFrame(cameraInputMat);
                if (cameraInputMat.cols() == 0 && cameraInputMat.rows() == 0) {
                    continue;
                }

                // get vision data
                var pipelineResult = runVisionProcess(cameraInputMat, streamOutputMat);
                updateNetworkTables(pipelineResult);
                if (cameraName.equals(SettingsManager.GeneralSettings.currentCamera)) {
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
}
