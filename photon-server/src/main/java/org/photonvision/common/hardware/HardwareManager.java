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

import eu.xeli.jpigpio.PigpioException;
import eu.xeli.jpigpio.PigpioSocket;
import eu.xeli.jpigpio.Utils;
import java.util.HashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;
import org.photonvision.common.hardware.PWM.CustomPWM;
import org.photonvision.common.hardware.PWM.PWMBase;
import org.photonvision.common.hardware.PWM.PiPWM;
import org.photonvision.common.hardware.metrics.MetricsBase;
import org.photonvision.common.hardware.metrics.MetricsPublisher;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class HardwareManager {
    HardwareConfig hardwareConfig;
    private static final Logger logger = new Logger(HardwareManager.class, LogGroup.General);
    private static final HashMap<Integer, Pair<? extends GPIOBase, ? extends PWMBase>> LEDs =
            new HashMap<>();

    public static HardwareManager getInstance() {
        return Singleton.INSTANCE;
    }

    public void setConfig(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
        CustomPWM.setConfig(hardwareConfig);
        CustomGPIO.setConfig(hardwareConfig);
        MetricsBase.setConfig(hardwareConfig);

        // Start pigpio if on pi
        if (Platform.isRaspberryPi()) {
            try {
                var pigpio = new PigpioSocket("localhost", 8888);
                GPIOBase.pigpio = pigpio;
                PWMBase.pigpio = pigpio;
                pigpio.gpioInitialize();
                Utils.addShutdown(pigpio);
            } catch (PigpioException e) {
                logger.error("Could not start pigpio");
                e.printStackTrace();
            }
        }

        hardwareConfig.ledPins.forEach(
                pin -> {
                    if (Platform.isRaspberryPi()) {
                        LEDs.put(
                                pin,
                                Pair.of(new PiGPIO(pin), new PiPWM(pin, 0, hardwareConfig.ledPWMRange.get(1))));
                    } else {
                        var pwm = new CustomPWM(pin);
                        pwm.setPwmRange(hardwareConfig.ledPWMRange);
                        LEDs.put(pin, Pair.of(new CustomGPIO(pin), pwm));
                    }
                });

        // Start hardware metrics thread
        MetricsPublisher.getInstance().startThread();
    }
    /** Example: HardwareManager.getInstance().getPWM(port).dimLEDs(int dimValue); */
    public PWMBase getPWM(int pin) {
        return LEDs.get(pin).getRight();
    }

    public GPIOBase getGPIO(int pin) {
        return LEDs.get(pin).getLeft();
    }

    public void blinkLEDs(int pulseTimeMillis, int blinks) {
        LEDs.values().forEach(led -> led.getRight().blink(pulseTimeMillis, blinks));
    }

    public void setBrightnessPercentage(int percentage) {
        LEDs.values().forEach(led -> led.getRight().dimLED(percentage));
    }

    public void turnLEDsOn() {
        LEDs.values().forEach(led -> led.getLeft().setHigh());
    }

    public void turnLEDsOff() {
        LEDs.values().forEach(led -> led.getLeft().setLow());
    }

    public void toggleLEDs() {
        LEDs.values().forEach(led -> led.getLeft().togglePin());
    }

    public void shutdown() {
        LEDs.values()
                .forEach(
                        led -> {
                            led.getLeft().shutdown();
                            led.getRight().shutdown();
                        });
    }

    private static class Singleton {
        private static final HardwareManager INSTANCE = new HardwareManager();
    }
}
