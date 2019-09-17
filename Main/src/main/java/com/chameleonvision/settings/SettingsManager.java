package com.chameleonvision.settings;

import com.chameleonvision.NoCameraException;

import java.io.*;
import java.nio.file.*;

import com.chameleonvision.vision.CamVideoMode;
import com.chameleonvision.vision.Camera;
import com.chameleonvision.vision.GeneralSettings;
import com.chameleonvision.vision.Pipeline;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.wpi.cscore.*;
import org.opencv.videoio.VideoCapture;

public class SettingsManager {
    private static SettingsManager instance;

    private SettingsManager() {
        InitiateGeneralSettings();
        InitiateCamerasInfo();
        InitiateUsbCameras();
        InitiateCameras();
        InitiateUsbCamerasSettings();

        if (!Cameras.containsKey(GeneralSettings.curr_camera) && Cameras.size() > 0) {
            String camName = Cameras.keySet().stream().findFirst().get();
            GeneralSettings.curr_camera = camName;
            GeneralSettings.curr_pipeline = Cameras.get(camName).pipelines.keySet().stream().findFirst().get();
        }
    }

    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            synchronized (SettingsManager.class) {
                if (instance == null) {
                    instance = new SettingsManager();
                }
            }
        }
        return instance;
    }

    public static HashMap<String, Camera> Cameras = new HashMap<String, Camera>();
    public static HashMap<String, UsbCamera> UsbCameras = new HashMap<String, UsbCamera>();
    public static HashMap<String, UsbCameraInfo> USBCamerasInfo = new HashMap<String, UsbCameraInfo>();
    public static com.chameleonvision.vision.GeneralSettings GeneralSettings;
    public static HashMap<String, String> CamerasCurrentPipeline = new HashMap<String, String>();
    public static HashMap<String, String> CameraPorts = new HashMap<String, String>();//TODO Implement ports
    private Path SettingsPath = Paths.get(System.getProperty("user.dir"), "Settings");
    private Path CamsPath = Paths.get(SettingsPath.toString(), "Cams");


    private void InitiateGeneralSettings() {
        CheckPath(SettingsPath);
        try {
            GeneralSettings = new Gson().fromJson(new FileReader(Paths.get(SettingsPath.toString(), "Settings.json").toString()), com.chameleonvision.vision.GeneralSettings.class);
        } catch (FileNotFoundException e) {
            GeneralSettings = new GeneralSettings();
        }
    }

    private void InitiateCamerasInfo() {
        List<Integer> TrueCameras = new ArrayList<Integer>();
        UsbCameraInfo[] UsbDevices = UsbCamera.enumerateUsbCameras();
        for (var i = 0; i < UsbDevices.length; i++) {
            var cap = new VideoCapture(UsbDevices[i].dev);
            if (cap.isOpened()) {
                TrueCameras.add(i);
                cap.release();
            }

        }
        for (var i : TrueCameras) {
            var DeviceName = UsbDevices[i].name;
            var suffix = 0;
            while (USBCamerasInfo.containsKey(DeviceName)) {
                suffix++;
                DeviceName = String.format("%s(%s)", UsbDevices[i].name, suffix);
            }
            USBCamerasInfo.put(DeviceName, UsbDevices[i]);
        }
    }

    private void InitiateUsbCameras() {
        for (Map.Entry<String, UsbCameraInfo> entry : USBCamerasInfo.entrySet()) {
            var device = entry.getValue();
            var name = entry.getKey();
            UsbCamera camera = new UsbCamera(name, device.dev);
            UsbCameras.put(name, camera);
        }
    }

    private void InitiateCameras() {
        CheckPath(CamsPath);
        for (Map.Entry<String, UsbCameraInfo> entry : USBCamerasInfo.entrySet()) {
            var camPath = Paths.get(CamsPath.toString(), String.format("%s.json", entry.getKey()));
            if (Files.exists(camPath)) {
                try {
                    Camera cam = new Gson().fromJson(new FileReader(camPath.toString()), Camera.class);
                    Cameras.put(entry.getKey(), cam);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                CreateNewCam(entry.getKey());
            }
        }
    }

    private void InitiateUsbCamerasSettings() {
        for (Map.Entry<String, UsbCamera> entry : UsbCameras.entrySet()) {
            var cam = entry.getValue();
            var camName = entry.getKey();
            var camInfo = Cameras.get(camName);
            cam.setPixelFormat(VideoMode.PixelFormat.valueOf(camInfo.camVideoMode.pixel_format));
            cam.setFPS(camInfo.camVideoMode.fps);
            cam.setResolution(camInfo.camVideoMode.width, camInfo.camVideoMode.height);
        }
    }

    private void CheckPath(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Creators
    private void CreateNewCam(String CameraName) {
        Camera cam = new Camera();
        var caminfo = USBCamerasInfo.get(CameraName);
        cam.path = caminfo.path;
        var videomode = UsbCameras.get(CameraName).enumerateVideoModes()[0];
        CamVideoMode CamVideoMode = new CamVideoMode();
        CamVideoMode.fps = videomode.fps;
        CamVideoMode.height = videomode.height;
        CamVideoMode.width = videomode.width;
        CamVideoMode.pixel_format = videomode.pixelFormat.name();
        cam.camVideoMode = CamVideoMode;
        cam.pipelines = new HashMap<String, Pipeline>();
        cam.resolution = 0;
        cam.FOV = 60.8;
        Cameras.put(CameraName, cam);

        CreateNewPipeline(null, CameraName);
        CreateNewPipeline(null, CameraName);//Created 2 pipeline for testing TODO add a create pipeline button

    }

    private void CreateNewPipeline(String PipeName, String CamName) {
        if (CamName == null) {
            CamName = GeneralSettings.curr_camera;
        }
        var cam = Cameras.get(CamName);
        if (PipeName == null) {
            var suffix = 0;
            PipeName = "pipeline" + suffix;
            while (cam.pipelines.containsKey(PipeName)) {
                suffix++;
                PipeName = "pipeline" + suffix;
            }
        } else if (cam.pipelines.containsKey(PipeName)) {
            System.err.println("Pipeline Already Exists");
        }
        cam.pipelines.put(PipeName, new Pipeline());
    }

    //Access Methods
    public Pipeline GetCurrentPipeline() throws NoCameraException {
        if (!GeneralSettings.curr_pipeline.equals("")) {
            return Cameras.get(GeneralSettings.curr_camera).pipelines.get(GeneralSettings.curr_pipeline);
        }
        throw new NoCameraException();
    }

    public Camera GetCurrentCamera() throws NoCameraException {
        if (!GeneralSettings.curr_camera.equals("")) {
            return Cameras.get(GeneralSettings.curr_camera);
        }
        throw new NoCameraException();
    }

    public UsbCamera GetCurrentUsbCamera() throws NoCameraException {
        if (!GeneralSettings.curr_camera.equals("")) {
            return UsbCameras.get(GeneralSettings.curr_camera);
        }
        throw new NoCameraException();
    }

    public List<String> GetResolutionList() throws NoCameraException {
        if (!GeneralSettings.curr_camera.equals("")) {
            List<String> list = new ArrayList<String>();
            var cam = UsbCameras.get(GeneralSettings.curr_camera);
            for (var res : cam.enumerateVideoModes()) {
                list.add(String.format("%s X %s at %s fps", res.width, res.height, res.fps));
            }
            return list;
        }
        throw new NoCameraException();
    }

    public void SetCurrentCamera(String CamName) throws Exception {
        if (Cameras.containsKey(CamName)) {
            GeneralSettings.curr_camera = CamName;
            GeneralSettings.curr_pipeline = GetCurrentCamera().pipelines.keySet().stream().findFirst().toString();
        }
    }

    public void SetCurrentPipeline(String PipelineName) throws Exception {
        if (GetCurrentCamera().pipelines.containsKey(PipelineName)) {
            GeneralSettings.curr_pipeline = PipelineName;
        }
    }


    public void SetCameraSettings(String cameraName, String field, Object value) {
        switch (field) {
            case "brightness":
                UsbCameras.get(cameraName).setBrightness((int) value);
                break;
            case "exposure":
                UsbCameras.get(cameraName).setExposureManual((int) value);
                break;
            case "resolution":
                VideoMode videoMode = UsbCameras.get(cameraName).enumerateVideoModes()[(int) value];
                Camera cam = Cameras.get(cameraName);
                cam.camVideoMode.height = videoMode.height;
                cam.camVideoMode.width = videoMode.width;
                cam.camVideoMode.fps = videoMode.fps;
                //cam.camVideoMode.pixel_format=videoMode.pixelFormat.toString().split(".")[1];//legacy from python
                cam.camVideoMode.pixel_format = videoMode.pixelFormat.toString();
                break;
        }

    }

    //Savers
    public void SaveSettings() {
        SaveCameras();
        SaveGeneralSettings();
    }

    private void SaveCameras() {
        for (Map.Entry<String, Camera> entry : Cameras.entrySet()) {
            try {
                Gson gson = new Gson();
                FileWriter writer = new FileWriter(Paths.get(CamsPath.toString(), String.format("%s.json", entry.getKey())).toString());
                gson.toJson(entry.getValue(), writer);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void SaveGeneralSettings() {
        try {
            Gson gson = new Gson();
            FileWriter writer = new FileWriter(Paths.get(SettingsPath.toString(), "Settings.json").toString());
            new Gson().toJson(GeneralSettings, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
