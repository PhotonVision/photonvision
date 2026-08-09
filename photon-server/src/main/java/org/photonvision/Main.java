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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.opencv.core.Size;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.OsImageData;
import org.photonvision.common.hardware.PiVersion;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.SystemMonitor;
import org.photonvision.common.logging.KernelLogLogger;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.logging.PvCSCoreLogger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.server.Server;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonMatOfDouble;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.PipelineProfiler;
import org.photonvision.vision.processes.VisionSourceManager;
import org.photonvision.vision.target.TargetModel;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.geometry.Rotation2d;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "java -jar photonvision.jar",
        mixinStandardHelpOptions = true,
        version = PhotonVersion.versionString)
public class Main implements Callable<Integer> {
    public static final int DEFAULT_WEBPORT = 5800;

    private static final Logger logger = new Logger(Main.class, LogGroup.General);

    @Option(
            names = {"-d", "--debug"},
            description = "Enable debug logging prints")
    private boolean debugMode;

    @Option(
            names = {"-t", "--test-mode"},
            description = "Run in test mode with 2019 and 2020 WPI field images in place of cameras")
    private boolean testMode;

    @Option(
            names = {"-n", "--disable-networking"},
            description = "Disables control device network settings")
    private boolean disableNetworking;

    @Option(
            names = {"-c", "--clear-config"},
            description = "Clears PhotonVision pipeline and networking settings. Preserves log files")
    private boolean clearConfig;

    @Option(
            names = {"-s", "--smoketest"},
            description =
                    "Exit Photon after loading native libraries and camera configs, but before starting up camera runners")
    private boolean smoketest;

    @Option(
            names = {"-p", "--platform"},
            description = "Specify platform override, based on Platform enum")
    private Optional<Platform> platform;

    private static void addTestModeSources() {
        ConfigManager.getInstance().load();

        CameraConfiguration camConf2026 =
                ConfigManager.getInstance().getConfig().getCameraConfigurations().get("WPI2026");
        if (camConf2026 == null) {
            camConf2026 =
                    new CameraConfiguration(
                            PVCameraInfo.fromFileInfo(
                                    TestUtils.getResourcesFolderPath(true)
                                            .resolve("testimages")
                                            .resolve(TestUtils.WPI2026Images.kBlueOutpostFuelSpread.path)
                                            .toString(),
                                    "WPI2026"));

            camConf2026.FOV = TestUtils.WPI2026Images.FOV.getDegrees();

            // stolen from SimCameraProperties
            int resWidth = (int) TestUtils.WPI2026Images.resolution.width;
            int resHeight = (int) TestUtils.WPI2026Images.resolution.height;
            double cx = resWidth / 2.0 - 0.5;
            double cy = resHeight / 2.0 - 0.5;

            double resDiag = Math.hypot(resWidth, resHeight);
            double diagRatio = Math.tan(TestUtils.WPI2026Images.FOV.getRadians() / 2);
            var fovWidth = new Rotation2d(Math.atan(diagRatio * (resWidth / resDiag)) * 2);
            var fovHeight = new Rotation2d(Math.atan(diagRatio * (resHeight / resDiag)) * 2);

            double fx = cx / Math.tan(fovWidth.getRadians() / 2.0);
            double fy = cy / Math.tan(fovHeight.getRadians() / 2.0);

            JsonMatOfDouble testCameraMatrix =
                    new JsonMatOfDouble(3, 3, new double[] {fx, 0, cx, 0, fy, cy, 0, 0, 1});
            JsonMatOfDouble testDistortion = new JsonMatOfDouble(1, 5, new double[] {0, 0, 0, 0, 0});

            camConf2026.calibrations.add(
                    new CameraCalibrationCoefficients(
                            new Size(4000, 1868),
                            testCameraMatrix,
                            testDistortion,
                            new double[0],
                            List.of(),
                            new Size(),
                            1,
                            CameraLensModel.LENSMODEL_OPENCV));

            logger.info("Added test camera calibration for WPI2026 " + camConf2026.calibrations);

            var pipeline2026 = new AprilTagPipelineSettings();
            var path_split = Path.of(camConf2026.matchedCameraInfo.path()).getFileName().toString();
            pipeline2026.pipelineNickname = path_split.replace(".jpg", "");
            pipeline2026.targetModel = TargetModel.kAprilTag6p5in_36h11;
            pipeline2026.tagFamily = AprilTagFamily.kTag36h11;
            pipeline2026.inputShouldShow = true;
            pipeline2026.solvePNPEnabled = true;
            pipeline2026.streamingFrameDivisor = FrameDivisor.QUARTER;
            pipeline2026.decimate = 4;

            var psList2026 = new ArrayList<CVPipelineSettings>();
            psList2026.add(pipeline2026);
            camConf2026.pipelineSettings = psList2026;
        }

        var cameraConfigs = List.of(camConf2026);

        ConfigManager.getInstance().unloadCameraConfigs();
        cameraConfigs.stream().forEach(ConfigManager.getInstance()::addCameraConfiguration);
        VisionSourceManager.getInstance().registerLoadedConfigs(cameraConfigs);
    }

