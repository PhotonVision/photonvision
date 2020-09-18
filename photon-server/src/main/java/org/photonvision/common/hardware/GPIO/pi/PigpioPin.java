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

package org.photonvision.common.hardware.GPIO.pi;

import static org.photonvision.common.hardware.GPIO.pi.PigpioException.*;

import java.util.ArrayList;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PigpioPin extends GPIOBase {

    public static final Logger logger = new Logger(PigpioPin.class, LogGroup.General);
    private static final PigpioSocket piSocket = new PigpioSocket();

    private final boolean isHardwarePWMPin;
    private final int pinNo;
    private final ArrayList<PigpioPulse> pulses = new ArrayList<>();

    private boolean hasFailedHardwarePWM;

    public PigpioPin(int pinNo) {
        isHardwarePWMPin = pinNo == 12 || pinNo == 13 || pinNo == 17 || pinNo == 18;
        this.pinNo = pinNo;
    }

    @Override
    public int getPinNumber() {
        return pinNo;
    }

    @Override
    protected void setStateImpl(boolean state) {
        try {
            piSocket.gpioWrite(pinNo, state);
        } catch (PigpioException e) {
            logger.error("gpioWrite FAIL - " + e.getMessage());
        }
    }

    @Override
    public boolean shutdown() {
        setState(false);
        return true;
    }

    @Override
    public boolean getStateImpl() {
        try {
            return piSocket.gpioRead(pinNo);
        } catch (PigpioException e) {
            logger.error("gpioRead FAIL - " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void blinkImpl(int pulseTimeMillis, int blinks) {
        boolean repeat = blinks == -1;

        if (repeat) {
            blinks = 1;
        }

        try {
            piSocket.waveTxStop();
            pulses.clear();

            var startPulse = new PigpioPulse(pinNo, 0, pulseTimeMillis * 1000);
            var endPulse = new PigpioPulse(0, pinNo, pulseTimeMillis * 1000);

            for (int i = 0; i < blinks; i++) {
                pulses.add(startPulse);
                pulses.add(endPulse);
            }

            piSocket.waveAddGeneric(pulses);
            var waveId = piSocket.waveCreate();

            if (waveId >= 0) {
                if (repeat) piSocket.waveSendRepeat(waveId);
                else piSocket.waveSendOnce(waveId);
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
            logger.error("Could not set blink - " + e.getMessage());
        }
    }

    @Override
    protected void setBrightnessImpl(int brightness) {
        if (isHardwarePWMPin) {
            try {
                piSocket.hardwarePWM(pinNo, 22000, (int) (1000000 * (brightness / 100.0)));
            } catch (PigpioException e) {
                logger.error("Failed to hardPWM - " + e.getMessage());
            }
        } else if (!hasFailedHardwarePWM) {
            logger.warn("Specified pin (" + pinNo + ") is not capable of hardware PWM - no action will be taken.");
            hasFailedHardwarePWM = true;
        }
    }
}
