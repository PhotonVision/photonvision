package org.photonvision.common.hardware.GPIO;

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

        this.gpio = gpio;

        this.adapter = deviceFactory.adapter;

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
