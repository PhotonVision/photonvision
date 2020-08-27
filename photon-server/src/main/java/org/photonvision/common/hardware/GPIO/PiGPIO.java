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
    private final int port;

    public static JPigpio getPigpioDaemon() {
        return Singleton.INSTANCE;
    }

    public PiGPIO(int address, int frequency, int range) {
        this.port = address;
        try {
            getPigpioDaemon().setPWMFrequency(this.port, frequency);
            getPigpioDaemon().setPWMRange(this.port, range);
        } catch (PigpioException e) {
            logger.error("Could not set PWM settings on port " + this.port);
            e.printStackTrace();
        }
    }

    @Override
    public void togglePin() {
        if (this.port != -1) {
            try {
                getPigpioDaemon().gpioWrite(this.port, !getPigpioDaemon().gpioRead(this.port));
            } catch (PigpioException e) {
                logger.error("Could not toggle on pin " + this.port);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setLow() {
        if (this.port != -1) {
            try {
                getPigpioDaemon().gpioWrite(this.port, false);
            } catch (PigpioException e) {
                logger.error("Could not set pin low on port " + this.port);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setHigh() {
        if (this.port != -1) {
            try {
                getPigpioDaemon().gpioWrite(this.port, true);
            } catch (PigpioException e) {
                logger.error("Could not set pin high on port " + this.port);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setState(boolean state) {
        if (this.port != -1) {
            try {
                getPigpioDaemon().gpioWrite(this.port, state);
            } catch (PigpioException e) {
                logger.error("Could not set pin state on port " + this.port);
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean shutdown() {
        if (this.port != -1) {
            try {
                getPigpioDaemon().gpioTerminate();
            } catch (PigpioException e) {
                logger.error("Could not terminate GPIO instance");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean getState() {
        if (this.port != -1) {
            try {
                return getPigpioDaemon().gpioRead(this.port);
            } catch (PigpioException e) {
                logger.error("Could not read pin on port " + this.port);
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        if (this.port != -1) {
            try {
                getPigpioDaemon().setPWMRange(this.port, range.get(0));
            } catch (PigpioException e) {
                logger.error("Could not set PWM range on port " + this.port);
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Integer> getPwmRange() {
        if (this.port != -1) {
            try {
                return List.of(0, getPigpioDaemon().getPWMRange(this.port));
            } catch (PigpioException e) {
                logger.error("Could not get PWM range on port " + this.port);
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    @Override
    public void blink(int pulseTimeMillis, int blinks) {
        if (this.port != -1) {
            try {
                pulses.clear();
                for (int i = 0; i < blinks; i++) {
                    pulses.add(new Pulse(1 << this.port, 0, pulseTimeMillis * 100));
                    pulses.add(new Pulse(0, 1 << this.port, pulseTimeMillis * 100));
                }
                getPigpioDaemon().waveAddGeneric(this.pulses);
                getPigpioDaemon().waveSendOnce(getPigpioDaemon().waveCreate());
            } catch (PigpioException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void dimLED(int dimPercentage) {
        if (this.port != -1) {
            try {
                getPigpioDaemon().setPWMDutycycle(this.port, getPwmRange().get(1) * (dimPercentage / 100));
            } catch (PigpioException e) {
                logger.error("Could not dim PWM on port " + this.port);
                e.printStackTrace();
            }
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
