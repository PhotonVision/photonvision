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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.pi.PigpioPin;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.MetricsManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.jni.PhotonTargetingJniLoader;

public class HardwareTest {
    @Test
    public void testHardware() {
        try {
            TestUtils.loadLibraries();
            PhotonTargetingJniLoader.load();
        } catch (UnsatisfiedLinkError | IOException e) {
            e.printStackTrace();
        }
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
        GPIOBase gpio;
        if (Platform.isRaspberryPi()) {
            gpio = new PigpioPin(18);
        } else {
            gpio = new CustomGPIO(18);
        }

        gpio.setOn(); // HIGH
        assertTrue(gpio.getState());

        gpio.setOff(); // LOW
        assertFalse(gpio.getState());

        gpio.togglePin(); // HIGH
        assertTrue(gpio.getState());

        gpio.togglePin(); // LOW
        assertFalse(gpio.getState());

        gpio.setState(true); // HIGH
        assertTrue(gpio.getState());

        gpio.setState(false); // LOW
        assertFalse(gpio.getState());

        var success = gpio.shutdown();
        assertTrue(success);
    }

    @Test
    public void testBlink() {
        if (!Platform.isRaspberryPi()) return;
        GPIOBase pwm = new PigpioPin(18);
        pwm.blink(125, 3);
        var startms = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - startms > 4500) break;
        }
    }
}
