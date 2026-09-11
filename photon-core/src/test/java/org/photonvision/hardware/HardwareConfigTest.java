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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import io.avaje.jsonb.Jsonb;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.gpio.CustomDeviceFactory;
import org.photonvision.common.util.TestUtils;

public class HardwareConfigTest {
    @Test
    public void loadJson() {
        System.out.println("Loading Hardware configs...");
        try (var stream = new FileInputStream(TestUtils.getHardwareConfigJson())) {
            var config = Jsonb.instance().type(HardwareConfig.class).fromJson(stream);
            assertEquals(config.deviceName, "PhotonVision");
            // Ensure defaults are not null
            assertArrayEquals(config.ledPins.stream().mapToInt(i -> i).toArray(), new int[] {2, 13});
            try (NativeDeviceFactoryInterface deviceFactory =
                    HardwareManager.configureCustomGPIO(config)) {
                assertTrue(deviceFactory instanceof CustomDeviceFactory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
