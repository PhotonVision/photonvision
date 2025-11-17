/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.dataflow.websocket;

import java.util.List;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.configuration.PhotonConfiguration;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.TestUtils;
import org.photonvision.raspi.LibCameraJNILoader;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionSourceManager;

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
                                LibCameraJNILoader.getInstance().isSupported() ? "Zerocopy Libcamera Working" : "",
                                TestUtils.isMrcalLoaded(),
                                c.neuralNetworkPropertyManager().getModels(),
                                NeuralNetworkModelManager.getInstance().getSupportedBackends(),
                                c.getHardwareConfig().deviceName.isEmpty()
                                        ? Platform.getHardwareModel()
                                        : c.getHardwareConfig().deviceName,
                                Platform.getPlatformName(),
                                NetworkTablesManager.getInstance().conflictingHostname,
                                NetworkTablesManager.getInstance().conflictingCameras),
                        c.getApriltagFieldLayout()),
                VisionSourceManager.getInstance().getVisionModules().stream()
                        .map(VisionModule::toUICameraConfig)
                        .toList());
    }
}
