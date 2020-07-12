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

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.exception.UnsupportedPinModeException;
import com.pi4j.util.CommandArgumentParser;

public class PiPWM extends PWMBase {
    private static final GpioController gpio = GpioFactory.getInstance();
    private final GpioPinPwmOutput pwm;
    private int[] pwmRange = new int[2];

    public PiPWM(int address) throws UnsupportedPinModeException {
        this.pwm =
                gpio.provisionPwmOutputPin(
                        CommandArgumentParser.getPin(RaspiPin.class, RaspiPin.getPinByAddress(address)));
    }

    @Override
    public void setPwmRate(int rate) {
        pwm.setPwm(rate);
    }

    @Override
    public void setPwmRange(int[] range) {
        pwm.setPwmRange(range[0]);
        pwmRange = range;
    }

    @Override
    public int getPwmRate() {
        return pwm.getPwm();
    }

    @Override
    public int[] getPwmRange() {
        return pwmRange;
    }

    @Override
    public boolean shutdown() {
        gpio.shutdown();
        return gpio.isShutdown();
    }
}
