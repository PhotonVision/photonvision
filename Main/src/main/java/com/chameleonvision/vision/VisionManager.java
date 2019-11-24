package com.chameleonvision.vision;

import com.chameleonvision.config.CameraConfig;
import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.util.Helpers;
import com.chameleonvision.util.Platform;
import com.chameleonvision.vision.camera.CameraProcess;
import com.chameleonvision.vision.camera.USBCameraProcess;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class VisionManager {
    private VisionManager() {
    }

    private static final LinkedHashMap<String, UsbCameraInfo> UsbCameraInfosByCameraName = new LinkedHashMap<>();
    private static final LinkedList<CameraConfig> LoadedCameraConfigs = new LinkedList<>();
    private static final LinkedList<VisionProcessManageable> VisionProcesses = new LinkedList<>();

    private static class VisionProcessManageable {
        public final int index;
        public final String name;
        public final VisionProcess visionProcess;

        public VisionProcessManageable(int index, String name, VisionProcess visionProcess) {
            this.index = index;
            this.name = name;
            this.visionProcess = visionProcess;
        }
    }

    private static VisionProcess currentUIVisionProcess;

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

        // TODO: (HIGH) Load pipelines from json
//        UsbCameraInfosByCameraName.forEach((cameraName, cameraInfo) -> {
//            Path cameraConfigFolder = Paths.get(CamConfigPath.toString(), String.format("%s\\", cameraName));
//            Path cameraConfigPath = Paths.get(cameraConfigFolder.toString(), String.format("%s.json", cameraName));
//            Path cameraPipelinesPath = Paths.get(cameraConfigFolder.toString(), "pipelines.json");
//            Path cameraDrivermodePath = Paths.get(cameraConfigFolder.toString(), "drivermode.json");

        return true;
    }

    public static boolean initializeProcesses() {
        for (int i = 0; i < LoadedCameraConfigs.size(); i++) {
            CameraConfig config = LoadedCameraConfigs.get(i);
            CameraProcess camera = new USBCameraProcess(config);
            VisionProcess process = new VisionProcess(camera, config.name);
            VisionProcesses.add(new VisionProcessManageable(i, config.name, process));
        }
        currentUIVisionProcess = getVisionProcessByIndex(0);
        return true;
    }

    public static void startProcesses() {
        VisionProcesses.forEach((vpm) -> {
            vpm.visionProcess.start();
        });
        ServerHandler.sendFullSettings();
    }

    public static VisionProcess getCurrentUIVisionProcess() {
        return currentUIVisionProcess;
    }

    public static void setCurrentProcessByIndex(int processIndex) {
        if (processIndex > VisionProcesses.size() - 1) {
            return;
        }

        currentUIVisionProcess = getVisionProcessByIndex(0);
    }

    public static VisionProcess getVisionProcessByIndex(int processIndex) {
        if (processIndex > VisionProcesses.size() - 1) {
            return null;
        }

        VisionProcessManageable vpm =  VisionProcesses.stream().filter(manageable -> manageable.index == processIndex).findFirst().orElse(null);
        return vpm != null ? vpm.visionProcess : null;
    }

    public static List<String> getAllCameraNicknames() {
        return VisionProcesses.stream().map(vpm -> vpm.visionProcess.getCamera()
                .getProperties().getNickname()).collect(Collectors.toList());
    }

    public static List<String> getCurrentCameraPipelineNicknames() {
        return currentUIVisionProcess.getPipelines().stream().map(cvPipeline -> cvPipeline.settings.nickname).collect(Collectors.toList());
    }

    public static void saveCameras() {
        VisionProcesses.forEach((vpm) -> {
            VisionProcess process = vpm.visionProcess;
            String cameraName = process.getCamera().getProperties().name;
            List<CVPipelineSettings> pipelines = process.getPipelines().stream().map(cvPipeline -> cvPipeline.settings).collect(Collectors.toList());
            CVPipelineSettings driverMode = process.getDriverModeSettings();
            CameraConfig config = CameraConfig.fromUSBCameraProcess((USBCameraProcess) process.getCamera());
            try {
                ConfigManager.saveCameraPipelines(cameraName, pipelines);
                ConfigManager.saveCameraDriverMode(cameraName, driverMode);
                ConfigManager.saveCameraConfig(cameraName, config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static List<String> getCurrentCameraResolutionList() {
        return currentUIVisionProcess.getCamera().getProperties().getVideoModes().stream().map(Helpers::VideoModeToString).collect(Collectors.toList());
    }

    public static int getCurrentUIVisionProcessIndex() {
        VisionProcessManageable vpm = VisionProcesses.stream().filter(v -> v.visionProcess == currentUIVisionProcess).findFirst().orElse(null);
        return vpm != null ? vpm.index : -1;
    }
}
