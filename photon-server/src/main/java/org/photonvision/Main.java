/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision;

import java.util.HashMap;
import java.util.List;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.datatransfer.networktables.NetworkTablesManager;
import org.photonvision.common.logging.Level;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.server.Server;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceManager;

public class Main {
    private static final Logger logger = new Logger(Main.class, LogGroup.General);

    public static final int DEFAULT_WEBPORT = 5800;

    public static void main(String[] args) {
        Logger.setLevel(LogGroup.Camera, Level.DE_PEST);
        Logger.setLevel(LogGroup.Server, Level.DE_PEST);
        Logger.setLevel(LogGroup.VisionProcess, Level.DE_PEST);
        Logger.setLevel(LogGroup.Data, Level.DE_PEST);
        Logger.setLevel(LogGroup.General, Level.DE_PEST);

        TestUtils.loadLibraries();
        ConfigManager.getInstance(); // init config manager
        NetworkManager.getInstance().initialize(false); // basically empty. todo: link to ConfigManager?
        NetworkTablesManager.setClientMode("127.0.0.1");

        HashMap<String, CameraConfiguration> camConfigs =
                ConfigManager.getInstance().getConfig().getCameraConfigurations();
        logger.info("Loaded " + camConfigs.size() + " configs from disk!");
        List<VisionSource> sources = VisionSourceManager.loadAllSources(camConfigs.values());

        var collectedSources = new HashMap<VisionSource, List<CVPipelineSettings>>();
        for (var src : sources) {
            var usbSrc = (USBCameraSource) src;
            collectedSources.put(usbSrc, usbSrc.configuration.pipelineSettings);
        }

        logger.info("Adding " + collectedSources.size() + " configs to VMM");
        VisionModuleManager.getInstance().addSources(collectedSources);
        ConfigManager.getInstance().addCameraConfigurations(collectedSources);

        VisionModuleManager.getInstance().startModules();
        Server.main(DEFAULT_WEBPORT);
    }
}