    private void tryLoadJNI(JNITypes type) {
        try {
            LoadJNI.forceLoad(type);
            logger.info("Loaded " + type.name() + "-JNI");
        } catch (IOException e) {
            logger.error("Failed to load " + type.name() + "-JNI!", e);
            if (smoketest) {
                System.exit(1);
            }
        }
    }

    @Override
    public Integer call() {
        var logLevel = debugMode ? LogLevel.TRACE : LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.Config, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
        logger.info("Logging initialized in debug mode.");

        System.setProperty("jsonb.disableAdapterSpi", "true");

        logger.info(
                "Starting PhotonVision version "
                        + PhotonVersion.versionString
                        + " on platform "
                        + Platform.getPlatformName()
                        + (Platform.isRaspberryPi() ? (" (Pi " + PiVersion.getPiVersion() + ")") : ""));

        if (OsImageData.IMAGE_METADATA.isPresent()) {
            logger.info("PhotonVision image data: " + OsImageData.IMAGE_METADATA.get());
        } else {
            logger.info("PhotonVision image version: unknown");
        }

        if (debugMode) {
            logger.info("Enabled debug logging");
        }

        if (testMode) {
            logger.info("Running in test mode - Cameras will not be used");
        }

        if (disableNetworking) {
            NetworkManager.getInstance().networkingIsDisabled = true;
        }

        if (clearConfig) {
            ConfigManager.getInstance().clearConfig();
        }

        if (platform.isPresent()) {
            Platform.overridePlatform(platform.get());
            logger.info("Overrode platform to: " + platform);
        }

        // We don't want to trigger an exit in test mode or smoke test. This is
        // specifically for MacOS.
        if (!(Platform.isSupported() || smoketest || testMode)) {
            logger.error("This platform is unsupported!");
            return 1;
        }

        try {
            boolean success = LoadJNI.loadLibraries();

            if (!success) {
                logger.error("Failed to load native libraries! Giving up :(");
                return 1;
            }
        } catch (Exception e) {
            logger.error("Failed to load native libraries!", e);
            return 1;
        }
        logger.info("WPILib and photon-targeting JNI libraries loaded.");

        if (!HAL.initialize(500, 0)) {
            logger.error("Failed to initialize the HAL! Giving up :(");
            return 1;
        }

        if (Platform.isRaspberryPi()) {
            tryLoadJNI(JNITypes.LIBCAMERA);
        }

        if (Platform.isRK3588()) {
            tryLoadJNI(JNITypes.RKNN_DETECTOR);
        } else {
            logger.warn("Platform does not support RKNN based machine learning!");
        }

        if (Platform.isQCS6490()) {
            tryLoadJNI(JNITypes.RUBIK_DETECTOR);
        } else {
            logger.warn("Platform does not support Rubik based machine learning!");
        }

        if (Platform.isWindows() || Platform.isLinux()) {
            tryLoadJNI(JNITypes.MRCAL);
        }

        CVMat.enablePrint(false);
        PipelineProfiler.enablePrint(false);

        // Add Linux kernel log->Photon logger
        KernelLogLogger.getInstance();

        // Add CSCore->Photon logger
        PvCSCoreLogger.getInstance();

        logger.debug("Loading ConfigManager...");
        ConfigManager.getInstance().load(); // init config manager
        ConfigManager.getInstance().requestSave();

        logger.info("Loading ML models...");
        var modelManager = NeuralNetworkModelManager.getInstance();
        modelManager.extractModels();
        modelManager.discoverModels();

        logger.debug("Loading NetworkManager...");
        NetworkManager.getInstance().reinitialize();

        logger.debug("Loading NetworkTablesManager...");
        NetworkTablesManager.getInstance()
                .setConfig(ConfigManager.getInstance().getConfig().getNetworkConfig());
        NetworkTablesManager.getInstance().registerTimedTasks();

        logger.debug("Loading HardwareManager...");
        // Force load the hardware manager
        HardwareManager.getInstance();

        if (smoketest) {
            logger.info("PhotonVision base functionality loaded -- smoketest complete");
            return 0;
        }

        logger.debug("Loading SystemMonitor...");
        SystemMonitor.getInstance().logSystemInformation();
        SystemMonitor.getInstance().startMonitor(500, 1000);

        // todo - should test mode just add test mode sources, but still allow local usb
        // cameras to be added?
        if (!testMode) {
            logger.debug("Loading VisionSourceManager...");
            VisionSourceManager.getInstance()
                    .registerLoadedConfigs(
                            ConfigManager.getInstance().getConfig().getCameraConfigurations().values());
        } else {
            addTestModeSources();
        }

        VisionSourceManager.getInstance().registerTimedTasks();

        logger.info("Starting server...");
        HardwareManager.getInstance().setError(Optional.empty());
        Server.initialize(DEFAULT_WEBPORT);
        return -1; // Don't exit, the server is running
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        if (exitCode >= 0) {
            System.exit(exitCode);
        }
    }
}
