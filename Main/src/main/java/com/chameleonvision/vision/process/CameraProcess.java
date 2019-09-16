package com.chameleonvision.vision.process;

import com.chameleonvision.MemoryManager;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.CameraValues;
import com.chameleonvision.vision.Pipeline;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.cameraserver.CameraServer;
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


    public CameraProcess(String CameraName){
        this.CameraName = CameraName;

        // add pipeline
        SettingsManager.CamerasCurrentPipeline.put(CameraName, SettingsManager.Cameras.get(CameraName).pipelines.keySet().toArray()[0].toString());

        // NetworkTables
        NetworkTable ntTable = NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + CameraName);
        ntPipelineEntry = ntTable.getEntry("Pipeline");
        ntDriverModeEntry =  ntTable.getEntry("Driver_Mode");

        imgWidth = SettingsManager.Cameras.get(CameraName).camVideoMode.width;
        imgHeight = SettingsManager.Cameras.get(CameraName).camVideoMode.height;
    }



    @Override
    public void run() {
        var cv_sink = cs.getVideo(SettingsManager.UsbCameras.get(CameraName));
        var cv_publish = cs.putVideo(CameraName, imgWidth, imgHeight);
        double fov = SettingsManager.Cameras.get(CameraName).FOV;
        CameraValues camVals = new CameraValues(imgWidth, imgHeight, fov);
        VisionProcess visionProcess = new VisionProcess(camVals);
        Pipeline currentPipeline;

        List<MatOfPoint> FoundContours = new ArrayList<>();
        List<MatOfPoint> FilteredContours = new ArrayList<>();
        Mat inputMat = new Mat();
        Mat bgrMat = new Mat();
        Mat hsvThreshMat = new Mat();
        Mat outputMat = new Mat();
        Mat contourBoxPointsMat = new Mat();
        Scalar contourColor = new Scalar(255, 0, 0);
        long startTime, endTime;
        
        while (!Thread.interrupted()) {
            startTime = System.nanoTime();

            FoundContours.clear();
            FilteredContours.clear();

            currentPipeline = SettingsManager.Cameras.get(CameraName).pipelines.get(SettingsManager.CamerasCurrentPipeline.get(CameraName));
            cv_sink.grabFrame(inputMat);
            if (inputMat.cols() !=0 && inputMat.rows() != 0) {
                Imgproc.cvtColor(inputMat, bgrMat, Imgproc.COLOR_RGB2BGR, 3);

                Scalar hsvLower = new Scalar(currentPipeline.hue.get(0), currentPipeline.saturation.get(0), currentPipeline.value.get(0));
                Scalar hsvUpper = new Scalar(currentPipeline.hue.get(1), currentPipeline.saturation.get(1), currentPipeline.value.get(1));

                visionProcess.HSVThreshold(inputMat, hsvThreshMat, hsvLower, hsvUpper, currentPipeline.erode, currentPipeline.dilate);
                FoundContours = visionProcess.FindContours(hsvThreshMat);
                FilteredContours = visionProcess.FilterContours(FoundContours, currentPipeline.area, currentPipeline.ratio, currentPipeline.extent, currentPipeline.sort_mode, currentPipeline.target_intersection, currentPipeline.target_group);

                if (currentPipeline.is_binary == 1) {
                    Imgproc.cvtColor(hsvThreshMat, hsvThreshMat, Imgproc.COLOR_GRAY2BGR, 3);
                    outputMat = hsvThreshMat;
                } else {
                    outputMat = inputMat;
                }

                if (FilteredContours.size() > 0) {
                    for (int i = 0; i < FilteredContours.size(); i++) {
                        Imgproc.drawContours(outputMat, FilteredContours, i,  contourColor, 10);
                    }
                }

                cv_publish.putFrame(outputMat);
                inputMat.release();
                hsvThreshMat.release();
                for (MatOfPoint oldMat : FoundContours) { oldMat.release(); }
                for (MatOfPoint oldMat1 : FilteredContours) { oldMat1.release(); }
            }
            memManager.run();
            endTime = System.nanoTime();
        }

    }
}
