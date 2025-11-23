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

package org.photonvision.common.hardware.GPIO;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;

public class CustomDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
        implements GpioDigitalInputOutputDeviceInterface {
    protected final CustomAdapter adapter;
    protected final int gpio;
    private boolean outputValue;

    public CustomDigitalInputOutputDevice(
            CustomDeviceFactory deviceFactory, String key, int gpio, DeviceMode mode) {
        super(key, deviceFactory);

        this.gpio = gpio;

        this.adapter = deviceFactory.adapter;

        this.outputValue = false;

        setMode(mode);
    }

    @Override
    public void setValue(boolean value) throws RuntimeIOException {
        outputValue = value;
        setValue(value);
    }

    @Override
    public boolean getValue() throws RuntimeIOException {
        return adapter.getGPIO(gpio);
    }

    @Override
    public int getGpio() {
        return gpio;
    }

    @Override
    public void setMode(DeviceMode mode) {
        if (mode == DeviceMode.DIGITAL_INPUT) {
            getValue();
        } else if (mode == DeviceMode.DIGITAL_OUTPUT) {
            setValue(outputValue);
        }
    }

    @Override
    public void closeDevice() {
        adapter.releaseGPIO(gpio);
    }
}
