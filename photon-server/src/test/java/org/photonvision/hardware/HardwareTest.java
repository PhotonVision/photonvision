package org.photonvision.hardware;

import static org.junit.jupiter.api.Assertions.*;


import com.pi4j.io.gpio.exception.UnsupportedPinModeException;
import org.junit.jupiter.api.Test;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;
import org.photonvision.common.hardware.PWM.CustomPWM;
import org.photonvision.common.hardware.PWM.PWMBase;
import org.photonvision.common.hardware.PWM.PiPWM;
import org.photonvision.common.hardware.metrics.CPU;
import org.photonvision.common.hardware.metrics.GPU;
import org.photonvision.common.hardware.metrics.RAM;
import org.photonvision.common.util.Platform;

public class HardwareTest {

    @Test
    public void testHardware() {
        if (!Platform.isRaspberryPi()) return;

        System.out.println("Testing on platform: " + Platform.CurrentPlatform);

        System.out.println("Printing CPU Info:");
        System.out.println("Memory: " + CPU.getMemory() + "MB");
        System.out.println("Temperature: " + CPU.getTemp() + "C");
        System.out.println("Utilization: : " + CPU.getUtilization() + "%");

        System.out.println("Printing GPU Info:");
        System.out.println("Memory: " + GPU.getMemory() + "MB");
        System.out.println("Temperature: " + GPU.getTemp() + "C");

        System.out.println("Printing RAM Info: ");
        System.out.println("Used RAM: : " + RAM.getUsedRam() + "MB");
    }

    @Test
    public void testGPIO() {
        GPIOBase gpio;
        if (Platform.isRaspberryPi()) {
            gpio = new PiGPIO(5);
        } else {
            gpio = new CustomGPIO(5);
            gpio.setStateCommand("gpio setState {p} {s}");
            gpio.setBlinkCommand("gpio blink {p} {delay} {duration}");
            gpio.setPulseCommand("gpio pulse {p} {blocking} {duration}");
            gpio.setShutdownCommand("gpio shutdown");
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
        
        var success = gpio.shutdown();
        assertTrue(success);
    }

    @Test
    public void testPWM() {
        PWMBase pwm;
        if (Platform.isRaspberryPi()) {
            try {
                pwm = new PiPWM(1);
            } catch (UnsupportedPinModeException e) {
                System.out.println("Invalid PWN port.");
                return;
            }
        } else {
            pwm = new CustomPWM(1);
            pwm.setPwmRateCommand("pwm setRate {p} {rate}");
            pwm.setPwmRangeCommand("pwm setRange {p} {range}");
        }
        pwm.setPwmRange(100);
        assertEquals(pwm.getPwmRange(), 100);

        pwm.setPwmRate(10);
        assertEquals(pwm.getPwmRate(), 10);

        var success = pwm.shutdown();
        assertTrue(success);
    }
}
