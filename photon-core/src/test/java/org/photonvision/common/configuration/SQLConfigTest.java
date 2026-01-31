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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.wpi.first.cscore.UsbCameraInfo;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.ColoredShapePipelineSettings;
import org.photonvision.vision.pipeline.ObjectDetectionPipelineSettings;
import org.photonvision.vision.pipeline.PipelineType;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class SQLConfigTest {
    @TempDir private static Path tmpDir;

    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    @Test
    @Order(1)
    public void testMigration() {
        SqlConfigProvider cfgLoader = new SqlConfigProvider(tmpDir);
        cfgLoader.load();

        assertEquals(
                DatabaseSchema.migrations.length,
                cfgLoader.getUserVersion(),
                "Database isn't at the correct version");
    }

    @Test
    @Order(2)
    public void testLoad() {
        var cfgLoader = new SqlConfigProvider(tmpDir);

        cfgLoader.load();

        var testCamCfg =
                new CameraConfiguration(
                        PVCameraInfo.fromUsbCameraInfo(
                                new UsbCameraInfo(0, "/dev/videoN", "some_name", new String[0], -1, 01)));

        testCamCfg.pipelineSettings =
                List.of(
                        new ReflectivePipelineSettings(),
                        new AprilTagPipelineSettings(),
                        new ColoredShapePipelineSettings());

        cfgLoader.getConfig().addCameraConfig(testCamCfg);
        cfgLoader.getConfig().getNetworkConfig().ntServerAddress = "5940";
        cfgLoader.saveToDisk();

        cfgLoader.load();
        System.out.println(cfgLoader.getConfig());

        assertEquals(cfgLoader.getConfig().getNetworkConfig().ntServerAddress, "5940");
    }

    @Test
    public void testLoad2024_3_1() {
        var cfgLoader =
                new SqlConfigProvider(
                        TestUtils.getConfigDirectoriesPath(false)
                                .resolve("photonvision_config_from_v2024.3.1"));

        assertDoesNotThrow(cfgLoader::load);

        System.out.println(cfgLoader.getConfig());
        for (var c : CameraQuirk.values()) {
            assertDoesNotThrow(
                    () ->
                            cfgLoader
                                    .config
                                    .getCameraConfigurations()
                                    .get("Microsoft_LifeCam_HD-3000")
                                    .cameraQuirks
                                    .hasQuirk(c));
        }
    }

    void common2025p3p1Assertions(PhotonConfiguration config) {
        // Make sure we got 8 cameras
        assertEquals(8, config.getCameraConfigurations().size());

        // Make sure exactly 2 have object detection pipelines
        long count =
                config.getCameraConfigurations().values().stream()
                        .filter(
                                c ->
                                        c.pipelineSettings.stream()
                                                .anyMatch(s -> s instanceof ObjectDetectionPipelineSettings))
                        .count();
        assertEquals(2, count);
    }

    @Test
    public void testLoadNewNNMM() throws JsonProcessingException {
        var folder = TestUtils.getConfigDirectoriesPath(false).resolve("2025.3.1-old-nnmm");
        var cfgManager = new ConfigManager(folder, new SqlConfigProvider(folder));

        // Replace global configmanager
        ConfigManager.INSTANCE = cfgManager;

        assertDoesNotThrow(cfgManager::load);

        System.out.println(cfgManager.getConfig());
        common2025p3p1Assertions(cfgManager.getConfig());

        // And we now see two models
        NeuralNetworkModelManager.getInstance();
        // force us to allow RKNN
        NeuralNetworkModelManager.getInstance().supportedBackends.add(Family.RKNN);
        NeuralNetworkModelManager.getInstance().discoverModels();
        assertEquals(5, NeuralNetworkModelManager.getInstance().models.get(Family.RKNN).size());

        ConfigManager.getInstance().saveToDisk();

        // Now that we have the config saved, load it again
        var reloadedProvider = new SqlConfigProvider(folder);
        reloadedProvider.load();
        common2025p3p1Assertions(reloadedProvider.getConfig());

        // And make sure NNPM has all 5 models
        assertEquals(5, reloadedProvider.getConfig().neuralNetworkPropertyManager().getModels().length);

        ConfigManager.INSTANCE = null;
    }

    @Test
    public void testMaxDetectionsMigration() {
        var folder = TestUtils.getConfigDirectoriesPath(false).resolve("2025.3.1-old-nnmm");
        var cfgManager = new ConfigManager(folder, new SqlConfigProvider(folder));

        // Replace global configmanager
        ConfigManager.INSTANCE = cfgManager;

        assertDoesNotThrow(cfgManager::load);

        Collection<CameraConfiguration> cameraConfigs =
                cfgManager.getConfig().getCameraConfigurations().values();

        for (CameraConfiguration cc : cameraConfigs) {
            for (CVPipelineSettings ps : cc.pipelineSettings) {
                if (ps instanceof AdvancedPipelineSettings adps) {
                    AdvancedPipelineSettings finalPs = adps;
                    if (finalPs.pipelineType.equals(PipelineType.AprilTag)
                            || finalPs.pipelineType.equals(PipelineType.Aruco)) {
                        assertEquals(127, finalPs.outputMaximumTargets);
                    } else if (finalPs.pipelineNickname.equals("TEST MIGRATION")) {
                        assertEquals(1, finalPs.outputMaximumTargets);
                    } else {
                        assertEquals(20, finalPs.outputMaximumTargets);
                    }
                } else {
                    System.out.println("Skipping pipeline settings type: " + ps.getClass().getSimpleName());
                }
            }
        }

        ConfigManager.INSTANCE = null;
    }
}
