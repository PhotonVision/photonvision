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

package wpiutil_extras;

import static org.junit.jupiter.api.Assertions.fail;

import edu.wpi.first.hal.HAL;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.QueuedFileLogger;
import org.photonvision.jni.WpilibLoader;

public class FileLoggerTest {
    @BeforeAll
    public static void load_wpilib() throws UnsatisfiedLinkError, IOException {
        if (!WpilibLoader.loadLibraries()) {
            fail();
        }
        if (!PhotonTargetingJniLoader.load()) {
            fail();
        }

        HAL.initialize(1000, 0);
    }

    @AfterAll
    public static void teardown() {
        HAL.shutdown();
    }

    @Test
    public void smoketest() throws InterruptedException {
        var logger = new QueuedFileLogger("/var/log/kern.log");
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1000);

            for (var line : logger.getNewlines()) {
                System.out.println(" ->:" + line);
            }
        }

        logger.stop();
    }
}
