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

package org.photonvision.common.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.photonvision.common.configuration.ConfigProvider;
import org.photonvision.common.logging.Logger;

public class LogFileManagementTest {
    @TempDir private Path tmpDir;

    @Test
    public void fileCleanupTest() {
        ConfigProvider cfgLoader = new ConfigProvider(tmpDir);
        // Ensure we instantiate the new log correctly

        Path testDir = cfgLoader.getLogsDir().resolve("test");

        assertDoesNotThrow(() -> Files.createDirectories(testDir));

        // Create a bunch of log files with dummy contents.
        for (int fileIdx = 0; fileIdx < Logger.MAX_LOGS_TO_KEEP + 5; fileIdx++) {
            String fname =
                    cfgLoader
                            .taToLogFname(
                                    LocalDateTime.ofEpochSecond(1500000000 + fileIdx * 60, 0, ZoneOffset.UTC));
            try {
                FileWriter testLogWriter = new FileWriter(testDir.resolve(fname).toString());
                testLogWriter.write("Test log contents created for testing purposes only");
                testLogWriter.close();
            } catch (IOException e) {
                fail("Could not create test files");
            }
        }

        // Confirm new log files were created
        assertTrue(
                Logger.MAX_LOGS_TO_KEEP + 5 <= countLogFiles(testDir.toString()), "Not enough log files discovered");

        // Run the log cleanup routine
        Logger.cleanLogs(testDir);

        // Confirm we deleted log files
        assertEquals(Logger.MAX_LOGS_TO_KEEP, countLogFiles(testDir.toString()), "Not enough log files deleted");

        // Clean uptest directory
        org.photonvision.common.util.file.FileUtils.deleteDirectory(testDir);
        try {
            Files.delete(testDir);
        } catch (IOException e) {
            // it's OK if this fails
        }
    }

    private int countLogFiles(String testDir) {
        return FileUtils.listFiles(
                        new File(testDir), new WildcardFileFilter("photonvision-*.log"), null)
                .size();
    }
}
