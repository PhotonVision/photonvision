package org.photonvision.common.hardware.GPIO;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class CustomPwmOutputDevice extends AbstractDevice
        implements InternalPwmOutputDeviceInterface {
    private static final Logger logger = new Logger(CustomPwmOutputDevice.class, LogGroup.General);

    protected final CustomAdapter adapter;
    protected final int gpio;
    private float state;

    public CustomPwmOutputDevice(
            CustomDeviceFactory deviceFactory, String key, int gpio, float initialValue) {
        super(key, deviceFactory);

        this.gpio = gpio;

        this.adapter = deviceFactory.adapter;

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
        throw new UnsupportedOperationException("PWM frequency cannot be retrieved");
    }

    @Override
    public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
        logger.warn("Setting PWM frequency is not implemented");
    }

    @Override
    protected void closeDevice() throws RuntimeIOException {
        adapter.releaseGPIO(gpio);
    }
}
