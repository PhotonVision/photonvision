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
import org.apache.commons.cli.*;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.server.Server;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceManager;

public class Main {
    public static final int DEFAULT_WEBPORT = 5800;

    private static final Logger logger = new Logger(Main.class, LogGroup.General);
    private static final boolean isRelease =
            !PhotonVersion.isRelease; // Hack!!!! Until PhotonVersion script fixed

    private static boolean isTestMode;
    private static boolean printDebugLogs;

    private static boolean handleArgs(String[] args) throws ParseException {
        final var options = new Options();
        options.addOption("d", "debug", false, "Enable debug logging prints");
        options.addOption("h", "help", false, "Show this help text and exit");
        if (!isRelease) {
            options.addOption(
                    "t",
                    "test-mode",
                    false,
                    "Run in test mode with a 2019 and 2020 WPI field image as available cameras");
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar photonvision.jar [options]", options);
            return false; // exit program
        } else {
            if (cmd.hasOption("debug")) {
                printDebugLogs = true;
                logger.info("Enabled debug logging");
            }

            if (cmd.hasOption("test-mode")) {
                isTestMode = true;
                logger.info("Running in test mode - Cameras will not be used");
            }
        }
        return true;
    }

    private static HashMap<VisionSource, List<CVPipelineSettings>> gatherSources() {
        if (!isTestMode) {
            var camConfigs = ConfigManager.getInstance().getConfig().getCameraConfigurations();
            logger.info("Loaded " + camConfigs.size() + " configs from disk.");
            var sources = VisionSourceManager.loadAllSources(camConfigs.values());

            var collectedSources = new HashMap<VisionSource, List<CVPipelineSettings>>();
            for (var src : sources) {
                var usbSrc = (USBCameraSource) src;
                collectedSources.put(usbSrc, usbSrc.configuration.pipelineSettings);
                logger.debug(
                        () ->
                                "Matched config for camera \""
                                        + src.getFrameProvider().getName()
                                        + "\" and loaded "
                                        + usbSrc.configuration.pipelineSettings.size()
                                        + " pipelines");
            }
            return collectedSources;
        } else {
            // todo: test mode
            return new HashMap<>();
        }
    }

    public static void main(String[] args) {
        try {
            if (!handleArgs(args)) return;
        } catch (ParseException e) {
            logger.error("Failed to parse command-line options!", e);
        }

        var logLevel = (isRelease || printDebugLogs) ? LogLevel.INFO : LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
        logger.info("Logging initialized in " + (isRelease ? "Release" : "Debug") + " mode.");

        logger.info(
                "Starting PhotonVision version "
                        + PhotonVersion.versionString
                        + " on "
                        + Platform.CurrentPlatform.toString());

        try {
            CameraServerCvJNI.forceLoad();
            logger.info("Native libraries loaded.");
        } catch (Exception e) {
            logger.error("Failed to load native libraries!", e);
        }

        ConfigManager.getInstance(); // init config manager
        NetworkManager.getInstance().initialize(false); // basically empty. todo: link to ConfigManager?
        NetworkTablesManager.setClientMode("127.0.0.1");

        HashMap<VisionSource, List<CVPipelineSettings>> allSources = gatherSources();

        logger.info("Adding " + allSources.size() + " configs to VMM.");
        VisionModuleManager.getInstance().addSources(allSources);
        ConfigManager.getInstance().addCameraConfigurations(allSources);

        VisionModuleManager.getInstance().startModules();
        Server.main(DEFAULT_WEBPORT);
    }
}
