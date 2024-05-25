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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ColoredShapePipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class SQLConfigTest {
    private static Path tmpDir;

    @BeforeAll
    public static void init() {
        TestUtils.loadLibraries();
        try {
            tmpDir = Files.createTempDirectory("SQLConfigTest");
        } catch (IOException e) {
            System.out.println("Couldn't create temporary directory, using current directory");
            tmpDir = Path.of("jdbc_test", "temp");
        }
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        Files.walk(tmpDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
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

        var testcamcfg =
                new CameraConfiguration(
                        "basename",
                        "a_unique_name",
                        "a_nick_name",
                        69,
                        "a/path/idk",
                        CameraType.UsbCamera,
                        QuirkyCamera.getQuirkyCamera(-1, -1),
                        List.of(),
                        0,
                        -1,
                        -1);
        testcamcfg.pipelineSettings =
                List.of(
                        new ReflectivePipelineSettings(),
                        new AprilTagPipelineSettings(),
                        new ColoredShapePipelineSettings());

        cfgLoader.getConfig().addCameraConfig(testcamcfg);
        cfgLoader.getConfig().getNetworkConfig().ntServerAddress = "5940";
        cfgLoader.saveToDisk();

        cfgLoader.load();
        System.out.println(cfgLoader.getConfig());

        assertEquals(cfgLoader.getConfig().getNetworkConfig().ntServerAddress, "5940");
    }
}
