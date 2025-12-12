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

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;

public class CustomDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
        implements GpioDigitalInputDeviceInterface {
    protected final CustomAdapter adapter;
    protected final int gpio;

    public CustomDigitalInputDevice(
            CustomDeviceFactory deviceFactory,
            String key,
            int gpio,
            GpioPullUpDown pud,
            GpioEventTrigger trigger) {
        super(key, deviceFactory);

        adapter = deviceFactory.adapter;
        this.gpio = gpio;
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
    public void setDebounceTimeMillis(int debounceTime) {
        throw new UnsupportedOperationException("Debounce is not supported");
    }

    @Override
    protected void closeDevice() throws RuntimeIOException {
        adapter.releaseGPIO(gpio);
    }
}
