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

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.VisionLED;
import org.photonvision.common.hardware.metrics.MetricsManager;

public class HardwareTest {
    @Test
    public void testHardware() {
        LoadJNI.loadLibraries();
        MetricsManager mm = new MetricsManager();

        if (!Platform.isRaspberryPi()) return;

        System.out.println("Testing on platform: " + Platform.getPlatformName());

        System.out.println("Printing CPU Info:");
        System.out.println("Memory: " + mm.getRamMem() + "MB");
        System.out.println("Temperature: " + mm.getCpuTemp() + "C");
        System.out.println("Utilization: : " + mm.getCpuUtilization() + "%");

        System.out.println("Printing GPU Info:");
        System.out.println("Memory: " + mm.getGpuMem() + "MB");

        System.out.println("Printing RAM Info: ");
        System.out.println("Used RAM: : " + mm.getRamUtil() + "MB");
    }

    @Test
    public void testGPIO() {
        NativeDeviceFactoryInterface deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
        Assumptions.assumeTrue(deviceFactory.getBoardInfo().isRecognised());

        VisionLED led = new VisionLED(List.of(2, 13), false, 0, 0, null);

        // Verify states can be set
        led.setState(true);
        assertEquals(deviceFactory.getGpioValue(2), 1);
        assertEquals(deviceFactory.getGpioValue(13), 1);
        led.setState(false);
        assertEquals(deviceFactory.getGpioValue(2), 0);
        assertEquals(deviceFactory.getGpioValue(13), 0);
    }

    @Test
    public void testBlink() throws InterruptedException {
        NativeDeviceFactoryInterface deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
        Assumptions.assumeTrue(deviceFactory.getBoardInfo().isRecognised());

        VisionLED led = new VisionLED(List.of(2, 13), false, 0, 0, null);

        // Verify blinking toggles between states
        HashSet<Integer> seenValues = new HashSet<>();
        led.blink(125, 3);
        var startms = System.currentTimeMillis();
        while (System.currentTimeMillis() - startms < 500) {
            seenValues.add(deviceFactory.getGpioValue(2));
        }
        assertEquals(seenValues.size(), 2);
        assertTrue(seenValues.contains(0));
        assertTrue(seenValues.contains(1));

        seenValues.clear();

        // Verify that after blinking, toggling has stopped
        while (System.currentTimeMillis() - startms < 125) {
            seenValues.add(deviceFactory.getGpioValue(2));
        }
        assertEquals(seenValues.size(), 1);
    }
}
