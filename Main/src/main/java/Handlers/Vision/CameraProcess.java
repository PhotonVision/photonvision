package Handlers.Vision;

import Classes.SettingsManager;
import Objects.Pipeline;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.Mat;
import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.MatOfPoint;

import java.util.List;

public class CameraProcess implements Runnable{
    private String CameraName;
    public CameraProcess(String CameraName){
        this.CameraName = CameraName;
    }

    @Override
    public void run() {

        //calling all classes
        CameraServer cs = CameraServer.getInstance();
//        NetworkTableInstance networkTableInstance = NetworkTableInstance.getDefault();
        SettingsManager manager = SettingsManager.getInstance();
        manager.CamerasCurrentPipeline.put(CameraName,manager.Cameras.get(CameraName).pipelines.keySet().toArray()[0].toString());
        //Setting up camera and network table
//        var Table = networkTableInstance.getTable("/Chameleon-Vision/" + CameraName);
//        var PipeLineEntry = Table.getEntry("Pipeline");
//        var DriverModeEntry =  Table.getEntry("Driver_Mode");
        var cv_sink = cs.getVideo(manager.UsbCameras.get(CameraName));
        int Width = manager.Cameras.get(CameraName).camVideoMode.width;
        int Height = manager.Cameras.get(CameraName).camVideoMode.heigh;
        var cv_publish = cs.putVideo(CameraName,Width,Height);
        //initial math setup for camera
        double DiagonalView = FastMath.toRadians(manager.Cameras.get(CameraName).FOV);
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

        while (!Thread.interrupted()){
            Pipeline pipeline = manager.Cameras.get(CameraName).pipelines.get(manager.CamerasCurrentPipeline.get(CameraName));
            time = cv_sink.grabFrame(mat);
//            Mat HSVImage = visionProcess.HSVThreshold(pipeline.hue,pipeline.saturation,pipeline.value,mat,pipeline.erode,pipeline.dilate);
//            List<MatOfPoint> Contours = visionProcess.FindContours(HSVImage);
//            List<MatOfPoint> FilterdContours = visionProcess.FilterContours(Contours,pipeline.area,pipeline.ratio,pipeline.extent,pipeline.sort_mode,pipeline.target_intersection,pipeline.target_group);
            cv_publish.putFrame(mat);
        }

    }
}
