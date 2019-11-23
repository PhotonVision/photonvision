package com.chameleonvision.vision;

import com.chameleonvision.config.CameraConfig;
import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.util.Platform;
import com.chameleonvision.vision.camera.CameraProcess;
import com.chameleonvision.vision.camera.USBCameraProcess;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class VisionManager {
    private VisionManager() {
    }

    private static final LinkedHashMap<String, UsbCameraInfo> UsbCameraInfosByCameraName = new LinkedHashMap<>();
    private static final LinkedList<CameraConfig> LoadedCameraConfigs = new LinkedList<>();
    private static final LinkedHashMap<Integer, Pair<VisionProcess, String>> VisionProcessesByIndex = new LinkedHashMap<>();

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
            VisionProcessesByIndex.put(i, Pair.of(process, config.name));
        }
        currentUIVisionProcess = VisionProcessesByIndex.get(0).getLeft();
        return true;
    }

    public static void startProcesses() {
        VisionProcessesByIndex.forEach((index, processNamePair) -> {
            processNamePair.getLeft().start();
        });
    }

    public static VisionProcess getCurrentUIVisionProcess() {
        return currentUIVisionProcess;
    }

    public static void setCurrentProcessByIndex(int processIndex) {
        if (processIndex > VisionProcessesByIndex.size() - 1) {
            return;
        }

        currentUIVisionProcess = VisionProcessesByIndex.get(processIndex).getLeft();
    }

    public static VisionProcess getVisionProcessByIndex(int processIndex) {
        if (processIndex > VisionProcessesByIndex.size() - 1) {
            return null;
        }

        return VisionProcessesByIndex.get(0).getLeft();
    }

    public static List<String> getAllCameraNicknames() {
        return VisionProcessesByIndex.values().stream().map(processNamePair -> processNamePair.getLeft().getCamera().getProperties().getNickname()).collect(Collectors.toList());
    }

    public static void saveCameras() {
        VisionProcessesByIndex.forEach((index, process) -> {
            VisionProcess p = process.getLeft();
            String cameraName = process.getRight();
            List<CVPipelineSettings> pipelines = p.getPipelines().stream().map(cvPipeline -> cvPipeline.settings).collect(Collectors.toList());
            CVPipelineSettings driverMode = p.getDriverModeSettings();
            CameraConfig config = CameraConfig.fromUSBCameraProcess((USBCameraProcess) p.getCamera());
            try {
                ConfigManager.saveCameraPipelines(cameraName, pipelines);
                ConfigManager.saveCameraDriverMode(cameraName, driverMode);
                ConfigManager.saveCameraConfig(cameraName, config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
