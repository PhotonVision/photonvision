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

public class HardwareManager {
    HardwareConfig hardwareConfig;
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

    private static class Singleton {
        private static final HardwareManager INSTANCE = new HardwareManager();
    }
}
