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

package org.photonvision.common.hardware.gpio;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

public class CustomDigitalOutputDevice extends AbstractDevice
        implements GpioDigitalOutputDeviceInterface {
    protected final CustomAdapter adapter;
    protected final int gpio;
    private boolean state;

    public CustomDigitalOutputDevice(
            CustomDeviceFactory deviceFactory, String key, int gpio, boolean initialValue) {
        super(key, deviceFactory);

        this.adapter = deviceFactory.adapter;
        this.gpio = gpio;
        this.state = initialValue;

        setValue(initialValue);
    }

    @Override
    public boolean getValue() throws RuntimeIOException {
        return state;
    }

    @Override
    public int getGpio() {
        return gpio;
    }

    @Override
    public void setValue(boolean value) throws RuntimeIOException {
        state = value;
        adapter.setGPIO(gpio, value);
    }

    @Override
    protected void closeDevice() throws RuntimeIOException {
        adapter.releaseGPIO(gpio);
    }
}
