package com.chameleonvision.vision.process;

import com.chameleonvision.MemoryManager;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.Pipeline;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.Mat;
import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.MatOfPoint;

import java.util.ArrayList;
import java.util.List;

public class CameraProcess implements Runnable {
    private String CameraName;
    public CameraProcess(String CameraName){
        this.CameraName = CameraName;
    }

    @Override
    public void run() {

        //calling all classes
        CameraServer cs = CameraServer.getInstance();
        NetworkTableInstance networkTableInstance = NetworkTableInstance.getDefault();
        SettingsManager manager = SettingsManager.getInstance();
        SettingsManager.CamerasCurrentPipeline.put(CameraName, SettingsManager.Cameras.get(CameraName).pipelines.keySet().toArray()[0].toString());
        //Setting up camera and network table
        var Table = networkTableInstance.getTable("/chameleon-vision/" + CameraName);
        var PipeLineEntry = Table.getEntry("Pipeline");
        var DriverModeEntry =  Table.getEntry("Driver_Mode");
        var cv_sink = cs.getVideo(SettingsManager.UsbCameras.get(CameraName));


        int Width = SettingsManager.Cameras.get(CameraName).camVideoMode.width;
        int Height = SettingsManager.Cameras.get(CameraName).camVideoMode.heigh;
        var cv_publish = cs.putVideo(CameraName,Width,Height);
        //initial math setup for camera
        double DiagonalView = FastMath.toRadians(SettingsManager.Cameras.get(CameraName).FOV);
        Fraction AspectFraction = new Fraction(Width,Height);
        int HorizontalRatio = AspectFraction.getNumerator();
        int VerticalRatio = AspectFraction.getDenominator();
        double HorizontalView = FastMath.atan(FastMath.tan(DiagonalView/2) * (HorizontalRatio / DiagonalView)) * 2;
        double VerticalView = FastMath.atan(FastMath.tan(DiagonalView/2) * (VerticalRatio / DiagonalView)) * 2;
        double H_FOCAL_LENGTH = Width / (2 * FastMath.tan(HorizontalView /2));
        double V_FOCAL_LENGTH = Width / (2 * FastMath.tan(VerticalView /2));
        double CenterX = ((double) Width / 2) - 0.5;
        double CenterY = ((double) Height/2) - 0.5;
        double CamArea = (double)(Width * Height);
        VisionProcess visionProcess = new VisionProcess(CenterX,CenterY,CamArea);
        Mat mat = new Mat();
        long time;

        MemoryManager memManager = new MemoryManager(125);

        Pipeline currentPipeline;
        Mat HSVImage = new Mat();
        List<MatOfPoint> FoundContours = new ArrayList<>();
        List<MatOfPoint> FilteredContours = new ArrayList<>();

        while (!Thread.interrupted()){
            FoundContours.clear();
            FilteredContours.clear();

            currentPipeline = SettingsManager.Cameras.get(CameraName).pipelines.get(SettingsManager.CamerasCurrentPipeline.get(CameraName));
            time = cv_sink.grabFrame(mat);
            if (mat.cols() !=0 && mat.rows() != 0) {
                HSVImage = visionProcess.HSVThreshold(currentPipeline.hue, currentPipeline.saturation, currentPipeline.value, mat, currentPipeline.erode, currentPipeline.dilate);
                FoundContours = visionProcess.FindContours(HSVImage);
                FilteredContours = visionProcess.FilterContours(FoundContours, currentPipeline.area, currentPipeline.ratio, currentPipeline.extent, currentPipeline.sort_mode, currentPipeline.target_intersection, currentPipeline.target_group);

                cv_publish.putFrame(mat);
                mat.release();
                HSVImage.release();
                for (MatOfPoint oldMat : FoundContours) { oldMat.release(); }
                for (MatOfPoint oldMat1 : FilteredContours) { oldMat1.release(); }
            }

            memManager.run();
        }

    }
}
