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

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.opencv.CVMat;

public class NetworkConfigTest {
    @TempDir private Path tmpDir;

    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
        CVMat.enablePrint(false);

        var logLevel = LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.Config, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
    }

    @Test
    public void testSerialization() throws IOException {
        Path path = tmpDir.resolve("netTest.json");
        JsonType<NetworkConfig> jsonb = Jsonb.instance().type(NetworkConfig.class);
        try (var outputStream = new FileOutputStream(path.toFile())) {
            jsonb.toJson(new NetworkConfig(), outputStream);
        }
        try (var inputStream = new FileInputStream(path.toFile())) {
            assertDoesNotThrow(() -> jsonb.fromJson(inputStream));
        }
        new File("netTest.json").delete();
    }

    @ParameterizedTest
    @CsvSource({"'network-team-number','9999'", "'network-ip-addr','127.0.0.1'"})
    public void testDeserializeTeamNumber(String testSource, String expectedNtServerAddress)
            throws IOException {
        var testDatabase = tmpDir.resolve("photon.sqlite");
        FileUtils.copyFile(
                TestUtils.getConfigDirectoriesPath(false)
                        .resolve(String.format("networkConfigTest/%s.sqlite", testSource))
                        .toFile(),
                testDatabase.toFile());
        var configMgr = new ConfigManager(tmpDir, new SqlConfigProvider(tmpDir));
        configMgr.load();
        assertEquals(expectedNtServerAddress, configMgr.getConfig().getNetworkConfig().ntServerAddress);
    }
}
