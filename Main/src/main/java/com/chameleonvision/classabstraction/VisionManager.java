package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.config.ConfigManager;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.util.FileHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class VisionManager {

    private VisionManager() {}

    private static final Path CamConfigPath = Paths.get(SettingsManager.SettingsPath.toString(), "cameras");

    public static final LinkedHashMap<String, UsbCameraInfo> UsbCameraInfosByCameraName = new LinkedHashMap<>();
    public static final LinkedHashMap<String, VisionProcess> VisionProcessesByCameraName = new LinkedHashMap<>();

    public static boolean initializeSources() {
        int suffix = 0;
        for (UsbCameraInfo info : UsbCamera.enumerateUsbCameras()) {
            VideoCapture cap = new VideoCapture(info.dev);
            if (cap.isOpened()) {
                cap.release();
                String name = info.name;
                while (UsbCameraInfosByCameraName.containsKey(name)) {
                    suffix++;
                    name = String.format("%s (%d)", name, suffix);
                }
                UsbCameraInfosByCameraName.put(name, info);
            }
        }

        if (UsbCameraInfosByCameraName.isEmpty()) {
            return false;
        }

        FileHelper.CheckPath(CamConfigPath);

        // load the config
        var loadedConfigs = ConfigManager.initializeCameraConfig(new ArrayList<>(UsbCameraInfosByCameraName.keySet()));

//        UsbCameraInfosByCameraName.forEach((cameraName, cameraInfo) -> {
//            Path cameraConfigFolder = Paths.get(CamConfigPath.toString(), String.format("%s\\", cameraName));
//            Path cameraConfigPath = Paths.get(cameraConfigFolder.toString(), String.format("%s.json", cameraName));
//            Path cameraPipelinesPath = Paths.get(cameraConfigFolder.toString(), "pipelines.json");
//            Path cameraDrivermodePath = Paths.get(cameraConfigFolder.toString(), "drivermode.json");
//
//            try {
//
//                boolean cameraFolderExists = Files.exists(cameraConfigFolder);
//
//                if (!cameraFolderExists) {
//                    Files.createDirectory(cameraConfigFolder);
//                }
//                boolean cameraConfigExists = cameraFolderExists && Files.exists(cameraConfigPath);
//
//                if (Files.exists(cameraConfigFolder)) {
//                    if (Files.exists(cameraConfigPath)) {
//                        File cameraConfigFile = new File(cameraConfigPath.toString());
//                        if (cameraConfigFile.length() != 0) {
//                            try {
//                                Gson gson  = new GsonBuilder().create();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    } else {
//                        Files.createFile(cameraConfigPath);
//                    }
//                } else {
//                    Files.createDirectory(cameraConfigFolder);
//                }
//            } catch (IOException ex) {
//
//            }
//
//
//        });

//        FileHelper.CheckPath(CamConfigPath);
//        UsbCameraInfosByCameraName.forEach((cameraName, cameraInfo) -> {
//            Path cameraConfigPath = Paths.get(CamConfigPath.toString(), String.format("%s.json", cameraName));
//            File cameraConfigFile = new File(cameraConfigPath.toString());
//            if (cameraConfigFile.exists() && cameraConfigFile.length() != 0) {
////                try {
////                    Gson gson = new GsonBuilder().registerTypeAdapter(USBCameraProcess.class, new CameraDeserializer());
////                }
//            }
//        })
        // TODO: implement new camera JSON loads
        return true;
    }

    public static boolean initializeProcesses() {

        return true;
    }
}
