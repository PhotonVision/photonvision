package com.chameleonvision.vision;

import com.chameleonvision.config.CameraConfig;
import com.chameleonvision.config.CameraJsonConfig;
import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.config.FullCameraConfiguration;
import com.chameleonvision.util.Helpers;
import com.chameleonvision.util.Platform;
import com.chameleonvision.vision.camera.USBCameraCapture;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class VisionManager {
    private VisionManager() {}

    private static final LinkedHashMap<String, UsbCameraInfo> usbCameraInfosByCameraName = new LinkedHashMap<>();
    private static final LinkedList<FullCameraConfiguration> loadedCameraConfigs = new LinkedList<>();
    private static final LinkedList<VisionProcessManageable> visionProcesses = new LinkedList<>();

    @SuppressWarnings("WeakerAccess")
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
                // Filter non-ascii characters because ext4 doesn't play nice with unicode in directory names
                String name = info.name.replaceAll("[^\\x00-\\x7F]", "");
                while (usbCameraInfosByCameraName.containsKey(name)) {
                    suffix++;
                    name = String.format("%s (%d)", name, suffix);
                }
                usbCameraInfosByCameraName.put(name, info);
            }
        }

        if (usbCameraInfosByCameraName.isEmpty()) {
            return false;
        }
        System.out.printf("[VisionManager] Found %s cameras!\n", usbCameraInfosByCameraName.size());

        // load the config
        List<CameraJsonConfig> preliminaryConfigs = new ArrayList<>();

        usbCameraInfosByCameraName.forEach((suffixedName, cameraInfo) -> {
            String truePath;

            if (Platform.CurrentPlatform.isWindows()) {
                truePath = cameraInfo.path;
            } else {
                truePath = Arrays.stream(cameraInfo.otherPaths).filter(x -> x.contains("/dev/v4l/by-path")).findFirst().orElse(cameraInfo.path);
            }

            preliminaryConfigs.add(new CameraJsonConfig(truePath, suffixedName));
        });

        loadedCameraConfigs.addAll(ConfigManager.initializeCameras(preliminaryConfigs));
        System.out.printf("[VisionManager] Loaded %s cameras!\n", loadedCameraConfigs.size());
        return true;
    }

    public static boolean initializeProcesses() {
        for (int i = 0; i < loadedCameraConfigs.size(); i++) {
            FullCameraConfiguration config = loadedCameraConfigs.get(i);

            CameraJsonConfig cameraJsonConfig = config.cameraConfig;

            USBCameraCapture camera = new USBCameraCapture(config);
            VisionProcess process = new VisionProcess(camera, config);
            process.pipelineManager.driverModePipeline.settings = config.driverMode;
            visionProcesses.add(new VisionProcessManageable(i, cameraJsonConfig.name, process));
        }
        currentUIVisionProcess = getVisionProcessByIndex(0);
        ConfigManager.settings.currentCamera = visionProcesses.get(0).name;

        System.out.printf("[VisionManager] Loaded %s vision processes! Current process: %s\n", visionProcesses.size(), visionProcesses.get(0).name);
        return true;
    }

    public static void startProcesses() {
        visionProcesses.forEach((vpm) -> vpm.visionProcess.start());
    }

    public static VisionProcess getCurrentUIVisionProcess() {
        return currentUIVisionProcess;
    }

    public static CameraConfig getCurrentCameraConfig() {
        return getCameraConfig(currentUIVisionProcess);
    }

    public static CameraConfig getCameraConfig(VisionProcess process) {
        String cameraName = process.getCamera().getProperties().name;
        return Objects.requireNonNull(loadedCameraConfigs.stream().filter(x -> x.cameraConfig.name.equals(cameraName)).findFirst().orElse(null)).fileConfig;
    }

    public static void setCurrentProcessByIndex(int processIndex) {
        if (processIndex > visionProcesses.size() - 1) {
            return;
        }

        currentUIVisionProcess = getVisionProcessByIndex(processIndex);
        ConfigManager.settings.currentCamera = visionProcesses.get(processIndex).name;
    }

    public static VisionProcess getVisionProcessByIndex(int processIndex) {
        if (processIndex > visionProcesses.size() - 1) {
            return null;
        }

        VisionProcessManageable vpm =  visionProcesses.stream().filter(manageable -> manageable.index == processIndex).findFirst().orElse(null);
        return vpm != null ? vpm.visionProcess : null;
    }

    public static List<String> getAllCameraNicknames() {
        return visionProcesses.stream().map(vpm -> vpm.visionProcess.getCamera()
                .getProperties().getNickname()).collect(Collectors.toList());
    }

    public static List<String> getCurrentCameraPipelineNicknames() {
        return currentUIVisionProcess.pipelineManager.pipelines.stream().map(cvPipeline -> cvPipeline.settings.nickname).collect(Collectors.toList());
    }


    public static void saveAllCameras() {
        visionProcesses.forEach((vpm) -> {
            VisionProcess process = vpm.visionProcess;
            String cameraName = process.getCamera().getProperties().name;
            List<CVPipelineSettings> pipelines = process.pipelineManager.pipelines.stream().map(cvPipeline -> cvPipeline.settings).collect(Collectors.toList());
            CVPipelineSettings driverMode = process.getDriverModeSettings();
            CameraJsonConfig config = CameraJsonConfig.fromVisionProcess(process);
            ConfigManager.saveCameraPipelines(cameraName, pipelines);
            ConfigManager.saveCameraDriverMode(cameraName, driverMode);
            ConfigManager.saveCameraConfig(cameraName, config);
        });
    }

    private static String getCurrentCameraName() {
        return currentUIVisionProcess.getCamera().getProperties().name;
    }

    public static void saveCurrentCameraSettings() {
        CameraJsonConfig config = CameraJsonConfig.fromVisionProcess(currentUIVisionProcess);
        ConfigManager.saveCameraConfig(getCurrentCameraName(), config);
    }

    public static void saveCurrentCameraPipelines() {
        currentUIVisionProcess.pipelineManager.saveAllPipelines();
    }

    public static void saveCurrentCameraDriverMode() {
        currentUIVisionProcess.pipelineManager.saveDriverModeConfig();
    }

    private static List<HashMap> getCameraResolutionList(USBCameraCapture capture) {
        return capture.getProperties().getVideoModes().stream().map(Helpers::VideoModeToHashMap).collect(Collectors.toList());
    }

    public static List<HashMap> getCurrentCameraResolutionList() {
        return getCameraResolutionList(currentUIVisionProcess.getCamera());
    }

    public static int getCurrentUIVisionProcessIndex() {
        VisionProcessManageable vpm = visionProcesses.stream().filter(v -> v.visionProcess == currentUIVisionProcess).findFirst().orElse(null);
        return vpm != null ? vpm.index : -1;
    }
}
