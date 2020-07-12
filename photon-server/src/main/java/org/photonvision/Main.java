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

import edu.wpi.cscore.CameraServerCvJNI;
import java.util.HashMap;
import java.util.List;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.Platform;
import org.photonvision.server.Server;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceManager;

public class Main {
    private static final Logger logger = new Logger(Main.class, LogGroup.General);
    public static final int DEFAULT_WEBPORT = 5800;

    private static String getVersion() {
        return "2020.7.1"; // TODO: grab from gradle/resource somehow
    }

    public static void main(String[] args) {
        Logger.setLevel(LogGroup.Camera, LogLevel.TRACE);
        Logger.setLevel(LogGroup.WebServer, LogLevel.TRACE);
        Logger.setLevel(LogGroup.VisionModule, LogLevel.TRACE);
        Logger.setLevel(LogGroup.Data, LogLevel.TRACE);
        Logger.setLevel(LogGroup.General, LogLevel.TRACE);

        logger.info("Logging initialized!");

        logger.info(
                "Starting PhotonVision version "
                        + getVersion()
                        + " on "
                        + Platform.CurrentPlatform.toString());
        try {
            logger.info("Loading native libraries...");
            CameraServerCvJNI.forceLoad();
        } catch (Exception e) {
            logger.error("Failed to load native libraries!");
            e.printStackTrace(); // TODO: redirect stacktrace to Logger stream somehow
        }
        logger.info("Native libaries loaded.");

        ConfigManager.getInstance(); // init config manager
        NetworkManager.getInstance().initialize(false); // basically empty. todo: link to ConfigManager?
        NetworkTablesManager.setClientMode("127.0.0.1");

        HashMap<String, CameraConfiguration> camConfigs =
                ConfigManager.getInstance().getConfig().getCameraConfigurations();
        logger.info("Loaded " + camConfigs.size() + " configs from disk.");
        List<VisionSource> sources = VisionSourceManager.loadAllSources(camConfigs.values());

        var collectedSources = new HashMap<VisionSource, List<CVPipelineSettings>>();
        for (var src : sources) {
            var usbSrc = (USBCameraSource) src;
            collectedSources.put(usbSrc, usbSrc.configuration.pipelineSettings);
            logger.trace(
                    () -> {
                        return "Matched config for camera \""
                                + src.getFrameProvider().getName()
                                + "\" and loaded "
                                + usbSrc.configuration.pipelineSettings.size()
                                + " pipelines";
                    });
        }

        logger.info("Adding " + collectedSources.size() + " configs to VMM.");
        VisionModuleManager.getInstance().addSources(collectedSources);
        ConfigManager.getInstance().addCameraConfigurations(collectedSources);

        VisionModuleManager.getInstance().startModules();
        Server.main(DEFAULT_WEBPORT);
    }
}
