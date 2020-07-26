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

package org.photonvision.common.hardware.PWM;

import eu.xeli.jpigpio.PigpioException;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PiPWM extends PWMBase {

    private static final Logger logger = new Logger(PWMBase.class, LogGroup.General);
    private final int pin;

    public PiPWM(int pin, int value, int range) {
        this.pin = pin;
        try {
            pigpio.setPWMRange(this.pin, range);
            pigpio.setPWMFrequency(this.pin, value);
        } catch (PigpioException e) {
            logger.error("Could not set PWM settings on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        try {
            pigpio.setPWMRange(this.pin, range.get(0));
        } catch (PigpioException e) {
            logger.error("Could not set PWM range on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public List<Integer> getPwmRange() {
        try {
            return List.of(0, pigpio.getPWMRange(this.pin));
        } catch (PigpioException e) {
            logger.error("Could not get PWM range on port " + this.pin);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean shutdown() {
        try {
            pigpio.gpioTerminate();
        } catch (PigpioException e) {
            logger.error("Could not stop GPIO instance");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void dimLED(int dimPercentage) {
        // Check to see if dimPercentage is within the range
        if (dimPercentage < getPwmRange().get(0) || dimPercentage > getPwmRange().get(1)) return;
        try {
            pigpio.setPWMFrequency(this.pin, dimPercentage);
        } catch (PigpioException e) {
            logger.error("Could not dim PWM on port " + this.pin);
            e.printStackTrace();
        }
    }
}
