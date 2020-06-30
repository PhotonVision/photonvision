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
