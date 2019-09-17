package com.chameleonvision.vision.process;

import com.chameleonvision.MemoryManager;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.CameraValues;
import com.chameleonvision.vision.Pipeline;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.cameraserver.CameraServer;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CameraProcess implements Runnable {
    private String CameraName;

    private CameraServer cs = CameraServer.getInstance();
    private NetworkTableEntry ntPipelineEntry, ntDriverModeEntry;

    private MemoryManager memManager = new MemoryManager(125);

    private int imgWidth, imgHeight;


    public CameraProcess(String CameraName) {
        this.CameraName = CameraName;

        // add pipeline
        SettingsManager.CamerasCurrentPipeline.put(CameraName, SettingsManager.Cameras.get(CameraName).pipelines.keySet().toArray()[0].toString());

        // NetworkTables
        NetworkTable ntTable = NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + CameraName);
        ntPipelineEntry = ntTable.getEntry("Pipeline");
        ntDriverModeEntry = ntTable.getEntry("Driver_Mode");

        imgWidth = SettingsManager.Cameras.get(CameraName).camVideoMode.width;
        imgHeight = SettingsManager.Cameras.get(CameraName).camVideoMode.height;
    }

    @Override
    public void run() {
        // camera values
        var cv_sink = cs.getVideo(SettingsManager.UsbCameras.get(CameraName));
        var cv_publish = cs.putVideo(CameraName, imgWidth, imgHeight);
        double fov = SettingsManager.Cameras.get(CameraName).FOV;
        CameraValues camVals = new CameraValues(imgWidth, imgHeight, fov);
        VisionProcess visionProcess = new VisionProcess(camVals);
        Pipeline currentPipeline;

        // actual OpenCV objects
        List<MatOfPoint> FoundContours = new ArrayList<>();
        List<MatOfPoint> FilteredContours = new ArrayList<>();
        List<RotatedRect> GroupedContours = new ArrayList<>();
        Mat inputMat = new Mat();
        Mat hsvThreshMat = new Mat();
        Mat outputMat = new Mat();
        Scalar contourColor = new Scalar(255, 0, 0);

        // processing time tracking
        long startTime;
        double processTimeMs;
        double fps;

        while (!Thread.interrupted()) {
            FoundContours.clear();
            FilteredContours.clear();
            GroupedContours.clear();

            currentPipeline = SettingsManager.Cameras.get(CameraName).pipelines.get(SettingsManager.CamerasCurrentPipeline.get(CameraName));

            // start fps counter right before grabbing input frame
            startTime = System.nanoTime();
            cv_sink.grabFrame(inputMat);
            if (inputMat.cols() == 0 && inputMat.rows() == 0) {
                continue;
            }

            Scalar hsvLower = new Scalar(currentPipeline.hue.get(0), currentPipeline.saturation.get(0), currentPipeline.value.get(0));
            Scalar hsvUpper = new Scalar(currentPipeline.hue.get(1), currentPipeline.saturation.get(1), currentPipeline.value.get(1));

            visionProcess.HSVThreshold(inputMat, hsvThreshMat, hsvLower, hsvUpper, currentPipeline.erode, currentPipeline.dilate);

            if (currentPipeline.is_binary == 1) {
                Imgproc.cvtColor(hsvThreshMat, outputMat, Imgproc.COLOR_GRAY2BGR, 3);
            } else {
                outputMat = inputMat;
            }

            FoundContours = visionProcess.FindContours(hsvThreshMat);
            if (FoundContours.size() > 0) {
                FilteredContours = visionProcess.FilterContours(FoundContours, currentPipeline.area, currentPipeline.ratio, currentPipeline.extent, currentPipeline.sort_mode, currentPipeline.target_intersection, currentPipeline.target_group);
                if (FilteredContours.size() > 0) {
                    GroupedContours = visionProcess.GroupTargets(FilteredContours, currentPipeline.target_intersection, currentPipeline.target_group);
                    if (GroupedContours.size() > 0) {
                        var finalRect = visionProcess.SortTargetsToOne(GroupedContours, currentPipeline.sort_mode);
                        if (finalRect != null) {
                            List<MatOfPoint> a = new ArrayList<>();
                            Point[] vertices = new Point[4];
                            finalRect.points(vertices);
                            a.add(new MatOfPoint(vertices));
                            Imgproc.drawContours(outputMat, a, 0, contourColor, 3);
                        }
                    }
                }
            }

            cv_publish.putFrame(outputMat);

            // calculate FPS after publishing output frame
            processTimeMs = (System.nanoTime() - startTime) * 1e-6;
            fps = 1000 / processTimeMs;
            System.out.printf("Process time: %fms, FPS: %.2f, FoundContours: %d, FilteredContours: %d, GroupedContours: %d\n", processTimeMs, fps, FoundContours.size(), FilteredContours.size(), GroupedContours.size());

            inputMat.release();
            hsvThreshMat.release();
        }
    }
}
