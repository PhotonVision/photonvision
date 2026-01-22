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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.diozero.internal.provider.builtin.DefaultDeviceFactory;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.VisionLED;
import org.photonvision.common.util.TestUtils;

public class HardwareTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    @Test
    public void testNativeGPIO() {
        try (NativeDeviceFactoryInterface deviceFactory = new DefaultDeviceFactory()) {
            Assumptions.assumeTrue(deviceFactory.getBoardInfo().isRecognised());

            try (VisionLED led = new VisionLED(deviceFactory, List.of(2, 13), false, 0, 100, 0, null)) {
                // Verify states can be set
                led.setState(true);
                assertEquals(1, deviceFactory.getGpioValue(2));
                assertEquals(1, deviceFactory.getGpioValue(13));
                led.setState(false);
                assertEquals(0, deviceFactory.getGpioValue(2));
                assertEquals(0, deviceFactory.getGpioValue(13));
            }
        }
    }

    @Nested
    class CustomGPIOTest {
        HardwareConfig hardwareConfig = null;
        NativeDeviceFactoryInterface deviceFactory = null;

        @BeforeEach
        void setup() throws IOException {
            System.out.println("Loading Hardware configs...");
            hardwareConfig =
                    new ObjectMapper().readValue(TestUtils.getHardwareConfigJson(), HardwareConfig.class);
            deviceFactory = HardwareManager.configureCustomGPIO(hardwareConfig);
        }

        @Test
        public void testCustomGPIO() throws IOException {
            try (VisionLED led = new VisionLED(deviceFactory, List.of(2, 13), false, 0, 100, 0, null)) {
                // Verify states can be set
                led.setState(true);
                assertEquals(1, deviceFactory.getGpioValue(2));
                assertEquals(1, deviceFactory.getGpioValue(13));
                led.setState(false);
                assertEquals(0, deviceFactory.getGpioValue(2));
                assertEquals(0, deviceFactory.getGpioValue(13));
            }
        }

        @Test
        public void testBlink() throws InterruptedException, IOException {
            try (VisionLED led = new VisionLED(deviceFactory, List.of(2, 13), false, 0, 100, 0, null)) {
                // Verify blinking toggles between states
                HashSet<Integer> seenValues = new HashSet<>();
                led.blink(125, 3);
                var startms = System.currentTimeMillis();
                while (System.currentTimeMillis() - startms < 1000) {
                    seenValues.add(deviceFactory.getGpioValue(2));
                }
                assertEquals(2, seenValues.size());
                assertTrue(seenValues.contains(0));
                assertTrue(seenValues.contains(1));

                seenValues.clear();

                // Verify that after blinking, toggling has stopped
                startms = System.currentTimeMillis();
                while (System.currentTimeMillis() - startms < 250) {
                    seenValues.add(deviceFactory.getGpioValue(2));
                }
                assertEquals(1, seenValues.size());
            }
        }
    }
}
