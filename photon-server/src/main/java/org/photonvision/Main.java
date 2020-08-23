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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.cli.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.server.Server;
import org.photonvision.vision.camera.FileVisionSource;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceManager;
import org.photonvision.vision.target.TargetModel;

public class Main {
    public static final int DEFAULT_WEBPORT = 5800;

    private static final Logger logger = new Logger(Main.class, LogGroup.General);
    private static final boolean isRelease = PhotonVersion.isRelease;

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
                    "Run in test mode with 2019 and 2020 WPI field images in place of cameras");
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

    private static void addTestModeSources() {
        var collectedSources = new HashMap<VisionSource, List<CVPipelineSettings>>();

        var camConf2019 =
                new CameraConfiguration("WPI2019", TestUtils.getTestMode2019ImagePath().toString());
        camConf2019.FOV = TestUtils.WPI2019Image.FOV;
        camConf2019.calibrations.add(TestUtils.get2019LifeCamCoeffs(true));

        var pipeline2019 = new ReflectivePipelineSettings();
        pipeline2019.pipelineNickname = "CargoShip";
        pipeline2019.targetModel = TargetModel.get2019Target();

        var psList2019 = new ArrayList<CVPipelineSettings>();
        psList2019.add(pipeline2019);

        var fvs2019 = new FileVisionSource(camConf2019);

        var camConf2020 =
                new CameraConfiguration("WPI2020", TestUtils.getTestMode2020ImagePath().toString());
        camConf2020.FOV = TestUtils.WPI2020Image.FOV;
        camConf2019.calibrations.add(TestUtils.get2019LifeCamCoeffs(true));

        var pipeline2020 = new ReflectivePipelineSettings();
        pipeline2020.pipelineNickname = "OuterPort";
        pipeline2020.targetModel = TargetModel.get2020Target();
        camConf2020.calibrations.add(TestUtils.get2020LifeCamCoeffs(true));

        var psList2020 = new ArrayList<CVPipelineSettings>();
        psList2020.add(pipeline2020);

        var fvs2020 = new FileVisionSource(camConf2020);

        collectedSources.put(fvs2019, psList2019);
        collectedSources.put(fvs2020, psList2020);

        //                logger.info("Adding " + allSources.size() + " configs to VMM.");
        VisionModuleManager.getInstance().addSources(collectedSources).forEach(VisionModule::start);
        ConfigManager.getInstance().addCameraConfigurations(collectedSources);
    }

    public static void main(String[] args) {
        try {
            if (!handleArgs(args)) return;
        } catch (ParseException e) {
            logger.error("Failed to parse command-line options!", e);
        }

        logger.info("Running in " + (isRelease ? "release" : "development") + " mode!");
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
            TestUtils.loadLibraries();
            logger.info("Native libraries loaded.");
        } catch (Exception e) {
            logger.error("Failed to load native libraries!", e);
        }

        ConfigManager.getInstance().load(); // init config manager
        ConfigManager.getInstance().requestSave();

        NetworkManager.getInstance().initialize(false);

        NetworkTablesManager.getInstance()
                .setConfig(ConfigManager.getInstance().getConfig().getNetworkConfig());

        //        HashMap<VisionSource, List<CVPipelineSettings>> allSources = gatherSources();

        //        logger.info("Adding " + allSources.size() + " configs to VMM.");
        //        VisionModuleManager.getInstance().addSources(allSources);
        //        ConfigManager.getInstance().addCameraConfigurations(allSources);

        if (!isTestMode) {
            VisionSourceManager.getInstance()
                    .registerLoadedConfigs(
                            ConfigManager.getInstance().getConfig().getCameraConfigurations().values());
            VisionSourceManager.getInstance().registerTimedTask();
        } else {
            addTestModeSources();
        }

        // Add hardware config to hardware manager
        HardwareManager.getInstance()
                .setConfig(ConfigManager.getInstance().getConfig().getHardwareConfig());

        Server.main(DEFAULT_WEBPORT);
    }
}
