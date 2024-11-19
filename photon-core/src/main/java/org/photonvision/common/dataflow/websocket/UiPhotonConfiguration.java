package org.photonvision.common.dataflow.websocket;

import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;

public class UIPhotonConfiguration {
    public List<UICameraConfiguration> cameraSettings;
    public UIProgramSettings settings;

    public UIPhotonConfiguration(
            UIProgramSettings settings, List<UICameraConfiguration> cameraSettings) {
        this.cameraSettings = cameraSettings;
        this.settings = settings;
    }

    public static UIPhotonConfiguration programStateToUi(PhotonConfiguration c) {
        return new UIPhotonConfiguration(
                new UIProgramSettings(
                        new UINetConfig(
                                c.getNetworkConfig(),
                                NetworkUtils.getAllActiveWiredInterfaces(),
                                NetworkManager.getInstance().networkingIsDisabled),
                        new UILightingConfig(
                                c.getHardwareSettings().ledBrightnessPercentage,
                                !c.getHardwareConfig().ledPins.isEmpty()),
                        new UIGeneralSettings(
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
                        c.getApriltagFieldLayout()),
                VisionModuleManager.getInstance().getModules().stream()
                        .map(VisionModule::toUICameraConfig)
                        .collect(Collectors.toList()));
    }
}
