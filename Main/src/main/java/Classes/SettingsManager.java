package Classes;
import Objects.*;
import java.io.*;
import java.nio.file.*;

import Objects.CamVideoMode;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.wpi.cscore.*;
import org.opencv.videoio.VideoCapture;

public class SettingsManager  {
    private static SettingsManager instance;
    private SettingsManager() {
        InitiateGeneralSettings();
        InitiateCamerasInfo();
        InitiateUsbCameras();
        InitiateCameras();
        InitiateUsbCamerasSettings();
    }
    public static synchronized SettingsManager getInstance(){
        if(instance == null){
            synchronized (SettingsManager.class) {
                if(instance == null){
                    instance = new SettingsManager();
                }
            }
        }
        return instance;
    }
    public static HashMap<String,Camera> Cameras = new HashMap<String, Camera>();
    public static HashMap<String,UsbCamera> UsbCameras = new HashMap<String, UsbCamera>();
    public static HashMap<String,UsbCameraInfo> USBCamerasInfo = new HashMap<String,UsbCameraInfo>();
    public static DefaultGeneralSettings GeneralSettings;
    public static HashMap CameraPort = new HashMap();
    public static HashMap CamerasCurrentPipeline = new HashMap();
    private Path SettingsPath = Paths.get(System.getProperty("user.dir"),"Settings");
    private Path CamsPath = Paths.get(SettingsPath.toString(),"Cams");


    private void InitiateGeneralSettings(){
        CheckPath(SettingsPath);
        try {
            GeneralSettings = new Gson().fromJson(new FileReader(Paths.get(SettingsPath.toString(),"Settings.json").toString()),DefaultGeneralSettings.class);
        } catch (FileNotFoundException e) {
            GeneralSettings = new DefaultGeneralSettings();
        }
    }

    private void InitiateCamerasInfo(){
        List<Integer> TrueCameras = new ArrayList<Integer>();
        UsbCameraInfo[] UsbDevices = UsbCamera.enumerateUsbCameras();
        for (var i=0; i < UsbDevices.length; i++){
            var cap = new VideoCapture(UsbDevices[i].dev);
            if (cap.isOpened()){
                TrueCameras.add(i);
                cap.release();
            }

        }
        for (var i: TrueCameras){
            var DeviceName = UsbDevices[i].name;
            var suffix = 0;
            while (USBCamerasInfo.containsKey(DeviceName)){
                suffix++;
                DeviceName = String.format("%s(%s)",UsbDevices[i].name,suffix);
            }
            USBCamerasInfo.put(DeviceName,UsbDevices[i]);
        }
    }
    private void InitiateUsbCameras(){
        for(Map.Entry<String,UsbCameraInfo> entry : USBCamerasInfo.entrySet()){
            var device = entry.getValue();
            var camera = new UsbCamera(device.name, device.dev);
            UsbCameras.put(device.name,camera);
        }
    }
    private void InitiateCameras(){
        CheckPath(CamsPath);
        for(Map.Entry<String,UsbCameraInfo> entry: USBCamerasInfo.entrySet()){
            var camPath = Paths.get(CamsPath.toString(),String.format("%s.json",entry.getKey()));
            if(Files.exists(camPath)){
                try {
                    Camera cam = new Gson().fromJson(new FileReader(camPath.toString()),Camera.class);
                    Cameras.put(entry.getKey(),cam);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else{
                CreateNewCam(entry.getKey());
            }
        }
    }
    private void InitiateUsbCamerasSettings(){
        for(Map.Entry<String,UsbCamera> entry: UsbCameras.entrySet()){
            var cam = entry.getValue();
            var camName = entry.getKey();
            var camInfo = Cameras.get(camName);
            cam.setPixelFormat(VideoMode.PixelFormat.valueOf(camInfo.camVideoMode.pixel_format));
            cam.setFPS(camInfo.camVideoMode.fps);
            cam.setResolution(camInfo.camVideoMode.width, camInfo.camVideoMode.heigh);
        }
    }
    private void CheckPath(Path path){
        if (!Files.exists(path)){
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Creators
    private void CreateNewCam(String CameraName){
        Camera cam = new Camera();
        var caminfo =USBCamerasInfo.get(CameraName);
        cam.path = caminfo.path;
        var videomode = UsbCameras.get(CameraName).enumerateVideoModes()[0];
        CamVideoMode CamVideoMode = new CamVideoMode();
        CamVideoMode.fps = videomode.fps;
        CamVideoMode.heigh = videomode.height;
        CamVideoMode.width = videomode.width;
        CamVideoMode.pixel_format = videomode.pixelFormat.name();
        cam.camVideoMode = CamVideoMode;
        cam.pipelines = new HashMap<String, DefaultPipeline>();
        cam.resolution = 0;
        cam.FOV = 60.8;
        Cameras.put(CameraName,cam);

        CreateNewPipeline(null,CameraName);

    }
    private void CreateNewPipeline(String PipeName, String CamName){
        if (CamName == null){
            CamName = GeneralSettings.curr_camera;
        }
        var cam = Cameras.get(CamName);
        if (PipeName == null){
            var suffix = 0;
            PipeName = "pipeline" + suffix;
            while (cam.pipelines.containsKey(PipeName)){
                suffix ++;
                PipeName = "pipeline"+suffix;
            }
        }
        else if (cam.pipelines.containsKey(PipeName)){
            System.err.println("Pipeline Already Exists");
        }
        cam.pipelines.put(PipeName,new DefaultPipeline());
    }
    //Savers
    public void SaveSettings(){
        SaveCameras();
        SaveGeneralSettings();
    }
    private void SaveCameras(){
        for(Map.Entry<String,Camera> entry: Cameras.entrySet()){
            try {
                Gson gson = new Gson();
                FileWriter writer = new FileWriter(Paths.get(CamsPath.toString(),String.format("%s.json",entry.getKey())).toString());
                gson.toJson(entry.getValue(),writer);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void SaveGeneralSettings(){
        try {
            Gson gson = new Gson();
            FileWriter writer = new FileWriter(Paths.get(SettingsPath.toString(),"Settings.json").toString());
            new Gson().toJson(GeneralSettings, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
