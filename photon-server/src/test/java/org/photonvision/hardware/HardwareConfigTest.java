/*
 * Copyright (C) 2020 Photon Vision.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.PWM.CustomPWM;
import org.photonvision.common.util.TestUtils;

public class HardwareConfigTest {

    @Test
    public void loadJson() {
        try {
            System.out.println("Loading Hardware configs...");
            var config =
                    new ObjectMapper().readValue(TestUtils.getHardwareConfigJson(), HardwareConfig.class);
            assertEquals(config.getDeviceName(), "PhotonVision");
            assertEquals(config.getDeviceLogoPath(), "photonvision.png");
            assertEquals(config.getSupportURL(), "https://support.photonvision.com");
            Assertions.assertArrayEquals(
                    config.getLedPins().stream().mapToInt(i -> i).toArray(), new int[] {1, 5});
            Assertions.assertArrayEquals(
                    config.getLedPWMRange().stream().mapToInt(i -> i).toArray(), new int[] {0, 100});

            CustomGPIO.setConfig(config);
            CustomPWM.setConfig(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
