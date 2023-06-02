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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.*;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ColoredShapePipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class SQLConfigTest {
    @BeforeAll
    public static void init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void testLoad() {
        var cfgLoader = new SqlConfigProvider(Path.of("jdbc_test"));

        cfgLoader.load();

        var testcamcfg =
                new CameraConfiguration(
                        "basename",
                        "a_unique_name",
                        "a_nick_name",
                        69,
                        "a/path/idk",
                        CameraType.UsbCamera,
                        List.of(),
                        0);
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
