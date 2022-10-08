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

package org.photonvision.common.configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ColoredShapePipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;
import org.photonvision.vision.target.TargetModel;

public class ConfigTest {
    private static ConfigManager configMgr;
    private static final CameraConfiguration cameraConfig =
            new CameraConfiguration("TestCamera", "/dev/video420");
    private static ReflectivePipelineSettings REFLECTIVE_PIPELINE_SETTINGS;
    private static ColoredShapePipelineSettings COLORED_SHAPE_PIPELINE_SETTINGS;
    private static AprilTagPipelineSettings APRIL_TAG_PIPELINE_SETTINGS;

    @BeforeAll
    public static void init() {
        TestUtils.loadLibraries();
        configMgr = new ConfigManager(Path.of("testconfigdir"));
        configMgr.load();

        Logger.setLevel(LogGroup.General, LogLevel.TRACE);

        REFLECTIVE_PIPELINE_SETTINGS = new ReflectivePipelineSettings();
        COLORED_SHAPE_PIPELINE_SETTINGS = new ColoredShapePipelineSettings();
        APRIL_TAG_PIPELINE_SETTINGS = new AprilTagPipelineSettings();

        REFLECTIVE_PIPELINE_SETTINGS.pipelineNickname = "2019Tape";
        REFLECTIVE_PIPELINE_SETTINGS.targetModel = TargetModel.k2019DualTarget;

        COLORED_SHAPE_PIPELINE_SETTINGS.pipelineNickname = "2019Cargo";
        COLORED_SHAPE_PIPELINE_SETTINGS.pipelineIndex = 1;

        APRIL_TAG_PIPELINE_SETTINGS.pipelineNickname = "apriltag";
        APRIL_TAG_PIPELINE_SETTINGS.pipelineIndex = 2;

        cameraConfig.addPipelineSetting(REFLECTIVE_PIPELINE_SETTINGS);
        cameraConfig.addPipelineSetting(COLORED_SHAPE_PIPELINE_SETTINGS);
        cameraConfig.addPipelineSetting(APRIL_TAG_PIPELINE_SETTINGS);
    }

    @Test
    @Order(1)
    public void serializeConfig() {
        TestUtils.loadLibraries();

        Logger.setLevel(LogGroup.General, LogLevel.TRACE);
        configMgr.getConfig().addCameraConfig(cameraConfig);
        configMgr.saveToDisk();

        var camConfDir =
                new File(
                        Path.of(configMgr.configDirectoryFile.toString(), "cameras", "TestCamera")
                                .toAbsolutePath()
                                .toString());
        Assertions.assertTrue(camConfDir.exists(), "TestCamera config folder not found!");

        Assertions.assertTrue(
                Files.exists(Path.of(configMgr.configDirectoryFile.toString(), "networkSettings.json")),
                "networkSettings.json file not found!");
    }

    @Test
    @Order(2)
    public void deserializeConfig() {
        var reflectivePipelineSettings =
                configMgr.getConfig().getCameraConfigurations().get("TestCamera").pipelineSettings.get(0);
        var coloredShapePipelineSettings =
                configMgr.getConfig().getCameraConfigurations().get("TestCamera").pipelineSettings.get(1);
        var apriltagPipelineSettings =
                configMgr.getConfig().getCameraConfigurations().get("TestCamera").pipelineSettings.get(2);

        Assertions.assertEquals(REFLECTIVE_PIPELINE_SETTINGS, reflectivePipelineSettings);
        Assertions.assertEquals(COLORED_SHAPE_PIPELINE_SETTINGS, coloredShapePipelineSettings);
        Assertions.assertEquals(APRIL_TAG_PIPELINE_SETTINGS, apriltagPipelineSettings);

        Assertions.assertTrue(
                reflectivePipelineSettings instanceof ReflectivePipelineSettings,
                "Conig loaded pipeline settings for index 0 not of expected type ReflectivePipelineSettings!");
        Assertions.assertTrue(
                coloredShapePipelineSettings instanceof ColoredShapePipelineSettings,
                "Conig loaded pipeline settings for index 1 not of expected type ColoredShapePipelineSettings!");
        Assertions.assertTrue(
                apriltagPipelineSettings instanceof AprilTagPipelineSettings,
                "Conig loaded pipeline settings for index 2 not of expected type AprilTagPipelineSettings!");
    }

    @AfterAll
    public static void cleanup() throws IOException {
        try {
            Files.deleteIfExists(Paths.get("settings.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileUtils.cleanDirectory(configMgr.configDirectoryFile);
        configMgr.configDirectoryFile.delete();
    }

    @Test
    public void testJacksonHandlesOldVersions() throws IOException {
        var str =
                "{\"baseName\":\"aaaaaa\",\"uniqueName\":\"aaaaaa\",\"nickname\":\"aaaaaa\",\"FOV\":70.0,\"path\":\"dev/vid\",\"cameraType\":\"UsbCamera\",\"currentPipelineIndex\":0,\"camPitch\":{\"radians\":0.0},\"calibrations\":[], \"cameraLEDs\":[]}";
        var writer = new FileWriter("test.json");
        writer.write(str);
        writer.flush();
        writer.close();
        Assertions.assertDoesNotThrow(
                () -> JacksonUtils.deserialize(Path.of("test.json"), CameraConfiguration.class));

        new File("test.json").delete();
    }
}
