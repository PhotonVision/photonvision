package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.USBCameraProcess;
import com.chameleonvision.classabstraction.config.CameraConfig;
import com.chameleonvision.classabstraction.config.ConfigManager;
import com.chameleonvision.settings.Platform;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.util.FileHelper;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VisionManager {

    private VisionManager() {}

    private static final Path CamConfigPath = Paths.get(SettingsManager.SettingsPath.toString(), "cameras");

    public static final LinkedHashMap<String, UsbCameraInfo> UsbCameraInfosByCameraName = new LinkedHashMap<>();
    private static final LinkedList<CameraConfig> LoadedCameraConfigs = new LinkedList<>();
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
        List<CameraConfig> preliminaryConfigs = new ArrayList<>();

        UsbCameraInfosByCameraName.values().forEach((cameraInfo) -> {
            String truePath;

            if (Platform.CurrentPlatform.isWindows()) {
                truePath = cameraInfo.path;
            } else {
                truePath = Arrays.stream(cameraInfo.otherPaths).filter(x -> x.contains("/dev/v4l/by-path")).findFirst().orElse(cameraInfo.path);
            }

           preliminaryConfigs.add(new CameraConfig(truePath, cameraInfo.name));
        });

        LoadedCameraConfigs.addAll(ConfigManager.initializeCameraConfig(preliminaryConfigs));
//        UsbCameraInfosByCameraName.forEach((cameraName, cameraInfo) -> {
//            Path cameraConfigFolder = Paths.get(CamConfigPath.toString(), String.format("%s\\", cameraName));
//            Path cameraConfigPath = Paths.get(cameraConfigFolder.toString(), String.format("%s.json", cameraName));
//            Path cameraPipelinesPath = Paths.get(cameraConfigFolder.toString(), "pipelines.json");
//            Path cameraDrivermodePath = Paths.get(cameraConfigFolder.toString(), "drivermode.json");

        return true;
    }

    public static boolean initializeProcesses() {
        LoadedCameraConfigs.forEach(config -> {
            var camera = new USBCameraProcess(config);
            VisionProcessesByCameraName.put(config.name, new VisionProcess(camera, config.name));
        });
        return true;
    }

    public static void startProcesses() {
        VisionProcessesByCameraName.forEach((name, process) -> {
            process.start();
        });
    }
}
