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

import static eu.xeli.jpigpio.PigpioException.*;

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

    private int activeWaveId = -1;

    public static JPigpio getPigpioDaemon() {
        return Singleton.INSTANCE;
    }

    public PiGPIO(int address) {
        this(address, 8000, 255);
    }

    public PiGPIO(int address, int frequency, int range) {
        port = address;
        if (port != -1) {
            try {
                //            var pigpioRange = (int) (range / 255.0) * 40000; // TODO: is this conversion
                // correct/necessary?
                getPigpioDaemon().setPWMFrequency(port, frequency);
                getPigpioDaemon().setPWMRange(port, range);
            } catch (PigpioException e) {
                logger.error("Could not set PWM settings on port " + port, e);
            }
        }
    }

    private void cancelWave() throws PigpioException {
        if (activeWaveId != -1) {
            logger.debug("Cancelling wave with id " + activeWaveId);
            getPigpioDaemon().waveDelete(activeWaveId);
            getPigpioDaemon().waveTxStop();
            activeWaveId = -1;
        }
    }

    @Override
    public int getPinNumber() {
        return port;
    }

    @Override
    public void setStateImpl(boolean state) {
        try {
            cancelWave();
            getPigpioDaemon().gpioWrite(port, state);
        } catch (PigpioException e) {
            logger.error("Could not set pin state on port " + port, e);
        }
    }

    @Override
    public boolean shutdown() {
        try {
            getPigpioDaemon().gpioTerminate();
        } catch (PigpioException e) {
            logger.error("Could not terminate GPIO instance", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean getStateImpl() {
        try {
            return getPigpioDaemon().gpioRead(port);
        } catch (PigpioException e) {
            logger.error("Could not read pin on port " + port, e);
            return false;
        }
    }

    @Override
    public void setPwmRangeImpl(List<Integer> range) {
        try {
            cancelWave();
            getPigpioDaemon().setPWMRange(port, range.get(0));
        } catch (PigpioException e) {
            logger.error("Could not set PWM range on port " + port, e);
        }
    }

    @Override
    public List<Integer> getPwmRangeImpl() {
        try {
            return List.of(0, getPigpioDaemon().getPWMRange(port));
        } catch (PigpioException e) {
            logger.error("Could not get PWM range on port " + port, e);
            return List.of(0, 255);
        }
    }

    @Override
    public void blinkImpl(int pulseTimeMillis, int blinks) {
        boolean repeat = blinks == -1;

        if (repeat) {
            blinks = 1;
        }

        try {
            cancelWave();
            pulses.clear();

            var startPulse = new Pulse(1 << port, 0, pulseTimeMillis * 1000);
            var endPulse = new Pulse(0, 1 << port, pulseTimeMillis * 1000);

            for (int i = 0; i < blinks; i++) {
                pulses.add(startPulse);
                pulses.add(endPulse);
            }

            getPigpioDaemon().waveAddGeneric(pulses);
            var waveId = getPigpioDaemon().waveCreate();

            if (waveId >= 0) {
                if (repeat) getPigpioDaemon().waveSendRepeat(waveId);
                else getPigpioDaemon().waveSendOnce(waveId);
                activeWaveId = waveId;
            } else {
                String error = "";
                switch (waveId) {
                    case PI_EMPTY_WAVEFORM:
                        error = "Waveform empty";
                        break;
                    case PI_TOO_MANY_CBS:
                        error = "Too many CBS";
                        break;
                    case PI_TOO_MANY_OOL:
                        error = "Too many OOL";
                        break;
                    case PI_NO_WAVEFORM_ID:
                        error = "No waveform ID";
                        break;
                }
                logger.error("Failed to send wave: " + error);
            }

        } catch (PigpioException e) {
            logger.error("Could not set blink on port " + port, e);
        }
    }

    @Override
    public void setBrightnessImpl(int brightness) {
        try {
            cancelWave();
            getPigpioDaemon().setPWMDutycycle(port, getPwmRangeImpl().get(1) * (brightness / 100));
        } catch (PigpioException e) {
            logger.error("Could not dim PWM on port " + port);
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
