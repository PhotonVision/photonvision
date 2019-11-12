package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.USBCameraProcess;
import com.chameleonvision.classabstraction.pipeline.CVPipelineSettings;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.util.FileHelper;
import com.chameleonvision.vision.camera.CameraDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class VisionManager {

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
        UsbCameraInfosByCameraName.forEach((cameraName, cameraInfo) -> {
            Path cameraConfigPath = Paths.get(CamConfigPath.toString(), String.format("%s.json", cameraName));
            File cameraConfigFile = new File(cameraConfigPath.toString());
            if (cameraConfigFile.exists() && cameraConfigFile.length() != 0) {
//                try {
//                    Gson gson = new GsonBuilder().registerTypeAdapter(USBCameraProcess.class, new CameraDeserializer());
//                }
            }
        });
        return true;
    }


    public static void initializeProcesses() {

    }
}
