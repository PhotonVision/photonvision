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

package org.photonvision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.cli.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.PiVersion;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.server.Server;
import org.photonvision.vision.camera.FileVisionSource;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.ColoredShapePipelineSettings;
import org.photonvision.vision.pipeline.PipelineProfiler;
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

    private static boolean isTestMode = false;
    private static Path testModeFolder = null;
    private static boolean printDebugLogs;

    private static boolean handleArgs(String[] args) throws ParseException {
        final var options = new Options();
        options.addOption("d", "debug", false, "Enable debug logging prints");
        options.addOption("h", "help", false, "Show this help text and exit");
        options.addOption(
                "t",
                "test-mode",
                false,
                "Run in test mode with 2019 and 2020 WPI field images in place of cameras");

        options.addOption("p", "path", true, "Point test mode to a specific folder");

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

                if (cmd.hasOption("path")) {
                    Path p = Path.of(System.getProperty("PATH_PREFIX", "") + cmd.getOptionValue("path"));
                    logger.info("Loading from Path " + p.toAbsolutePath().toString());
                    testModeFolder = p;
                }
            }
        }
        return true;
    }

    private static void addTestModeFromFolder() {
        ConfigManager.getInstance().load();

        try {
            var reflective = new ReflectivePipelineSettings();
            var shape = new ColoredShapePipelineSettings();
            var aprilTag = new AprilTagPipelineSettings();
            List<VisionSource> collectedSources =
                    Files.list(testModeFolder)
                            .filter(p -> p.toFile().isFile())
                            .map(
                                    p -> {
                                        try {
                                            //                                            var camConf =
                                            //
                                            // ConfigManager.getInstance()
                                            //                                                            .getConfig()
                                            //
                                            // .getCameraConfigurations()
                                            //
                                            // .get(p.getFileName().toString());

                                            //                                            if (camConf == null && false) {
                                            CameraConfiguration camConf;
                                            if (true) {
                                                camConf =
                                                        new CameraConfiguration(
                                                                p.getFileName().toString(), p.toAbsolutePath().toString());
                                                camConf.FOV = TestUtils.WPI2019Image.FOV; // Good guess?
                                                camConf.addCalibration(TestUtils.get2020LifeCamCoeffs(false));

                                                var pipeSettings = new AprilTagPipelineSettings();
                                                pipeSettings.pipelineNickname = p.getFileName().toString();
                                                pipeSettings.outputShowMultipleTargets = true;
                                                pipeSettings.inputShouldShow = true;
                                                pipeSettings.outputShouldShow = false;
                                                pipeSettings.solvePNPEnabled = true;

                                                var psList = new ArrayList<CVPipelineSettings>();
                                                //                                                psList.add(reflective);
                                                //                                                psList.add(shape);
                                                psList.add(aprilTag);
                                                camConf.pipelineSettings = psList;
                                            }

                                            return new FileVisionSource(camConf);
                                        } catch (Exception e) {
                                            logger.error("Couldn't load image " + p.getFileName().toString(), e);
                                            return null;
                                        }
                                    })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            ConfigManager.getInstance().unloadCameraConfigs();
            VisionModuleManager.getInstance().addSources(collectedSources).forEach(VisionModule::start);
            ConfigManager.getInstance().addCameraConfigurations(collectedSources);
        } catch (IOException e) {
            logger.error("Path does not exist!");
            System.exit(1);
        }
    }

    private static void addTestModeSources() {
        ConfigManager.getInstance().load();

        var camConf2019 =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get("WPI2019");
        if (camConf2019 == null) {
            camConf2019 =
                    new CameraConfiguration("WPI2019", TestUtils.getTestMode2019ImagePath().toString());
            camConf2019.FOV = TestUtils.WPI2019Image.FOV;
            camConf2019.calibrations.add(TestUtils.get2019LifeCamCoeffs(true));

            var pipeline2019 = new ReflectivePipelineSettings();
            pipeline2019.pipelineNickname = "CargoShip";
            pipeline2019.targetModel = TargetModel.k2019DualTarget;
            pipeline2019.outputShowMultipleTargets = true;
            pipeline2019.contourGroupingMode = ContourGroupingMode.Dual;
            pipeline2019.inputShouldShow = true;

            var psList2019 = new ArrayList<CVPipelineSettings>();
            psList2019.add(pipeline2019);
            camConf2019.pipelineSettings = psList2019;
        }

        var camConf2020 =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get("WPI2020");
        if (camConf2020 == null) {
            camConf2020 =
                    new CameraConfiguration("WPI2020", TestUtils.getTestMode2020ImagePath().toString());
            camConf2020.FOV = TestUtils.WPI2020Image.FOV;
            camConf2020.calibrations.add(TestUtils.get2019LifeCamCoeffs(true));

            var pipeline2020 = new ReflectivePipelineSettings();
            pipeline2020.pipelineNickname = "OuterPort";
            pipeline2020.targetModel = TargetModel.k2020HighGoalOuter;
            camConf2020.calibrations.add(TestUtils.get2020LifeCamCoeffs(true));
            pipeline2020.inputShouldShow = true;

            var psList2020 = new ArrayList<CVPipelineSettings>();
            psList2020.add(pipeline2020);
            camConf2020.pipelineSettings = psList2020;
        }

        var camConf2022 =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get("WPI2022");
        if (camConf2022 == null) {
            camConf2022 =
                    new CameraConfiguration("WPI2022", TestUtils.getTestMode2022ImagePath().toString());
            camConf2022.FOV = TestUtils.WPI2022Image.FOV;
            camConf2022.calibrations.add(TestUtils.get2019LifeCamCoeffs(true));

            var pipeline2022 = new ReflectivePipelineSettings();
            pipeline2022.pipelineNickname = "OuterPort";
            pipeline2022.targetModel = TargetModel.k2020HighGoalOuter;
            pipeline2022.inputShouldShow = true;
            //        camConf2020.calibrations.add(TestUtils.get2020LifeCamCoeffs(true));

            var psList2022 = new ArrayList<CVPipelineSettings>();
            psList2022.add(pipeline2022);
            camConf2022.pipelineSettings = psList2022;
        }

        CameraConfiguration camConf2023 =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get("WPI2023");
        if (camConf2023 == null) {
            camConf2023 =
                    new CameraConfiguration(
                            "WPI2023",
                            TestUtils.getResourcesFolderPath(true)
                                    .resolve("testimages")
                                    .resolve(TestUtils.WPI2023Apriltags.k383_60_Angle2.path)
                                    .toString());

            camConf2023.FOV = TestUtils.WPI2023Apriltags.FOV;
            camConf2023.calibrations.add(TestUtils.get2023LifeCamCoeffs(true));

            var pipeline2023 = new AprilTagPipelineSettings();
            var path_split = Path.of(camConf2023.path).getFileName().toString();
            pipeline2023.pipelineNickname = path_split.replace(".png", "");
            pipeline2023.targetModel = TargetModel.k6in_16h5;
            pipeline2023.inputShouldShow = true;
            pipeline2023.solvePNPEnabled = true;

            var psList2023 = new ArrayList<CVPipelineSettings>();
            psList2023.add(pipeline2023);
            camConf2023.pipelineSettings = psList2023;
        }

        // Colored shape testing
        var camConfShape =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get("Shape");

        // If we haven't saved shape settings, create a new one
        if (camConfShape == null) {
            camConfShape =
                    new CameraConfiguration(
                            "Shape",
                            TestUtils.getPowercellImagePath(TestUtils.PowercellTestImages.kPowercell_test_1, true)
                                    .toString());
            var settings = new ColoredShapePipelineSettings();
            settings.hsvHue = new IntegerCouple(0, 35);
            settings.hsvSaturation = new IntegerCouple(82, 255);
            settings.hsvValue = new IntegerCouple(62, 255);
            settings.contourShape = ContourShape.Triangle;
            settings.outputShowMultipleTargets = true;
            settings.circleAccuracy = 15;
            settings.inputShouldShow = true;
            camConfShape.addPipelineSetting(settings);
        }

        var collectedSources = new ArrayList<VisionSource>();

        var fvsShape = new FileVisionSource(camConfShape);
        var fvs2019 = new FileVisionSource(camConf2019);
        var fvs2020 = new FileVisionSource(camConf2020);
        var fvs2022 = new FileVisionSource(camConf2022);
        var fvs2023 = new FileVisionSource(camConf2023);

        collectedSources.add(fvs2023);
        collectedSources.add(fvs2022);
        collectedSources.add(fvsShape);
        collectedSources.add(fvs2020);
        collectedSources.add(fvs2019);

        ConfigManager.getInstance().unloadCameraConfigs();
        VisionModuleManager.getInstance().addSources(collectedSources).forEach(VisionModule::start);
        ConfigManager.getInstance().addCameraConfigurations(collectedSources);
    }

    public static void main(String[] args) {
        try {
            TestUtils.loadLibraries();
            logger.info("Native libraries loaded.");
        } catch (Exception e) {
            logger.error("Failed to load native libraries!", e);
        }

        try {
            LibCameraJNI.forceLoad();
        } catch (IOException e) {
            logger.error("Failed to load native libraries!", e);
        }

        try {
            if (!handleArgs(args)) {
                System.exit(0);
            }
        } catch (ParseException e) {
            logger.error("Failed to parse command-line options!", e);
        }

        CVMat.enablePrint(false);
        PipelineProfiler.enablePrint(false);

        var logLevel = printDebugLogs ? LogLevel.TRACE : LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
        logger.info("Logging initialized in debug mode.");

        logger.info(
                "Starting PhotonVision version "
                        + PhotonVersion.versionString
                        + " on "
                        + Platform.getPlatformName()
                        + (Platform.isRaspberryPi() ? (" (Pi " + PiVersion.getPiVersion() + ")") : ""));

        ConfigManager.getInstance().load(); // init config manager
        ConfigManager.getInstance().requestSave();

        // Force load the hardware manager
        HardwareManager.getInstance();

        NetworkManager.getInstance().reinitialize();

        NetworkTablesManager.getInstance()
                .setConfig(ConfigManager.getInstance().getConfig().getNetworkConfig());

        if (!isTestMode) {
            VisionSourceManager.getInstance()
                    .registerLoadedConfigs(
                            ConfigManager.getInstance().getConfig().getCameraConfigurations().values());

            VisionSourceManager.getInstance().registerTimedTask();
        } else {
            if (testModeFolder == null) {
                addTestModeSources();
            } else {
                addTestModeFromFolder();
            }
        }

        Server.main(DEFAULT_WEBPORT);
    }
}
