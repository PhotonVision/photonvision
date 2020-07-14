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

import static org.junit.jupiter.api.Assertions.*;

import com.pi4j.io.gpio.exception.UnsupportedPinModeException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;
import org.photonvision.common.hardware.PWM.CustomPWM;
import org.photonvision.common.hardware.PWM.PWMBase;
import org.photonvision.common.hardware.PWM.PiPWM;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.CPU;
import org.photonvision.common.hardware.metrics.GPU;
import org.photonvision.common.hardware.metrics.RAM;

public class HardwareTest {

    @Test
    public void testHardware() {
        CPU cpu = CPU.getInstance();
        RAM ram = RAM.getInstance();
        GPU gpu = GPU.getInstance();

        if (!Platform.isRaspberryPi()) return;

        System.out.println("Testing on platform: " + Platform.CurrentPlatform);

        System.out.println("Printing CPU Info:");
        System.out.println("Memory: " + cpu.getMemory() + "MB");
        System.out.println("Temperature: " + cpu.getTemp() + "C");
        System.out.println("Utilization: : " + cpu.getUtilization() + "%");

        System.out.println("Printing GPU Info:");
        System.out.println("Memory: " + gpu.getMemory() + "MB");
        System.out.println("Temperature: " + gpu.getTemp() + "C");

        System.out.println("Printing RAM Info: ");
        System.out.println("Used RAM: : " + ram.getUsedRam() + "MB");
    }

    @Test
    public void testGPIO() {
        GPIOBase gpio;
        if (Platform.isRaspberryPi()) {
            gpio = new PiGPIO(5);
        } else {
            gpio = new CustomGPIO(5);
        }

        gpio.setHigh(); // HIGH
        assertTrue(gpio.getState());

        gpio.setLow(); // LOW
        assertFalse(gpio.getState());

        gpio.togglePin(); // HIGH
        assertTrue(gpio.getState());

        gpio.togglePin(); // LOW
        assertFalse(gpio.getState());

        gpio.setState(true); // HIGH
        assertTrue(gpio.getState());

        gpio.setState(false); // LOW
        assertFalse(gpio.getState());

        gpio.blink(10, 100);

        var success = gpio.shutdown();
        assertTrue(success);
    }

    @Test
    public void testPWM() {
        PWMBase pwm;
        if (Platform.isRaspberryPi()) {
            try {
                pwm = new PiPWM(1, 0, 100);
            } catch (UnsupportedPinModeException e) {
                System.out.println("Invalid PWN port.");
                return;
            }
        } else {
            pwm = new CustomPWM(1);
        }
        pwm.setPwmRange(List.of(0, 100));
        assertEquals(pwm.getPwmRange().get(0), 0);
        assertEquals(pwm.getPwmRange().get(1), 100);

        pwm.dimLED(40);
        pwm.shutdown();
        var success = pwm.shutdown();
        assertTrue(success);
    }
}
