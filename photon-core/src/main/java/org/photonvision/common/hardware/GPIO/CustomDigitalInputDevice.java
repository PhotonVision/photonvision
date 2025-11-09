package org.photonvision.common.hardware.GPIO;

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

        this.gpio = gpio;

        adapter = deviceFactory.adapter;
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
