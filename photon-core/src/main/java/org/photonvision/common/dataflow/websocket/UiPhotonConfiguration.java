package org.photonvision.common.dataflow.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;

public class UiPhotonConfiguration {
    public static Map<String, Object> toHashMap(PhotonConfiguration c) {
        Map<String, Object> map = new HashMap<>();
        var settingsSubmap = new HashMap<String, Object>();

        // Hack active interfaces into networkSettings
        var netConfigMap = c.getNetworkConfig().toHashMap();
        netConfigMap.put("networkInterfaceNames", NetworkUtils.getAllActiveWiredInterfaces());
        netConfigMap.put("networkingDisabled", NetworkManager.getInstance().networkingIsDisabled);

        settingsSubmap.put("networkSettings", netConfigMap);

        var lightingConfig = new UILightingConfig();
        lightingConfig.brightness = c.getHardwareSettings().ledBrightnessPercentage;
        lightingConfig.supported = !c.getHardwareConfig().ledPins.isEmpty();
        settingsSubmap.put("lighting", SerializationUtils.objectToHashMap(lightingConfig));
        // General Settings
        var generalSubmap = new HashMap<String, Object>();
        generalSubmap.put("version", PhotonVersion.versionString);
        generalSubmap.put(
                "gpuAcceleration",
                LibCameraJNILoader.isSupported()
                        ? "Zerocopy Libcamera Working"
                        : ""); // TODO add support for other types of GPU accel
        generalSubmap.put("mrCalWorking", MrCalJNILoader.getInstance().isLoaded());
        generalSubmap.put("availableModels", NeuralNetworkModelManager.getInstance().getModels());
        generalSubmap.put(
                "supportedBackends", NeuralNetworkModelManager.getInstance().getSupportedBackends());
        generalSubmap.put(
                "hardwareModel",
                c.getHardwareConfig().deviceName.isEmpty()
                        ? Platform.getHardwareModel()
                        : c.getHardwareConfig().deviceName);
        generalSubmap.put("hardwarePlatform", Platform.getPlatformName());
        settingsSubmap.put("general", generalSubmap);
        // AprilTagFieldLayout
        settingsSubmap.put("atfl", c.getApriltagFieldLayout());

        map.put(
                "cameraSettings",
                VisionModuleManager.getInstance().getModules().stream()
                        .map(VisionModule::toUICameraConfig)
                        .map(SerializationUtils::objectToHashMap)
                        .collect(Collectors.toList()));
        map.put("settings", settingsSubmap);

        return map;
    }
}
