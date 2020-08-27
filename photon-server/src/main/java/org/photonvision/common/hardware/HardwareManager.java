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

package org.photonvision.common.hardware;

import java.io.IOException;
import java.util.HashMap;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;
import org.photonvision.common.hardware.metrics.MetricsBase;
import org.photonvision.common.hardware.metrics.MetricsPublisher;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class HardwareManager {
    HardwareConfig hardwareConfig;
    private final HashMap<Integer, GPIOBase> LEDs = new HashMap<>();
    private final ShellExec shellExec = new ShellExec(true, false);
    private final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    public static HardwareManager getInstance() {
        if (Singleton.INSTANCE == null) {
            Singleton.INSTANCE = new HardwareManager();
        }
        return Singleton.INSTANCE;
    }

    public void setConfig(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
        CustomGPIO.setConfig(hardwareConfig);
        MetricsBase.setConfig(hardwareConfig);

        hardwareConfig.ledPins.forEach(
                pin -> {
                    if (Platform.isRaspberryPi()) {
                        LEDs.put(
                                pin,
                                new PiGPIO(pin, hardwareConfig.ledPWMFrequency, hardwareConfig.ledPWMRange.get(1)));
                    } else {
                        LEDs.put(pin, new CustomGPIO(pin));
                    }
                });

        // Start hardware metrics thread
        if (Platform.isLinux()) MetricsPublisher.getInstance().startTask();
    }

    /** Example: HardwareManager.getInstance().getPWM(port).dimLEDs(int dimValue); */
    public GPIOBase getGPIO(int pin) {
        return LEDs.get(pin);
    }

    public void blinkLEDs(int pulseTimeMillis, int blinks) {
        LEDs.values().forEach(led -> led.blink(pulseTimeMillis, blinks));
    }

    public void setBrightnessPercentage(int percentage) {
        LEDs.values().forEach(led -> led.dimLED(percentage));
    }

    public void turnLEDsOn() {
        LEDs.values().forEach(GPIOBase::setHigh);
    }

    public void turnLEDsOff() {
        LEDs.values().forEach(GPIOBase::setLow);
    }

    public void toggleLEDs() {
        LEDs.values().forEach(GPIOBase::togglePin);
    }

    public void shutdown() {
        LEDs.values().forEach(GPIOBase::shutdown);
    }

    public GPIOBase redStatusLED() {
        try {
            return LEDs.get(hardwareConfig.statusRGBPins.get(0));
        } catch (ArrayIndexOutOfBoundsException e) {
            return LEDs.get(-1);
        }
    }

    public GPIOBase greenStatusLED() {
        try {
            return LEDs.get(hardwareConfig.statusRGBPins.get(1));
        } catch (ArrayIndexOutOfBoundsException e) {
            return LEDs.get(-1);
        }
    }

    public GPIOBase blueStatusLED() {
        try {
            return LEDs.get(hardwareConfig.statusRGBPins.get(2));
        } catch (ArrayIndexOutOfBoundsException e) {
            return LEDs.get(-1);
        }
    }

    public boolean restartDevice() {
        try {
            return shellExec.executeBashCommand(hardwareConfig.restartHardwareCommand) == 0;
        } catch (IOException e) {
            logger.error("Could not restart device!", e);
            return false;
        }
    }

    public HardwareConfig getConfig() {
        return hardwareConfig;
    }

    private static class Singleton {
        private static HardwareManager INSTANCE;
    }
}
