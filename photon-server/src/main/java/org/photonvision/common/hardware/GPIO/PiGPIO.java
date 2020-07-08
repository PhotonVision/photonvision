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

import com.pi4j.io.gpio.*;

public class PiGPIO extends GPIOBase {
    private static final GpioController gpio = GpioFactory.getInstance();
    private final GpioPinDigitalOutput pin;

    public PiGPIO(int address) {
        this.pin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(address), PinState.LOW);
    }

    @Override
    public void togglePin() {
        pin.toggle();
    }

    @Override
    public void setLow() {
        pin.setState(PinState.LOW);
    }

    @Override
    public void setHigh() {
        pin.setState(PinState.HIGH);
    }

    @Override
    public void setState(boolean state) {
        pin.setState(state);
    }

    @Override
    public void blink(long delay, long duration) {
        pin.blink(delay, duration);
    }

    @Override
    public void pulse(long duration, boolean blocking) {
        pin.pulse(duration, blocking);
    }

    @Override
    public boolean shutdown() {
        gpio.shutdown();
        return gpio.isShutdown();
    }

    @Override
    public boolean getState() {
        return pin.isState(PinState.HIGH);
    }
}
