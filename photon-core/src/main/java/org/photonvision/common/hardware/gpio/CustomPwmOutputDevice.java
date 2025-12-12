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
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;

public class CustomPwmOutputDevice extends AbstractDevice
        implements InternalPwmOutputDeviceInterface {
    protected final CustomAdapter adapter;
    protected final int gpio;
    private float state;
    private int frequency;

    public CustomPwmOutputDevice(
            CustomDeviceFactory deviceFactory,
            String key,
            int gpio,
            int pwmFrequency,
            float initialValue) {
        super(key, deviceFactory);

        this.adapter = deviceFactory.adapter;
        this.gpio = gpio;
        this.frequency = pwmFrequency;

        setValue(initialValue);
    }

    @Override
    public int getGpio() {
        return gpio;
    }

    @Override
    public int getPwmNum() {
        return gpio;
    }

    @Override
    public float getValue() throws RuntimeIOException {
        return state;
    }

    @Override
    public void setValue(float value) throws RuntimeIOException {
        state = value;
        adapter.setPWM(gpio, value);
    }

    @Override
    public int getPwmFrequency() throws RuntimeIOException {
        return frequency;
    }

    @Override
    public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
        frequency = frequencyHz;
        adapter.setPwmFrequency(gpio, frequencyHz);
    }

    @Override
    protected void closeDevice() throws RuntimeIOException {
        adapter.releaseGPIO(gpio);
    }
}
