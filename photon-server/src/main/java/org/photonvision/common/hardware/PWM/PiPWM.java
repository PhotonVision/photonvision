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

import com.diozero.PwmLed;
import com.diozero.util.RuntimeIOException;
import java.util.ArrayList;
import java.util.List;

public class PiPWM extends PWMBase {

    private List<Integer> pwmRange =
            new ArrayList<>() {
                {
                    add(0);
                    add(0);
                }
            };
    private final int pin;
    private final PwmLed LED;

    public PiPWM(int pin, int value, int range) throws RuntimeIOException {
        this.pin = pin;
        this.pwmRange.set(1, range);
        LED = new PwmLed(pin);
        LED.setValue(value);
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        pwmRange = range;
    }

    @Override
    public List<Integer> getPwmRange() {
        return pwmRange;
    }

    @Override
    public boolean shutdown() {
        LED.close();
        return true;
    }

    @Override
    public void dimLED(int dimPercentage) {
        // Check to see if dimPercentage is within the range
        if (dimPercentage < pwmRange.get(0) || dimPercentage > pwmRange.get(1)) return;
        LED.setValue(dimPercentage);
    }
}
