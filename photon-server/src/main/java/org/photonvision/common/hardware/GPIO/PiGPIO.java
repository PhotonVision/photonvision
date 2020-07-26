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

package org.photonvision.common.hardware.GPIO;

import eu.xeli.jpigpio.PigpioException;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PiGPIO extends GPIOBase {
    private static final Logger logger = new Logger(PiGPIO.class, LogGroup.General);
    private final int pin;

    public PiGPIO(int address) {
        this.pin = address;
    }

    @Override
    public void togglePin() {
        try {
            pigpio.gpioWrite(this.pin, !pigpio.gpioRead(this.pin));
        } catch (PigpioException e) {
            logger.error("Could not toggle on pin " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setLow() {
        try {
            pigpio.gpioWrite(this.pin, false);
        } catch (PigpioException e) {
            logger.error("Could not set pin low on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setHigh() {
        try {
            pigpio.gpioWrite(this.pin, true);
        } catch (PigpioException e) {
            logger.error("Could not set pin high on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setState(boolean state) {
        try {
            pigpio.gpioWrite(this.pin, state);
        } catch (PigpioException e) {
            logger.error("Could not set pin state on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void blink(long delay, long duration) {
        try {
            pigpio.gpioTrigger(this.pin, duration, true);
        } catch (PigpioException e) {
            logger.error("Could not blink pin on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public boolean shutdown() {
        try {
            pigpio.gpioTerminate();
        } catch (PigpioException e) {
            logger.error("Could not terminate GPIO instance");
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean getState() {
        try {
            return pigpio.gpioRead(this.pin);
        } catch (PigpioException e) {
            logger.error("Could not read pin on port " + this.pin);
            e.printStackTrace();
            return false;
        }
    }
}
