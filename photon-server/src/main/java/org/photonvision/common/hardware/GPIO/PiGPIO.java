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

import eu.xeli.jpigpio.JPigpio;
import eu.xeli.jpigpio.PigpioException;
import eu.xeli.jpigpio.PigpioSocket;
import eu.xeli.jpigpio.Pulse;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PiGPIO extends GPIOBase {
    private static final Logger logger = new Logger(PiGPIO.class, LogGroup.General);
    private final ArrayList<Pulse> pulses = new ArrayList<>();
    public final int pin;

    public static JPigpio getPigpioDaemon() {
        return Singleton.INSTANCE;
    }

    public PiGPIO(int address, int frequency, int range) {
        this.pin = address;
        try {
            getPigpioDaemon().setPWMFrequency(this.pin, frequency);
            getPigpioDaemon().setPWMRange(this.pin, range);
        } catch (PigpioException e) {
            logger.error("Could not set PWM settings on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void togglePin() {
        try {
            getPigpioDaemon().gpioWrite(this.pin, !getPigpioDaemon().gpioRead(this.pin));
        } catch (PigpioException e) {
            logger.error("Could not toggle on pin " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setLow() {
        try {
            getPigpioDaemon().gpioWrite(this.pin, false);
        } catch (PigpioException e) {
            logger.error("Could not set pin low on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setHigh() {
        try {
            getPigpioDaemon().gpioWrite(this.pin, true);
        } catch (PigpioException e) {
            logger.error("Could not set pin high on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setState(boolean state) {
        try {
            getPigpioDaemon().gpioWrite(this.pin, state);
        } catch (PigpioException e) {
            logger.error("Could not set pin state on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public boolean shutdown() {
        try {
            getPigpioDaemon().gpioTerminate();
        } catch (PigpioException e) {
            logger.error("Could not terminate GPIO instance");
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean getState() {
        try {
            return getPigpioDaemon().gpioRead(this.pin);
        } catch (PigpioException e) {
            logger.error("Could not read pin on port " + this.pin);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        try {
            getPigpioDaemon().setPWMRange(this.pin, range.get(0));
        } catch (PigpioException e) {
            logger.error("Could not set PWM range on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public List<Integer> getPwmRange() {
        try {
            return List.of(0, getPigpioDaemon().getPWMRange(this.pin));
        } catch (PigpioException e) {
            logger.error("Could not get PWM range on port " + this.pin);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void blink(int pulseTimeMillis, int blinks) {
        try {
            pulses.clear();
            for (int i = 0; i < blinks; i++) {
                pulses.add(new Pulse(1 << this.pin, 0, pulseTimeMillis * 100));
                pulses.add(new Pulse(0, 1 << this.pin, pulseTimeMillis * 100));
            }
            getPigpioDaemon().waveAddGeneric(this.pulses);
            getPigpioDaemon().waveSendOnce(getPigpioDaemon().waveCreate());
        } catch (PigpioException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dimLED(int dimPercentage) {
        try {
            getPigpioDaemon().setPWMDutycycle(this.pin, getPwmRange().get(1) * (dimPercentage / 100));
        } catch (PigpioException e) {
            logger.error("Could not dim PWM on port " + this.pin);
            e.printStackTrace();
        }
    }

    private static class Singleton {
        public static JPigpio INSTANCE;

        static {
            try {
                INSTANCE = new PigpioSocket("localhost", 8888);
            } catch (PigpioException e) {
                logger.error("Could not connect to pigpio daemon");
                e.printStackTrace();
            }
        }
    }
}
