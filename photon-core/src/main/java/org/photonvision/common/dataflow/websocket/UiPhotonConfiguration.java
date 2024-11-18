package org.photonvision.common.dataflow.websocket;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.NetworkConfig;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.networking.NetworkUtils.NMDeviceInfo;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;

public class UiPhotonConfiguration {
    public static class UiNetConfig extends NetworkConfig {
        public UiNetConfig(
                NetworkConfig config,
                List<NMDeviceInfo> networkInterfaceNames,
                boolean networkingDisabled) {
            super(config);
            this.networkInterfaceNames = networkInterfaceNames;
            this.networkingDisabled = networkingDisabled;
        }

        public List<NMDeviceInfo> networkInterfaceNames;
        public boolean networkingDisabled;
    }

    public static class GeneralSettings {
        public GeneralSettings(
                String version,
                String gpuAcceleration,
                boolean mrCalWorking,
                Map<String, ArrayList<String>> availableModels,
                List<String> supportedBackends,
                String hardwareModel,
                String hardwarePlatform) {
            this.version = version;
            this.gpuAcceleration = gpuAcceleration;
            this.mrCalWorking = mrCalWorking;
            this.availableModels = availableModels;
            this.supportedBackends = supportedBackends;
            this.hardwareModel = hardwareModel;
            this.hardwarePlatform = hardwarePlatform;
        }

        public String version;
        public String gpuAcceleration;
        public boolean mrCalWorking;
        public Map<String, ArrayList<String>> availableModels;
        public List<String> supportedBackends;
        public String hardwareModel;
        public String hardwarePlatform;
    }

    public static class ProgramSettings {
        public ProgramSettings(
                UiNetConfig networkSettings,
                UILightingConfig lighting,
                GeneralSettings general,
                AprilTagFieldLayout atfl) {
            this.networkSettings = networkSettings;
            this.lighting = lighting;
            this.general = general;
            this.atfl = atfl;
        }

        public UiNetConfig networkSettings;
        public UILightingConfig lighting;
        public GeneralSettings general;
        public AprilTagFieldLayout atfl;
    }

    public List<UICameraConfiguration> cameraSettings;
    public ProgramSettings settings;

    public static UiPhotonConfiguration programStateToUi(PhotonConfiguration c) {
        var ret = new UiPhotonConfiguration();

        ret.settings =
                new ProgramSettings(
                        new UiNetConfig(
                                c.getNetworkConfig(),
                                NetworkUtils.getAllActiveWiredInterfaces(),
                                NetworkManager.getInstance().networkingIsDisabled),
                        new UILightingConfig(
                                c.getHardwareSettings().ledBrightnessPercentage,
                                !c.getHardwareConfig().ledPins.isEmpty()),
                        new GeneralSettings(
                                PhotonVersion.versionString,
                                // TODO add support for other types of GPU accel
                                LibCameraJNILoader.isSupported() ? "Zerocopy Libcamera Working" : "",
                                MrCalJNILoader.getInstance().isLoaded(),
                                NeuralNetworkModelManager.getInstance().getModels(),
                                NeuralNetworkModelManager.getInstance().getSupportedBackends(),
                                c.getHardwareConfig().deviceName.isEmpty()
                                        ? Platform.getHardwareModel()
                                        : c.getHardwareConfig().deviceName,
                                Platform.getPlatformName()),
                        c.getApriltagFieldLayout());

        ret.cameraSettings =
                VisionModuleManager.getInstance().getModules().stream()
                        .map(VisionModule::toUICameraConfig)
                        .collect(Collectors.toList());

        return ret;
    }
}
