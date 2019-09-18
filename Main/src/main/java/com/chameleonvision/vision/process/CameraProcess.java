package com.chameleonvision.vision.process;

import com.chameleonvision.MemoryManager;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.CameraValues;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.web.Server;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.first.networktables.*;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CameraProcess implements Runnable {
    private String CameraName;

    // NetworkTables
    private NetworkTableEntry ntPipelineEntry;
    private NetworkTableEntry ntDriverModeEntry;
    private NetworkTableEntry ntYawEntry;
    private NetworkTableEntry ntPitchEntry;
    private NetworkTableEntry ntDistanceEntry;
    private NetworkTableEntry ntTimeStampEntry;
    private NetworkTableEntry ntValidEntry;

    private MemoryManager memManager = new MemoryManager(125);

    // chameleon specific
    private Pipeline currentPipeline;
    private VisionProcess visionProcess;
    private CameraValues camVals;

    // cscore
    private CvSink cvSink;
    private CvSource cvPublish;

    // pipeline process items
    private List<MatOfPoint> FoundContours = new ArrayList<>();
    private List<MatOfPoint> FilteredContours = new ArrayList<>();
    private List<RotatedRect> GroupedContours = new ArrayList<>();
    private Mat cameraInputMat = new Mat();
    private Mat hsvThreshMat = new Mat();
    private Mat streamOutputMat = new Mat();
    private Scalar contourRectColor = new Scalar(255, 0, 0);

    private void ChangeCameraValues(int exposure, int brightness) {
        SettingsManager.UsbCameras.get(CameraName).setBrightness(brightness);
        SettingsManager.UsbCameras.get(CameraName).setExposureManual(exposure);
    }

    private void DriverModeListener(EntryNotification entryNotification) {
        if (entryNotification.value.getBoolean()) {
            ChangeCameraValues(25, 15);
        } else {
            Pipeline pipeline = SettingsManager.Cameras.get(CameraName).pipelines.get(SettingsManager.CamerasCurrentPipeline.get(CameraName));
            ChangeCameraValues(pipeline.exposure, pipeline.brightness);
        }
    }

    private void PipelineListener(EntryNotification entryNotification) {
        if (SettingsManager.Cameras.get(CameraName).pipelines.containsKey(entryNotification.value.getString())) {
            SettingsManager.CamerasCurrentPipeline.put(CameraName, entryNotification.value.getString());
            Pipeline pipeline = SettingsManager.Cameras.get(CameraName).pipelines.get(SettingsManager.CamerasCurrentPipeline.get(CameraName));
            ChangeCameraValues(pipeline.exposure, pipeline.brightness);
            //TODO Send Pipeline change using websocket to client
        } else {
            ntPipelineEntry.setString(SettingsManager.CamerasCurrentPipeline.get(CameraName));
        }
    }

    public CameraProcess(String cameraName) {
        CameraName = cameraName;
        SettingsManager.CamerasCurrentPipeline.put(CameraName,SettingsManager.Cameras.get(CameraName).pipelines.keySet().stream().findFirst().toString());
        // NetworkTables
        NetworkTable ntTable = NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + cameraName);
        ntPipelineEntry = ntTable.getEntry("Pipeline");
        ntDriverModeEntry = ntTable.getEntry("Driver_Mode");
        ntPitchEntry = ntTable.getEntry("Pitch");
        ntYawEntry = ntTable.getEntry("Yaw");
        ntDistanceEntry = ntTable.getEntry("Distance");
        ntTimeStampEntry = ntTable.getEntry("TimeStamp");
        ntValidEntry = ntTable.getEntry("Valid");
        ntDriverModeEntry.addListener(this::DriverModeListener, EntryListenerFlags.kUpdate);
        ntPipelineEntry.addListener(this::PipelineListener, EntryListenerFlags.kUpdate);
        ntDriverModeEntry.setBoolean(false);
        ntPipelineEntry.setString(SettingsManager.CamerasCurrentPipeline.get(cameraName));

        // camera settings
        camVals = new CameraValues(SettingsManager.Cameras.get(cameraName));
        visionProcess = new VisionProcess(camVals);

        // cscore setup
        CameraServer cs = CameraServer.getInstance();
        cvSink = cs.getVideo(SettingsManager.UsbCameras.get(cameraName));
        cvPublish = cs.putVideo(cameraName, camVals.ImageWidth, camVals.ImageHeight);

    }

    private void drawContour(Mat inputMat, RotatedRect contourRect) {
        if (contourRect == null) return;
        List<MatOfPoint> drawnContour = new ArrayList<>();
        Point[] vertices = new Point[4];
        contourRect.points(vertices);
        drawnContour.add(new MatOfPoint(vertices));
        Imgproc.drawContours(inputMat, drawnContour, 0, contourRectColor, 3);
        Imgproc.circle(inputMat, contourRect.center, 3, contourRectColor);
    }

    // TODO: Separate video output, contour drawing, data output to separate function, maybe even second thread
    private PipelineResult runVisionProcess(Mat inputImage, Mat outputImage) {
        var pipelineResult = new PipelineResult();

        Scalar hsvLower = new Scalar(currentPipeline.hue.get(0), currentPipeline.saturation.get(0), currentPipeline.value.get(0));
        Scalar hsvUpper = new Scalar(currentPipeline.hue.get(1), currentPipeline.saturation.get(1), currentPipeline.value.get(1));

        visionProcess.HSVThreshold(inputImage, hsvThreshMat, hsvLower, hsvUpper, currentPipeline.erode, currentPipeline.dilate);

        if (currentPipeline.is_binary == 1) {
            Imgproc.cvtColor(hsvThreshMat, outputImage, Imgproc.COLOR_GRAY2BGR, 3);
        } else {
            inputImage.copyTo(outputImage);
        }

        FoundContours = visionProcess.FindContours(hsvThreshMat);
        if (FoundContours.size() > 0) {
            FilteredContours = visionProcess.FilterContours(FoundContours, currentPipeline.area, currentPipeline.ratio, currentPipeline.extent, currentPipeline.sort_mode, currentPipeline.target_intersection, currentPipeline.target_group);
            if (FilteredContours.size() > 0) {
                GroupedContours = visionProcess.GroupTargets(FilteredContours, currentPipeline.target_intersection, currentPipeline.target_group);
                if (GroupedContours.size() > 0) {
                    var finalRect = visionProcess.SortTargetsToOne(GroupedContours, currentPipeline.sort_mode);
                    pipelineResult.RawPoint = finalRect;
                    pipelineResult.IsValid = true;
                    if (!currentPipeline.is_calibrated) {
                        pipelineResult.CalibratedX = camVals.CenterX;
                        pipelineResult.CalibratedY = camVals.CenterY;
                    } else {
                        pipelineResult.CalibratedX = (finalRect.center.y - currentPipeline.B) / currentPipeline.M;
                        pipelineResult.CalibratedY = finalRect.center.x * currentPipeline.M + currentPipeline.B;
                        pipelineResult.Pitch = camVals.CalculatePitch(finalRect.center.y, pipelineResult.CalibratedY);
                        pipelineResult.Yaw = camVals.CalculateYaw(finalRect.center.x, pipelineResult.CalibratedX);
                    }
                    // TODO Send pitch yaw distance and Raw Point using websockets to client for calib calc
                    drawContour(outputImage, finalRect);
                }
            }
        }

        return pipelineResult;
    }

    @Override
    public void run() {
        // processing time tracking
        long startTime, TimeStamp;
        double processTimeMs;
        double fps = 0;

        while (!Thread.interrupted()) {
            FoundContours.clear();
            FilteredContours.clear();
            GroupedContours.clear();
            currentPipeline = SettingsManager.Cameras.get(CameraName).pipelines.get(SettingsManager.CamerasCurrentPipeline.get(CameraName));
            
//            System.out.println(SettingsManager.CamerasCurrentPipeline.get(CameraName));
            // start fps counter right before grabbing input frame
            startTime = System.nanoTime();
            TimeStamp = cvSink.grabFrame(cameraInputMat);
            if (cameraInputMat.cols() == 0 && cameraInputMat.rows() == 0) {
                continue;
            }

            // get vision data
            var pipelineResult = runVisionProcess(cameraInputMat, streamOutputMat);

            ntValidEntry.setBoolean(pipelineResult.IsValid);
            if (pipelineResult.IsValid){
                ntYawEntry.setNumber(pipelineResult.Yaw);
                ntPitchEntry.setNumber(pipelineResult.Pitch);
            }
            ntTimeStampEntry.setNumber(TimeStamp);
            if (CameraName.equals(SettingsManager.GeneralSettings.curr_camera)){
                HashMap<String,Object> WebSend = new HashMap<>();
                HashMap<String,Object> point = new HashMap<>();
                List<Double> center = new ArrayList<Double>();
                center.add(pipelineResult.RawPoint.center.x);
                center.add(pipelineResult.RawPoint.center.y);
                point.put("pitch", pipelineResult.Pitch);
                point.put("yaw", pipelineResult.Yaw);
                point.put("fps", fps);
                WebSend.put("point", point);
                WebSend.put("raw_point",center);
                Server.broadcastMessage(WebSend);
            }
            cvPublish.putFrame(streamOutputMat);
            // calculate FPS after publishing output frame
            processTimeMs = (System.nanoTime() - startTime) * 1e-6;
            fps = 1000 / processTimeMs;
            System.out.printf("%s - Process time: %fms, FPS: %.2f, FoundContours: %d, FilteredContours: %d, GroupedContours: %d\n",CameraName ,processTimeMs, fps, FoundContours.size(), FilteredContours.size(), GroupedContours.size());

            cameraInputMat.release();
            hsvThreshMat.release();

            memManager.run(true);
        }
    }
}
