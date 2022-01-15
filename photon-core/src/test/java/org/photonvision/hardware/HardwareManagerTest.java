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

package org.photonvision.hardware;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.photonvision.common.hardware.GPIO.pi.PigpioException;
import org.photonvision.common.hardware.GPIO.pi.PigpioSocket;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class HardwareManagerTest {
    public static final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    @Test
    public void managementTest() throws InterruptedException {
        Assumptions.assumeTrue(Platform.isRaspberryPi());
        var socket = new PigpioSocket();
        try {
            socket.gpioWrite(18, false);
            socket.gpioWrite(13, false);
            Thread.sleep(500);
            for (int i = 0; i < 1000000; i++) {
                int duty = 1000000 - i;
                socket.hardwarePWM(18, 1000000, duty);
                Thread.sleep(0, 25);
            }
        } catch (PigpioException e) {
            logger.error("error", e);
        }
    }
}
