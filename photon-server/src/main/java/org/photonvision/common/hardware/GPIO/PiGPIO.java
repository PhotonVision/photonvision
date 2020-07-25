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

import com.diozero.LED;
import com.diozero.util.RuntimeIOException;

public class PiGPIO extends GPIOBase {
    private final LED pin;

    public PiGPIO(int address) throws RuntimeIOException {
        this.pin = new LED(address);
    }

    @Override
    public void togglePin() {
        pin.toggle();
    }

    @Override
    public void setLow() {
        pin.off();
    }

    @Override
    public void setHigh() {
        pin.on();
    }

    @Override
    public void setState(boolean state) {
        pin.setOn(state);
    }

    @Override
    public void blink(long delay, long duration) {
        pin.blink(0f, delay, (int) duration, false);
    }

    @Override
    public boolean shutdown() {
        pin.close();
        return true;
    }

    @Override
    public boolean getState() {
        return pin.isLit();
    }
}
