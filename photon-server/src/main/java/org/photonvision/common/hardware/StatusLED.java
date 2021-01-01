/*
 * Copyright (C) Photon Vision.
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

import java.util.List;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.pi.PigpioPin;

public class StatusLED {
    public final GPIOBase redLED;
    public final GPIOBase greenLED;
    public final GPIOBase blueLED;

    public StatusLED(List<Integer> statusLedPins) {
        // fill unassigned pins with -1 to disable
        if (statusLedPins.size() != 3) {
            for (int i = 0; i < 3 - statusLedPins.size(); i++) {
                statusLedPins.add(-1);
            }
        }

        if (Platform.isRaspberryPi()) {
            redLED = new PigpioPin(statusLedPins.get(0));
            greenLED = new PigpioPin(statusLedPins.get(1));
            blueLED = new PigpioPin(statusLedPins.get(2));
        } else {
            redLED = new CustomGPIO(statusLedPins.get(0));
            greenLED = new CustomGPIO(statusLedPins.get(1));
            blueLED = new CustomGPIO(statusLedPins.get(2));
        }
    }
}
